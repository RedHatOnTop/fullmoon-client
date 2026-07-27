# shot-app.ps1 — screenshot the running Pinion window.
# WebView2 content is not in the DWM redirection surface for PrintWindow on
# every driver, so this brings the window forward and BitBlts the screen area
# it occupies. Run under Windows PowerShell 5.1 (System.Drawing).
#
#   powershell -ExecutionPolicy Bypass -File scripts/shot-app.ps1 -Out shot.png

param(
  [string]$Out = "pinion-shot.png",
  [string]$Process = "pinion",
  [int]$SettleMs = 900,
  [string]$TitleLike = ""
)

Add-Type -AssemblyName System.Drawing

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
"@

$proc = Get-Process -Name $Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Where-Object { $TitleLike -eq '' -or $_.MainWindowTitle -like $TitleLike } | Select-Object -First 1
if (-not $proc) { Write-Error "no visible window for process '$Process'"; exit 2 }

$h = $proc.MainWindowHandle
[void][Win]::ShowWindow($h, 9)              # SW_RESTORE
[void][Win]::SetWindowPos($h, [IntPtr](-1), 0, 0, 0, 0, 0x0003)  # HWND_TOPMOST, keep size/pos
[void][Win]::SetForegroundWindow($h)
Start-Sleep -Milliseconds $SettleMs

$r = New-Object Win+RECT
if (-not [Win]::GetWindowRect($h, [ref]$r)) { Write-Error "GetWindowRect failed"; exit 3 }
$w = $r.Right - $r.Left
$ht = $r.Bottom - $r.Top
if ($w -le 0 -or $ht -le 0) { Write-Error "empty window rect"; exit 4 }

$bmp = New-Object System.Drawing.Bitmap $w, $ht
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($r.Left, $r.Top, 0, 0, (New-Object System.Drawing.Size $w, $ht))
$g.Dispose()

$path = [System.IO.Path]::GetFullPath($Out)
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

[void][Win]::SetWindowPos($h, [IntPtr](-2), 0, 0, 0, 0, 0x0003)  # HWND_NOTOPMOST
Write-Output "SHOT $path ${w}x${ht}"
