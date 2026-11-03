package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.timeline.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs a workout's [timeline] in real time (plan D-6, D-7). It walks the phases on a monotonic [Clock], takes the
 * runner's commands, and publishes a [state] for the screen and [events] for the audio.
 *
 * Phase boundaries are absolute clock times, so late timer wake-ups never shift the schedule (NF-1). A command that
 * doesn't apply in the current status does nothing; see each command.
 *
 * Commands may be called from any thread, including from a collector of [state] or [events]. They apply in call
 * order, and each one first applies the time that has passed.
 *
 * @param timeline The workout to run.
 * @param clock The monotonic clock: `AppContainer.clock` on the tablet, `Clock { testScheduler.currentTime }` in tests.
 * @param scope Where the session's timer coroutine runs: its owner's scope, such as `viewModelScope`. Cancelling it
 *   stops the timer and completes [events].
 * @param waitForAnnouncement Whether get ready waits for [announcementFinished] before its countdown starts (TTS-10):
 *   `true` when voice cues are on and the audio is wired.
 * @param announcementTimeoutMillis How long get ready waits at most for [announcementFinished].
 */
class WorkoutSession internal constructor(
    val timeline: Timeline,
    private val clock: Clock,
    private val scope: CoroutineScope,
    waitForAnnouncement: Boolean,
    announcementTimeoutMillis: Long,
    private val sleepFor: (plannedMillis: Long) -> Long,
) {
    constructor(
        timeline: Timeline,
        clock: Clock,
        scope: CoroutineScope,
        waitForAnnouncement: Boolean = false,
        announcementTimeoutMillis: Long = ANNOUNCEMENT_TIMEOUT_MILLIS,
    ) : this(timeline, clock, scope, waitForAnnouncement, announcementTimeoutMillis, sleepFor = { it })

    private val machine = SessionMachine(timeline, waitForAnnouncement, announcementTimeoutMillis)

    // Guarded by lock: the machine, and the following fields.
    private val lock = Any()
    private val queue = ArrayDeque<SessionMachine.(now: Long) -> List<SessionEvent>>()
    private var draining = false
    private var timerLaunched = false

    private val mutableState = MutableStateFlow(machine.state())

    /** What the runner screen shows. A new value comes after every change: while running, once per displayed second. */
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    private val eventChannel = Channel<SessionEvent>(Channel.UNLIMITED)

    /**
     * What happens, for the audio, in order and as it happens. Collect it once: each event goes to one collector.
     * Events sent before the collector starts are kept, so [start] may come first. It completes after
     * [SessionEvent.Completed] or [SessionEvent.Stopped], or when the scope is cancelled.
     */
    val events: Flow<SessionEvent> = eventChannel.receiveAsFlow()

    /** Tells the timer to plan its next wake-up again: sent after every batch of commands. */
    private val replan = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { eventChannel.close() }
    }

    /** Starts the workout with get ready. Applies once, before anything else. */
    fun start() {
        execute { start(it) }
        val launchTimer =
            synchronized(lock) {
                val first = !timerLaunched
                timerLaunched = true
                first
            }
        if (launchTimer) scope.launch { runTimer() }
    }

    /** Pauses, keeping the time left (EX-4, EX-5). Applies while running. */
    fun pause() = execute { pause(it) }

    /** Resumes with the time left (EX-4). After Next or Previous, the target exercise starts. Applies while paused. */
    fun resume() = execute { resume(it) }

    /** Moves to the next exercise, staying paused, or completes on the last one (EX-9). Applies while paused. */
    fun next() = execute { next(it) }

    /**
     * Moves to the previous exercise, staying paused; restarts the first one (EX-9). Applies while paused, except in
     * get ready ([SessionState.canGoPrevious]).
     */
    fun previous() = execute { previous(it) }

    /** Ends the workout (EX-8). Applies before start, while running and while paused. */
    fun stop() = execute { stop(it) }

    /**
     * "Get ready in" has been spoken, or couldn't be: the countdown starts now (TTS-10). Applies while running in get
     * ready and waiting for it; a late or repeated call is ignored.
     */
    fun announcementFinished() = execute { announcementFinished(it) }

    /**
     * Queues [command], then applies the queue unless another call is already doing so: a call from another thread,
     * or an outer call on this thread that is publishing events to a collector that calls back. Only the thread that
     * applies the queue publishes, so events go out in order and a command never runs in the middle of another.
     */
    private fun execute(command: SessionMachine.(now: Long) -> List<SessionEvent>) {
        synchronized(lock) {
            queue.addLast(command)
            if (draining) return
            draining = true
        }
        var done = false
        try {
            while (!done) {
                var events: List<SessionEvent> = emptyList()
                var state: SessionState? = null
                synchronized(lock) {
                    val queued = queue.removeFirstOrNull()
                    if (queued == null) {
                        draining = false
                        done = true
                    } else {
                        events = queued.invoke(machine, clock.nowMillis())
                        state = machine.state()
                    }
                }
                state?.let { publish(it, events) }
            }
        } finally {
            if (!done) synchronized(lock) { draining = false }
        }
        replan.trySend(Unit)
    }

    private fun publish(
        state: SessionState,
        events: List<SessionEvent>,
    ) {
        mutableState.value = state
        events.forEach { eventChannel.trySend(it) }
        if (state.status.isFinal) eventChannel.close()
    }

    /**
     * The timer: sleeps until the machine's next wake-up, then applies the time. It plans again after every command,
     * and ends when the workout does.
     */
    private suspend fun runTimer() {
        while (true) {
            val wakeAt =
                synchronized(lock) {
                    if (machine.state().status.isFinal) return
                    machine.nextWakeAt()
                }
            if (wakeAt == null) {
                replan.receive()
                continue
            }
            val millis = wakeAt - clock.nowMillis()
            if (millis > 0) {
                // Woken by a command or by the time: either way, plan again. An early wake-up just sleeps again.
                withTimeoutOrNull(sleepFor(millis).coerceAtLeast(1)) { replan.receive() }
            } else {
                execute { advanceTo(it) }
                // execute() sends replan once the time is applied. If this call was queued behind another one, it
                // comes when that one has finished, so the next plan never sees the time before it was applied.
                replan.receive()
            }
        }
    }

    private val SessionStatus.isFinal: Boolean
        get() = this == SessionStatus.COMPLETED || this == SessionStatus.STOPPED
}
