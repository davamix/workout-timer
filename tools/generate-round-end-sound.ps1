#Requires -Version 7.0
<#
.SYNOPSIS
    Generates the round-end chime: app/src/main/res/raw/round_end.wav (SND-1, plan D-10).

.DESCRIPTION
    A short two-note rising chime, built from sine waves, so the repository holds no third-party audio. Each note is
    a sine with two quieter overtones, a short attack and an exponential decay. The mix is normalised to a fixed
    peak, and each note fades out over its last few milliseconds, so the file ends in silence.

    The output is deterministic: running the script again gives an identical file. Tune the parameters below by
    ear, run the script, and commit the new file. Never edit the WAV file by hand.

    Format: WAV, 16-bit PCM, mono, 44 100 Hz.

.EXAMPLE
    pwsh tools/generate-round-end-sound.ps1
#>
[CmdletBinding()]
param(
    # Where to write the file. The default is the app's raw resource.
    [string] $OutFile = (Join-Path $PSScriptRoot '..' 'app' 'src' 'main' 'res' 'raw' 'round_end.wav')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# --- Parameters -------------------------------------------------------------------------------------------------

$SampleRate = 44100

# The notes: frequency in Hz, start and length in seconds. C6, then G6 a little later.
$Notes = @(
    @{ Frequency = 1046.50; Start = 0.00; Length = 0.40 }
    @{ Frequency = 1567.98; Start = 0.12; Length = 0.40 }
)

# Each note's partials: a multiple of its frequency and a relative amplitude. Partial k decays k times faster.
$Partials = @(
    @{ Multiple = 1; Amplitude = 1.00 }
    @{ Multiple = 2; Amplitude = 0.30 }
    @{ Multiple = 3; Amplitude = 0.12 }
)

$AttackSeconds = 0.005   # linear rise at the start of each note
$DecaySeconds = 0.10     # time constant of the fundamental's exponential decay
$FadeOutSeconds = 0.010  # linear fade to silence at the end of each note
$PeakDbfs = -3.0         # peak level of the whole chime

# --- Synthesis --------------------------------------------------------------------------------------------------

$totalSeconds = ($Notes | ForEach-Object { $_.Start + $_.Length } | Measure-Object -Maximum).Maximum
$sampleCount = [int][Math]::Ceiling($totalSeconds * $SampleRate)
$mix = [double[]]::new($sampleCount)

$attackSamples = [Math]::Max(1, [int][Math]::Round($AttackSeconds * $SampleRate))
$fadeSamples = [Math]::Max(2, [int][Math]::Round($FadeOutSeconds * $SampleRate))

foreach ($note in $Notes) {
    $first = [int][Math]::Round($note.Start * $SampleRate)
    $length = [int][Math]::Round($note.Length * $SampleRate)
    $last = [Math]::Min($length, $sampleCount - $first) - 1

    foreach ($partial in $Partials) {
        $omega = 2.0 * [Math]::PI * $note.Frequency * $partial.Multiple / $SampleRate
        $decayPerSample = $partial.Multiple / ($DecaySeconds * $SampleRate)
        $amplitude = [double]$partial.Amplitude

        for ($i = 0; $i -le $last; $i++) {
            $envelope = [Math]::Exp(-$i * $decayPerSample)
            if ($i -lt $attackSamples) { $envelope *= $i / $attackSamples }
            if ($i -ge $length - $fadeSamples) { $envelope *= ($length - 1 - $i) / ($fadeSamples - 1) }
            $mix[$first + $i] += $amplitude * $envelope * [Math]::Sin($omega * $i)
        }
    }
}

$peak = 0.0
foreach ($sample in $mix) { $peak = [Math]::Max($peak, [Math]::Abs($sample)) }
$gain = [Math]::Pow(10.0, $PeakDbfs / 20.0) / $peak

# --- WAV file ---------------------------------------------------------------------------------------------------

$OutFile = [System.IO.Path]::GetFullPath($OutFile)
New-Item -ItemType Directory -Force -Path (Split-Path $OutFile) | Out-Null

$channels = 1
$bitsPerSample = 16
$blockAlign = $channels * $bitsPerSample / 8
$dataBytes = $sampleCount * $blockAlign
$ascii = [System.Text.Encoding]::ASCII

$stream = [System.IO.File]::Create($OutFile)
$writer = [System.IO.BinaryWriter]::new($stream)
try {
    # RIFF header. BinaryWriter writes little-endian, as WAV needs.
    $writer.Write($ascii.GetBytes('RIFF'))
    $writer.Write([uint32](36 + $dataBytes))
    $writer.Write($ascii.GetBytes('WAVE'))

    # Format chunk: PCM.
    $writer.Write($ascii.GetBytes('fmt '))
    $writer.Write([uint32]16)
    $writer.Write([uint16]1)
    $writer.Write([uint16]$channels)
    $writer.Write([uint32]$SampleRate)
    $writer.Write([uint32]($SampleRate * $blockAlign))
    $writer.Write([uint16]$blockAlign)
    $writer.Write([uint16]$bitsPerSample)

    # Data chunk.
    $writer.Write($ascii.GetBytes('data'))
    $writer.Write([uint32]$dataBytes)
    foreach ($sample in $mix) {
        $writer.Write([int16][Math]::Round($sample * $gain * 32767.0))
    }
}
finally {
    $writer.Dispose()
}

$milliseconds = [Math]::Round(1000.0 * $sampleCount / $SampleRate)
Write-Output "Wrote $OutFile ($milliseconds ms, $((Get-Item $OutFile).Length) bytes)"
