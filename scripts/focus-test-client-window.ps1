[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot,
    [int]$ProcessId = 0,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'SilentlyContinue'

Add-Type @'
using System;
using System.Runtime.InteropServices;

public static class EchoWarriorWindowFocus {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);
}
'@

$escapedProjectRoot = [regex]::Escape($ProjectRoot)
$deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

while ([DateTime]::UtcNow -lt $deadline) {
    $candidateId = $ProcessId
    if ($candidateId -le 0) {
        $candidate = Get-CimInstance Win32_Process |
            Where-Object {
                $_.Name -match '^javaw?\.exe$' -and
                $_.CommandLine -match $escapedProjectRoot -and
                $_.CommandLine -match 'net\.minecraft\.client\.main\.Main'
            } |
            Select-Object -First 1
        if ($candidate) {
            $candidateId = $candidate.ProcessId
        }
    }

    if ($candidateId -gt 0) {
        $process = Get-Process -Id $candidateId
        if ($process -and $process.MainWindowHandle -ne [IntPtr]::Zero) {
            Start-Sleep -Milliseconds 750
            $process.Refresh()
            $handle = $process.MainWindowHandle
            if ($handle -ne [IntPtr]::Zero) {
                [EchoWarriorWindowFocus]::ShowWindowAsync($handle, 9) | Out-Null
                $shell = New-Object -ComObject WScript.Shell
                $shell.AppActivate($candidateId) | Out-Null
                [EchoWarriorWindowFocus]::SetForegroundWindow($handle) | Out-Null
                exit 0
            }
        }
    }

    Start-Sleep -Milliseconds 250
}

exit 1
