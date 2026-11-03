package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline

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
 */
internal class SessionMachine(
    timeline: Timeline,
) {
    private val phases = timeline.phases

    private var status = SessionStatus.NOT_STARTED
    private var index = 0

    /** While running: the clock time the current phase started, moved forward by any pause in it. */
    private var phaseStartMillis = 0L

    /** While paused or stopped: the time elapsed in the current phase. */
    private var frozenElapsedMillis = 0L

    /** The current phase's displayed time, as last reported by [SessionEvent.PhaseStarted] or [SessionEvent.SecondChanged]. */
    private var displayedSeconds = phases[0].durationSeconds

    /** Starts the get-ready phase and its countdown. Applies in [SessionStatus.NOT_STARTED]. */
    fun start(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.NOT_STARTED) return@buildList
            status = SessionStatus.RUNNING
            startPhase(0, at = now, events = this)
            add(SessionEvent.CountdownStarted)
        }

    /** Pauses, keeping the time left in the phase to the millisecond (EX-4, EX-5). Applies in [SessionStatus.RUNNING]. */
    fun pause(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.RUNNING) return@buildList
            frozenElapsedMillis = elapsedAt(now)
            status = SessionStatus.PAUSED
            add(SessionEvent.Paused)
        }

    /** Resumes with the time the phase had left (EX-4). Applies in [SessionStatus.PAUSED]. */
    fun resume(now: Long): List<SessionEvent> =
        buildList {
            advance(now, events = this)
            if (status != SessionStatus.PAUSED) return@buildList
            phaseStartMillis = now - frozenElapsedMillis
            status = SessionStatus.RUNNING
            add(SessionEvent.Resumed)
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
            status = SessionStatus.STOPPED
            add(SessionEvent.Stopped)
        }

    /**
     * Applies the time that has passed up to [now]: every phase boundary on the way, in order, then the current
     * phase's displayed second if it changed. Only the current second is reported, even if several have passed.
     */
    fun advanceTo(now: Long): List<SessionEvent> = buildList { advance(now, events = this) }

    /** The state as of the last call. */
    fun state(): SessionState =
        SessionState(
            status = status,
            phaseIndex = index,
            phase = phases[index],
            remainingSeconds = displayedSeconds,
            awaitingAnnouncement = false,
            canGoPrevious = status == SessionStatus.PAUSED && phases[index] !is Phase.GetReady,
        )

    /**
     * The clock time of the next change that time alone will cause, or `null` if there is none: the next whole second
     * of the running phase, which includes its end.
     */
    fun nextWakeAt(): Long? =
        when (status) {
            SessionStatus.RUNNING ->
                phaseStartMillis + (phases[index].durationSeconds - displayedSeconds + 1) * MILLIS_PER_SECOND
            else -> null
        }

    private fun advance(
        now: Long,
        events: MutableList<SessionEvent>,
    ) {
        if (status != SessionStatus.RUNNING) return
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

    private fun complete(events: MutableList<SessionEvent>) {
        status = SessionStatus.COMPLETED
        displayedSeconds = 0
        events += SessionEvent.Completed
    }

    /** The time elapsed in the running phase at [now]. */
    private fun elapsedAt(now: Long): Long = (now - phaseStartMillis).coerceAtLeast(0)

    /** The running phase's time left at [now], rounded up to the whole second (UI-3). */
    private fun remainingSecondsAt(now: Long): Int = ceilSeconds(phases[index].durationSeconds * MILLIS_PER_SECOND - elapsedAt(now))

    private companion object {
        const val MILLIS_PER_SECOND = 1000L

        /** [millis] rounded up to whole seconds. [millis] is never negative here. */
        fun ceilSeconds(millis: Long): Int = ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }
}
