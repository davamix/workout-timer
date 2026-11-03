package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.timeline.NavigationTarget
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline

/** How long get ready waits for "Get ready in" before the countdown starts anyway (TTS-10, plan §2.3). */
const val ANNOUNCEMENT_TIMEOUT_MILLIS = 5_000L

/**
 * The workout engine's rules, with no coroutines, clock or threads (plan D-7). [WorkoutSession] drives it; tests call
 * it directly with chosen times.
 *
 * Every method takes the clock time `now`, in milliseconds, and returns the events it caused, in order. The times
 * passed in must never decrease. Phase boundaries are absolute clock times: when a phase ends, the next one starts
 * at the exact end time, however late the call that notices it (NF-1).
 *
 * Every command first applies the time that has passed, as [advanceTo] does, then applies itself. A command that
 * doesn't apply in the current status does nothing: no change, no event and no exception.
 *
 * @param waitForAnnouncement Whether get ready waits for [announcementFinished] before its countdown starts (TTS-10).
 * @param announcementTimeoutMillis How long it waits at most.
 */
internal class SessionMachine(
    private val timeline: Timeline,
    private val waitForAnnouncement: Boolean = false,
    private val announcementTimeoutMillis: Long = ANNOUNCEMENT_TIMEOUT_MILLIS,
) {
    private val phases = timeline.phases

    private var status = SessionStatus.NOT_STARTED
    private var index = 0

    /** While running and not [awaiting]: the clock time the current phase started, moved forward by any pause in it. */
    private var phaseStartMillis = 0L

    /** While paused or stopped: the time elapsed in the current phase. */
    private var frozenElapsedMillis = 0L

    /** The current phase's displayed time, as last reported by [SessionEvent.PhaseStarted] or [SessionEvent.SecondChanged]. */
    private var displayedSeconds = phases[0].durationSeconds

    /** Whether the session is running in get ready and waiting for the announcement; the countdown hasn't started. */
    private var awaiting = false

    /** While [awaiting]: the clock time the wait began. */
    private var awaitStartMillis = 0L

    /** While paused: what the next [resume] must begin, besides resuming. */
    private var pendingOnResume = PendingStart.NOTHING

    private enum class PendingStart {
        NOTHING,

        /** The phase was reached with Next or Previous, so it hasn't started: send [SessionEvent.PhaseStarted]. */
        PHASE,

        /** Get ready was paused while waiting for the announcement: start the countdown. */
        COUNTDOWN,
    }

    /**
     * Starts the get-ready phase. Its countdown starts at once, or after the announcement if [waitForAnnouncement].
     * Applies in [SessionStatus.NOT_STARTED].
     */
    fun start(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.NOT_STARTED) return@buildList
            status = SessionStatus.RUNNING
            startPhase(0, at = now, events = this)
            if (waitForAnnouncement) {
                awaiting = true
                awaitStartMillis = now
            } else {
                startCountdown(at = now, events = this)
            }
        }

    /**
     * Pauses, keeping the time left in the phase to the millisecond (EX-4, EX-5). A pause while waiting for the
     * announcement ends the wait: the countdown starts on resume. Applies in [SessionStatus.RUNNING].
     */
    fun pause(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.RUNNING) return@buildList
            frozenElapsedMillis = elapsedAt(now)
            if (awaiting) {
                awaiting = false
                pendingOnResume = PendingStart.COUNTDOWN
            }
            status = SessionStatus.PAUSED
            add(SessionEvent.Paused)
        }

    /**
     * Resumes with the time the phase had left (EX-4). A phase reached with Next or Previous starts now, and a get
     * ready paused while waiting starts its countdown now. Applies in [SessionStatus.PAUSED].
     */
    fun resume(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.PAUSED) return@buildList
            phaseStartMillis = now - frozenElapsedMillis
            status = SessionStatus.RUNNING
            add(SessionEvent.Resumed)
            when (pendingOnResume) {
                PendingStart.NOTHING -> Unit
                PendingStart.PHASE -> add(SessionEvent.PhaseStarted(index, phases[index]))
                PendingStart.COUNTDOWN -> add(SessionEvent.CountdownStarted)
            }
            pendingOnResume = PendingStart.NOTHING
        }

    /**
     * Moves to the target of [Timeline.next] and stays paused at its start, with its full duration (EX-9). Next on the
     * last exercise completes the workout. Applies in [SessionStatus.PAUSED].
     */
    fun next(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.PAUSED) return@buildList
            when (val target = timeline.next(index)) {
                is NavigationTarget.GoTo -> moveTo(target.phaseIndex)
                NavigationTarget.Complete -> complete(events = this)
            }
        }

    /**
     * Moves to the target of [Timeline.previous] and stays paused at its start, with its full duration (EX-9). On the
     * first exercise this restarts it. Applies in [SessionStatus.PAUSED], except in get ready.
     */
    fun previous(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.PAUSED) return@buildList
            timeline.previous(index)?.let { moveTo(it) }
        }

    /**
     * Ends the session (EX-8). Applies in [SessionStatus.NOT_STARTED], [SessionStatus.RUNNING] and
     * [SessionStatus.PAUSED]. The state keeps the phase and the time it had left.
     */
    fun stop(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            when (status) {
                SessionStatus.NOT_STARTED, SessionStatus.PAUSED -> Unit
                SessionStatus.RUNNING -> frozenElapsedMillis = elapsedAt(now)
                SessionStatus.COMPLETED, SessionStatus.STOPPED -> return@buildList
            }
            awaiting = false
            status = SessionStatus.STOPPED
            add(SessionEvent.Stopped)
        }

    /**
     * "Get ready in" has been spoken, or couldn't be: the countdown starts now (TTS-10). Applies while running in get
     * ready and waiting for the announcement; a call that comes late, twice or while paused is ignored.
     */
    fun announcementFinished(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status == SessionStatus.RUNNING && awaiting) startCountdown(at = now, events = this)
        }

    /**
     * Applies the time that has passed up to [now]: the announcement timeout and every phase boundary on the way, in
     * order, then the current phase's displayed second if it changed. Only the current second is reported, even if
     * several have passed.
     */
    fun advanceTo(now: Long): List<SessionEvent> = buildList { advance(now, events = this) }

    /** The state as of the last call. */
    fun state(): SessionState =
        SessionState(
            status = status,
            phaseIndex = index,
            phase = phases[index],
            remainingSeconds = displayedSeconds,
            awaitingAnnouncement = status == SessionStatus.RUNNING && awaiting,
            canGoPrevious = status == SessionStatus.PAUSED && phases[index] !is Phase.GetReady,
        )

    /**
     * The clock time of the next change that time alone will cause, or `null` if there is none: while waiting for the
     * announcement, its timeout; while running, the next whole second of the phase, which includes its end.
     */
    fun nextWakeAt(): Long? =
        when {
            status != SessionStatus.RUNNING -> null
            awaiting -> awaitStartMillis + announcementTimeoutMillis
            else -> phaseStartMillis + (phases[index].durationSeconds - displayedSeconds + 1) * MILLIS_PER_SECOND
        }

    private fun advance(
        now: Long,
        events: MutableList<SessionEvent>,
    ) {
        if (status != SessionStatus.RUNNING) return
        if (awaiting) {
            val timeout = awaitStartMillis + announcementTimeoutMillis
            if (now < timeout) return
            startCountdown(at = timeout, events = events)
        }
        while (true) {
            val phase = phases[index]
            val end = phaseStartMillis + phase.durationSeconds * MILLIS_PER_SECOND
            if (now < end) break
            if (index == phases.lastIndex) {
                complete(events)
                return
            }
            if (phase is Phase.Exercise && phase.exercise.isLastInRound && !phase.exercise.isLastInWorkout) {
                events += SessionEvent.RoundEnded(phase.exercise.roundNumber)
            }
            startPhase(index + 1, at = end, events = events)
        }
        val seconds = remainingSecondsAt(now)
        if (seconds != displayedSeconds) {
            displayedSeconds = seconds
            events += SessionEvent.SecondChanged(index, phases[index], seconds)
        }
    }

    private fun startPhase(
        newIndex: Int,
        at: Long,
        events: MutableList<SessionEvent>,
    ) {
        index = newIndex
        phaseStartMillis = at
        displayedSeconds = phases[newIndex].durationSeconds
        events += SessionEvent.PhaseStarted(newIndex, phases[newIndex])
    }

    private fun startCountdown(
        at: Long,
        events: MutableList<SessionEvent>,
    ) {
        awaiting = false
        phaseStartMillis = at
        events += SessionEvent.CountdownStarted
    }

    /** While paused: goes to the start of the phase at [newIndex], which starts on the next [resume]. */
    private fun moveTo(newIndex: Int) {
        index = newIndex
        frozenElapsedMillis = 0
        displayedSeconds = phases[newIndex].durationSeconds
        pendingOnResume = PendingStart.PHASE
    }

    private fun complete(events: MutableList<SessionEvent>) {
        status = SessionStatus.COMPLETED
        displayedSeconds = 0
        pendingOnResume = PendingStart.NOTHING
        events += SessionEvent.Completed
    }

    /** The time elapsed in the running phase at [now]. Get ready doesn't count while waiting for the announcement. */
    private fun elapsedAt(now: Long): Long = if (awaiting) 0 else (now - phaseStartMillis).coerceAtLeast(0)

    /** The running phase's time left at [now], rounded up to the whole second (UI-3). */
    private fun remainingSecondsAt(now: Long): Int = ceilSeconds(phases[index].durationSeconds * MILLIS_PER_SECOND - elapsedAt(now))

    private companion object {
        const val MILLIS_PER_SECOND = 1000L

        /** [millis] rounded up to whole seconds. [millis] is never negative here. */
        fun ceilSeconds(millis: Long): Int = ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }
}
