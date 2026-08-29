#!/usr/bin/env python3
"""Drive the client on a scratch X display and photograph what it draws.

The captures under docs/evidence are the only claim this project makes that its screens look the
way the layout says they do, so taking one has to be repeatable by someone who was not here. By
hand that is an Xvfb, a Gradle run, a wait for the atlases, XTEST taps in the right order, and an
import off the root window — six things to get wrong. This is those six things.

Readiness is read out of the run log rather than off the window, because the box this runs on has
no xwininfo and no xdotool: the atlas line is the last thing the client prints before it is
drawing, and SETTLE is the margin between "drawing" and "drawing the fade-in's last frame".

Shots are cumulative on one client. A traversal capture is Tab, photograph, Tab, photograph, and
that only means anything if it is the same surface each time, so keys are never replayed from the
start and the screen is never reopened between shots.

The display size and the gui scale are both arguments because a screen's layout depends on the
number of gui pixels it gets, and the two together are what decide that. Neither can be left to
the machine: the geometry sizes the Xvfb and the client window as one number, since a client
smaller than the display is a photograph with a mat around it, and gui scale is on `auto` in any
fresh options.txt — and run/ is not in the repo.

    tools/capture.py /tmp/out --geometry 1920x1080 kit:F7 focus-1:Tab focus-2:Tab
"""

import argparse
import os
import pathlib
import re
import shutil
import signal
import subprocess
import sys
import time

from Xlib import X, XK, display as xdisplay
from Xlib.ext import xtest

HERE = pathlib.Path(__file__).resolve().parent
MOD = HERE.parent / "mod"

READY = re.compile(r"Created: \S+ .*minecraft:textures/atlas")
SETTLE = 8.0
TIMEOUT = 240.0


def parse_shot(text):
    name, _, keys = text.partition(":")
    if not name or not keys:
        raise argparse.ArgumentTypeError(f"want name:KEY[,KEY...], got {text!r}")
    return name, [k.strip() for k in keys.split(",") if k.strip()]


def parse_geometry(text):
    found = re.fullmatch(r"(\d+)x(\d+)", text)
    if not found:
        raise argparse.ArgumentTypeError(f"want WxH in device pixels, got {text!r}")
    return int(found.group(1)), int(found.group(2))


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("outdir", type=pathlib.Path)
    ap.add_argument("shots", nargs="+", type=parse_shot, metavar="NAME:KEYS")
    ap.add_argument("--display", default=":9")
    ap.add_argument("--geometry", type=parse_geometry, default=(1280, 720),
                    help="device pixels (default: %(default)s)")
    ap.add_argument("--scale", type=int, default=2,
                    help="gui scale, so gui pixels are geometry over this (default: %(default)s)")
    ap.add_argument("--gap", type=float, default=0.6,
                    help="seconds between a tap and the next thing (default: %(default)s)")
    args = ap.parse_args()

    for tool in ("Xvfb", "import"):
        if not shutil.which(tool):
            sys.exit(f"{tool} is not on PATH")

    width, height = args.geometry
    args.outdir.mkdir(parents=True, exist_ok=True)
    log = args.outdir / "run.log"
    number = args.display.lstrip(":")
    xauth = f"/tmp/i3-xauth{number}"
    pathlib.Path(xauth).write_text("")
    pin(MOD / "run" / "options.txt", {"guiScale": args.scale, "fullscreen": "false"})
    print(f"{width}x{height} at scale {args.scale} "
          f"= {width // args.scale}x{height // args.scale} gui px", flush=True)

    env = {k: v for k, v in os.environ.items()
           if k not in ("WAYLAND_DISPLAY", "XAUTHORITY", "XDG_SESSION_TYPE")}
    env["DISPLAY"] = args.display

    xvfb = subprocess.Popen(
        ["Xvfb", args.display, "-screen", "0", f"{width}x{height}x24", "+extension", "GLX",
         "+extension", "RANDR", "+extension", "XTEST", "-nolisten", "tcp"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    client = None
    try:
        time.sleep(1.5)
        with log.open("wb") as sink:
            client = subprocess.Popen(
                ["./gradlew", "-p", ".", "runClient", "--console=plain",
                 f"-Pclient_width={width}", f"-Pclient_height={height}"],
                cwd=MOD, env=env, stdout=sink, stderr=subprocess.STDOUT,
                start_new_session=True)
            wait_for_atlas(log, client)

        print(f"atlases up; settling {SETTLE:.0f}s", flush=True)
        time.sleep(SETTLE)

        d = xdisplay.Display(args.display)
        # Park the pointer somewhere no control is, so a keyboard-only capture is not silently a
        # hover capture. The content column is centred and never reaches a corner.
        xtest.fake_input(d, X.MotionNotify, x=width - 1, y=height - 1)
        d.sync()
        for name, keys in args.shots:
            for key in keys:
                tap(d, key)
                time.sleep(args.gap)
            shot = args.outdir / f"{name}.png"
            subprocess.run(["import", "-display", args.display, "-window", "root", str(shot)],
                           env={**env, "XAUTHORITY": xauth}, check=True)
            print(f"{shot}  ({' '.join(keys)})", flush=True)
        d.close()
    finally:
        # The Gradle run is a session of its own so that this reaches the JVM under it and not
        # just the wrapper; a client left holding the display is the next run's failure.
        if client and client.poll() is None:
            os.killpg(os.getpgid(client.pid), signal.SIGTERM)
        if xvfb.poll() is None:
            xvfb.terminate()
        for proc in (client, xvfb):
            if proc is None:
                continue
            try:
                proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                proc.kill()


def pin(options, settings):
    """Overwrite a few options.txt lines and leave the rest of the file alone.

    Only the ones a layout would notice. The game rewrites this file on the way out, so pinning is
    something the rig does before every run rather than once by hand.
    """
    options.parent.mkdir(parents=True, exist_ok=True)
    lines = options.read_text(errors="replace").splitlines() if options.exists() else []
    kept = [line for line in lines if line.partition(":")[0] not in settings]
    options.write_text("\n".join(kept + [f"{k}:{v}" for k, v in settings.items()]) + "\n")


def wait_for_atlas(log, client):
    deadline = time.monotonic() + TIMEOUT
    while time.monotonic() < deadline:
        if client.poll() is not None:
            sys.exit(f"client exited {client.returncode} before drawing; see {log}")
        if READY.search(log.read_text(errors="replace")):
            return
        time.sleep(1.0)
    sys.exit(f"no atlas line in {TIMEOUT:.0f}s; see {log}")


def tap(d, name):
    keysym = XK.string_to_keysym(name)
    if keysym == X.NoSymbol:
        sys.exit(f"no keysym named {name!r}")
    code = d.keysym_to_keycode(keysym)
    if not code:
        sys.exit(f"{name} is not on this keyboard")
    xtest.fake_input(d, X.KeyPress, code)
    d.sync()
    time.sleep(0.05)
    xtest.fake_input(d, X.KeyRelease, code)
    d.sync()


if __name__ == "__main__":
    main()
