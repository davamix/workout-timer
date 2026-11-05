# Helpers for the tablet skill, for Git Bash. Load them at the start of every Bash call that uses them, from the repo
# root (shell functions don't carry over between calls):
#
#   source .claude/skills/tablet/helpers.sh
#
# Each function says what it does above it. SKILL.md lists them under "Helpers".

export MSYS_NO_PATHCONV=1
adb=/d/Android/Sdk/platform-tools/adb.exe
pkg=io.github.davamix.workouttimer.debug
activity=io.github.davamix.workouttimer.MainActivity
runner=io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner
gradle_log=build/gradle.log

# Runs ./gradlew with the given tasks, quietly. The output goes to build/gradle.log, and on failure the errors are
# printed. Returns Gradle's own exit code, so `gradle installDebug && instrument …` stops when the build fails. A pipe
# such as `./gradlew … | grep … | tail` would return the last command's code instead.
gradle() {
    mkdir -p build
    ./gradlew "$@" --console=plain -q > "$gradle_log" 2>&1
    local rc=$?
    if [ $rc -ne 0 ]; then
        echo "gradle $* failed (exit $rc). Full output: $gradle_log"
        grep -v -i 'unsafe\|^WARNING\|^\s*$' "$gradle_log" | head -40
    fi
    return $rc
}

# Runs instrumented tests on the tablet and keeps the app's data, unlike connectedDebugAndroidTest. Install both APKs
# first: `gradle installDebug installDebugAndroidTest && instrument …`.
#   instrument                          the whole suite
#   instrument ui.runner.RunnerScreenTest[#method][,…]   classes or methods, the package prefix optional
#   instrument <class> -e <key> <value>  with arguments for the test
# It passed when the summary ends with "OK (N tests)".
instrument() {
    local spec=""
    if [ $# -gt 0 ] && [ "$1" != "-e" ]; then
        spec=$(echo "$1" | tr ',' '\n' | sed 's/^\(io\.github\.davamix\.workouttimer\.\)\{0,1\}/io.github.davamix.workouttimer./' | paste -sd, -)
        shift
    fi
    $adb shell am instrument -w ${spec:+-e class "$spec"} "$@" "$runner" | tr -d '\r' | grep -v '^$' | grep -v '^\s*at ' | tail -40
}

# Prints every text on screen, on one line. A running workout can't be read this way: its time changes every second,
# so uiautomator never sees the screen idle. Read it while paused, or take a screenshot.
texts() {
    $adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o 'text="[^"]\+"' | sed 's/^text=//' | tr '\n' ' '
    echo
}

# Waits until PATTERN (a grep pattern) is on screen, for at most SECONDS (default 30), without sleeping: each dump
# takes about 2 s. Fails with a message on timeout.
wait_for() {
    local end=$((SECONDS + ${2:-30}))
    until $adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -q "$1"; do
        [ $SECONDS -ge $end ] && { echo "timeout: '$1' not on screen after ${2:-30} s"; return 1; }
    done
}

# Taps the node whose text is exactly TEXT. If several match, "last" takes the last one, such as a dialog's button
# over the screen's button of the same name; the default is the first.
tap_text() {
    local bounds
    bounds=$($adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o "text=\"$1\"[^>]*bounds=\"[^\"]*\"" |
        if [ "$2" = last ]; then tail -1; else head -1; fi | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | paste -sd' ' -)
    [ -z "$bounds" ] && { echo "tap_text: '$1' not on screen"; return 1; }
    local x1 y1 x2 y2
    read -r x1 y1 x2 y2 <<< "$bounds"
    $adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

# Opens the app as its launcher icon does: if its task exists, it comes to the front as it was.
launch() {
    $adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$pkg/$activity" > /dev/null 2>&1
}

# Force-stops the app. With "cold", the speech engine too, for a cold start: preparing the voice then takes 9-10 s.
stop_app() {
    $adb shell am force-stop $pkg
    if [ "$1" = cold ]; then $adb shell am force-stop com.google.android.tts; fi
}

# Saves a screenshot to FILE, a local path such as one in the scratchpad. Look at it with Read.
shot() {
    $adb shell screencap -p /sdcard/wt.png && $adb pull /sdcard/wt.png "$1" > /dev/null && $adb shell rm /sdcard/wt.png
}

# Presses Start on the workout called NAME. The workout list must be on screen.
start_workout() {
    local nodes name_node name_y start_xy
    nodes=$($adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o '<node [^>]*>')
    name_node=$(echo "$nodes" | grep "text=\"$1\"" | head -1)
    [ -z "$name_node" ] && { echo "start_workout: no workout '$1' on screen"; return 1; }
    name_y=$(echo "$name_node" | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | paste -sd' ' - | awk '{ print int(($2 + $4) / 2) }')
    # The Start button on the same row: the one nearest to the name, vertically.
    start_xy=$(echo "$nodes" | grep 'text="Start"' | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | paste -d' ' - - - - |
        awk -v ny="$name_y" '{ x = int(($1 + $3) / 2); y = int(($2 + $4) / 2); d = y - ny; if (d < 0) d = -d
            if (best == "" || d < best) { best = d; bx = x; by = y } } END { if (best != "") print bx, by }')
    [ -z "$start_xy" ] && { echo "start_workout: no Start button on screen"; return 1; }
    $adb shell input tap $start_xy
}

# Ends the app's process the way Android does to free memory (EX-11). `am kill` doesn't work on this tablet. Home,
# another app and Home again make the app cached rather than the "previous" app; then SIGKILL. Its task stays in
# Recents, so `launch` then rebuilds the activity from its saved state.
end_process() {
    $adb shell input keyevent KEYCODE_HOME
    $adb shell sleep 1
    $adb shell am start -a android.settings.SETTINGS > /dev/null 2>&1
    $adb shell sleep 2
    $adb shell input keyevent KEYCODE_HOME
    $adb shell sleep 2
    local pid
    pid=$($adb shell pidof $pkg | tr -d '\r')
    [ -z "$pid" ] && { echo "end_process: the app isn't running"; return 1; }
    $adb shell run-as $pkg kill -9 "$pid"
    $adb shell sleep 1
    if [ -z "$($adb shell pidof $pkg | tr -d '\r')" ]; then echo "process $pid ended"; else echo "process $pid still running"; return 1; fi
}

# Prints "yes" if the app's window keeps the screen on (FLAG_KEEP_SCREEN_ON, 0x80; NF-2), and "no" otherwise.
keeps_screen_on() {
    local flags
    flags=$($adb shell dumpsys window windows | grep -A6 "Window{.*$pkg/$activity" | grep -o 'fl=#[0-9a-f]*' | head -1 | cut -d'#' -f2)
    [ -z "$flags" ] && { echo "keeps_screen_on: the app has no window"; return 1; }
    if [ $((0x$flags & 0x80)) -ne 0 ]; then echo yes; else echo no; fi
}

# Prints whether the screen is on: Awake, or Asleep or Dozing when it's off.
wakefulness() {
    $adb shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*' | head -1 | cut -d= -f2
}

# Sets the screen timeout to MILLIS, or back to the tablet's usual "never" with `screen_timeout never`, and prints the
# new value. Ask the user before changing it, and always put it back.
screen_timeout() {
    local value=$1
    [ "$value" = never ] && value=2147483647
    $adb shell settings put system screen_off_timeout "$value"
    $adb shell settings get system screen_off_timeout | tr -d '\r'
}

# Waits until a log line from TAG matches REGEX, for at most SECONDS (default 60), without sleeping. Clear the log
# first with `$adb logcat -c`, so an older line doesn't count.
wait_for_log() {
    timeout "${3:-60}" $adb logcat -s "$1:V" -e "$2" -m 1 > /dev/null
    local rc=$?
    [ $rc -ne 0 ] && echo "timeout: no '$2' from $1 after ${3:-60} s"
    return $rc
}
