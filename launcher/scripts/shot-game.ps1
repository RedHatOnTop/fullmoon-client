# shot-game.ps1 — screenshot the running Minecraft window without touching it.
#
# The window is found by owning process, never by title. Title matching once
# grabbed a Zetile tile whose header happened to contain the word Minecraft and
# shoved someone's editor off the desktop; a probe must not be able to move a
# window it does not own. The game process is identified by its command line
# (the launcher runs it out of the Pinion instances tree), and among its windows
# only the GLFW top-level is a candidate.
#
# Capture is PrintWindow(PW_RENDERFULLCONTENT), not a screen BitBlt, so the
# window never has to be raised or focused. `-Hide` gets it out of the way
# without stopping it: moved off the desktop and restyled WS_EX_TOOLWINDOW, so
# it is gone from the taskbar and from Alt-Tab too.
#
# ShowWindow(SW_HIDE) was tried and does not work here. A hidden window loses
# its DWM surface, Minecraft stops swapping frames into it, and PrintWindow
# comes back with a flat white plate that survives any settle delay.
#
# The process must be per-monitor DPI aware before it asks for a window rect.
# Minecraft's window is, powershell.exe is not: on a 125% display an unaware
# caller is handed 698x422 for an 870x527 window, and PrintWindow then draws
# real pixels into that undersized bitmap — which crops the right and bottom
# edges rather than scaling them.
#
#   powershell -ExecutionPolicy Bypass -File scripts/shot-game.ps1 -Out hud.png -Hide

param(
  [string]$Out = "game-shot.png",
  [int]$ProcessId = 0,
  [switch]$Hide,
  [switch]$Show,
  [switch]$Park,
  [switch]$HideOnly,
  [switch]$List,
  [int]$SettleMs = 700
)

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Dpi {
  [DllImport("user32.dll")] public static extern IntPtr SetProcessDpiAwarenessContext(IntPtr ctx);
  public static readonly IntPtr PER_MONITOR_V2 = new IntPtr(-4);
}
"@
[void][Dpi]::SetProcessDpiAwarenessContext([Dpi]::PER_MONITOR_V2)

Add-Type -AssemblyName System.Drawing

Add-Type @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public class GameWin {
  public delegate bool EnumProc(IntPtr h, IntPtr p);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr p);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr h);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetClassName(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
  [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
  [DllImport("user32.dll", EntryPoint="GetWindowLongPtrW")] public static extern IntPtr GetWindowLongPtr(IntPtr h, int idx);
  [DllImport("user32.dll", EntryPoint="SetWindowLongPtrW")] public static extern IntPtr SetWindowLongPtr(IntPtr h, int idx, IntPtr v);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
  [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }

  public const uint SWP_NOSIZE = 0x0001, SWP_NOZORDER = 0x0004, SWP_NOACTIVATE = 0x0010, SWP_FRAMECHANGED = 0x0020;
  public const uint PW_RENDERFULLCONTENT = 0x0002;
  public const int SW_HIDE = 0, SW_SHOWNA = 8;
  public const int GWL_EXSTYLE = -20;
  public const long WS_EX_TOOLWINDOW = 0x00000080, WS_EX_APPWINDOW = 0x00040000;

  public static uint Owner = 0;
  public static List<string> Seen = new List<string>();
  public static IntPtr Found = IntPtr.Zero;

  /* Only a top-level window owned by the game process counts, and among those
     only the GLFW surface — the JVM also owns invisible message windows. */
  public static IntPtr Find(uint pid) {
    Owner = pid; Found = IntPtr.Zero; Seen.Clear();
    EnumWindows(new EnumProc(Visit), IntPtr.Zero);
    return Found;
  }

  static bool Visit(IntPtr h, IntPtr p) {
    uint pid; GetWindowThreadProcessId(h, out pid);
    if (pid != Owner) return true;
    StringBuilder c = new StringBuilder(128); GetClassName(h, c, 128);
    int n = GetWindowTextLength(h);
    StringBuilder t = new StringBuilder(n + 1); GetWindowText(h, t, t.Capacity);
    Seen.Add(string.Format("{0} class={1} visible={2} title={3}", h, c, IsWindowVisible(h), t));
    if (c.ToString().StartsWith("GLFW") && Found == IntPtr.Zero) Found = h;
    return true;
  }
}
"@

function Find-GameProcess {
  # the launcher always runs the client with its game directory inside the
  # Pinion instances tree, which no other java process on this box does
  $p = Get-CimInstance Win32_Process -Filter "Name='java.exe' or Name='javaw.exe'" |
    Where-Object { $_.CommandLine -and $_.CommandLine -match 'Pinion[\\/]instances' -and $_.CommandLine -match 'net\.minecraft\.client\.main\.Main' }
  if (-not $p) { return 0 }
  return @($p)[0].ProcessId
}

if ($ProcessId -eq 0) { $ProcessId = Find-GameProcess }
if ($ProcessId -eq 0) { Write-Error "no Minecraft client process (java running out of the Pinion instances tree)"; exit 2 }

$h = [GameWin]::Find([uint32]$ProcessId)
if ($List) {
  Write-Output ("PID {0}" -f $ProcessId)
  [GameWin]::Seen | ForEach-Object { Write-Output $_ }
  exit 0
}
if ($h -eq [IntPtr]::Zero) {
  Write-Error ("pid {0} owns no GLFW window yet. windows: {1}" -f $ProcessId, ([GameWin]::Seen -join ' | '))
  exit 3
}

$flags = [GameWin]::SWP_NOSIZE -bor [GameWin]::SWP_NOZORDER -bor [GameWin]::SWP_NOACTIVATE

if ($Show) { [void][GameWin]::ShowWindow($h, [GameWin]::SW_SHOWNA); Write-Output "SHOWN"; exit 0 }

if ($Hide -or $HideOnly -or $Park) {
  # a window SW_HIDE left behind still has to come back to render
  if (-not [GameWin]::IsWindowVisible($h)) { [void][GameWin]::ShowWindow($h, [GameWin]::SW_SHOWNA) }
  [void][GameWin]::SetWindowPos($h, [IntPtr]::Zero, -3000, 0, 0, 0, $flags)
}
if ($Hide -or $HideOnly) {
  $ex = [long][GameWin]::GetWindowLongPtr($h, [GameWin]::GWL_EXSTYLE)
  $want = ($ex -bor [GameWin]::WS_EX_TOOLWINDOW) -band (-bnot [GameWin]::WS_EX_APPWINDOW)
  if ($want -ne $ex) {
    [void][GameWin]::SetWindowLongPtr($h, [GameWin]::GWL_EXSTYLE, [IntPtr]$want)
    [void][GameWin]::SetWindowPos($h, [IntPtr]::Zero, -3000, 0, 0, 0, $flags -bor [GameWin]::SWP_FRAMECHANGED)
  }
}
if ($Hide -or $HideOnly -or $Park) { Start-Sleep -Milliseconds $SettleMs }
if ($HideOnly) { Write-Output ("HIDDEN pid={0} hwnd={1}" -f $ProcessId, $h); exit 0 }

$r = New-Object GameWin+RECT
if (-not [GameWin]::GetWindowRect($h, [ref]$r)) { Write-Error "GetWindowRect failed"; exit 4 }
$w = $r.Right - $r.Left
$ht = $r.Bottom - $r.Top
if ($w -le 0 -or $ht -le 0) { Write-Error "empty window rect"; exit 5 }

$bmp = New-Object System.Drawing.Bitmap $w, $ht
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
$ok = [GameWin]::PrintWindow($h, $hdc, [GameWin]::PW_RENDERFULLCONTENT)
$g.ReleaseHdc($hdc)
$g.Dispose()
if (-not $ok) { Write-Error "PrintWindow failed"; exit 6 }

# A surface the compositor would not hand over comes back as a flat plate —
# black when it was never composited, white when it has been recreated but not
# yet drawn into. Both are uniform, so measure spread rather than brightness:
# a picture of a game is never one colour. Sample the client area only, or the
# title bar's own contrast passes the check for a blank game.
$cr = New-Object GameWin+RECT
[void][GameWin]::GetClientRect($h, [ref]$cr)
$origin = New-Object GameWin+POINT
[void][GameWin]::ClientToScreen($h, [ref]$origin)
$cx0 = [math]::Max(0, $origin.X - $r.Left)
$cy0 = [math]::Max(0, $origin.Y - $r.Top)
$cx1 = [math]::Min($w, $cx0 + ($cr.Right - $cr.Left))
$cy1 = [math]::Min($ht, $cy0 + ($cr.Bottom - $cr.Top))

$sample = 0; $lit = 0; $sum = 0.0; $sumSq = 0.0
for ($y = $cy0; $y -lt $cy1; $y += 37) {
  for ($x = $cx0; $x -lt $cx1; $x += 53) {
    $c = $bmp.GetPixel($x, $y); $sample++
    $v = ($c.R + $c.G + $c.B) / 3.0
    $sum += $v; $sumSq += $v * $v
    if ($c.R + $c.G + $c.B -gt 24) { $lit++ }
  }
}

$path = [System.IO.Path]::GetFullPath($Out)
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

$mean = $sum / [math]::Max(1, $sample)
$sd = [math]::Sqrt([math]::Max(0.0, ($sumSq / [math]::Max(1, $sample)) - $mean * $mean))
$pct = [math]::Round(100 * $lit / [math]::Max(1, $sample))
Write-Output ("SHOT {0} {1}x{2} lit={3}% sd={4}" -f $path, $w, $ht, $pct, [math]::Round($sd, 1))
if ($sd -lt 6) { Write-Error ("captured surface is a flat plate (mean {0}, sd {1}) — PrintWindow did not get the GL content" -f [math]::Round($mean), [math]::Round($sd, 1)); exit 7 }
