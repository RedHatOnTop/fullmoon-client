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

    tools/capture.py /tmp/out kit:F7 focus-1:Tab focus-2:Tab
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
GEOMETRY = "1280x720x24"

#: Somewhere no control is, so a keyboard-only capture is not silently a hover capture. The
#: content column is centred and never this wide, whatever the screen.
PARKED = (1279, 719)


def parse_shot(text):
    name, _, keys = text.partition(":")
    if not name or not keys:
        raise argparse.ArgumentTypeError(f"want name:KEY[,KEY...], got {text!r}")
    return name, [k.strip() for k in keys.split(",") if k.strip()]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("outdir", type=pathlib.Path)
    ap.add_argument("shots", nargs="+", type=parse_shot, metavar="NAME:KEYS")
    ap.add_argument("--display", default=":9")
    ap.add_argument("--gap", type=float, default=0.6,
                    help="seconds between a tap and the next thing (default: %(default)s)")
    args = ap.parse_args()

    for tool in ("Xvfb", "import"):
        if not shutil.which(tool):
            sys.exit(f"{tool} is not on PATH")

    args.outdir.mkdir(parents=True, exist_ok=True)
    log = args.outdir / "run.log"
    number = args.display.lstrip(":")
    xauth = f"/tmp/i3-xauth{number}"
    pathlib.Path(xauth).write_text("")

    env = {k: v for k, v in os.environ.items()
           if k not in ("WAYLAND_DISPLAY", "XAUTHORITY", "XDG_SESSION_TYPE")}
    env["DISPLAY"] = args.display

    xvfb = subprocess.Popen(
        ["Xvfb", args.display, "-screen", "0", GEOMETRY, "+extension", "GLX",
         "+extension", "RANDR", "+extension", "XTEST", "-nolisten", "tcp"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    client = None
    try:
        time.sleep(1.5)
        with log.open("wb") as sink:
            client = subprocess.Popen(
                ["./gradlew", "-p", ".", "runClient", "--console=plain"],
                cwd=MOD, env=env, stdout=sink, stderr=subprocess.STDOUT,
                start_new_session=True)
            wait_for_atlas(log, client)

        print(f"atlases up; settling {SETTLE:.0f}s", flush=True)
        time.sleep(SETTLE)

        d = xdisplay.Display(args.display)
        xtest.fake_input(d, X.MotionNotify, x=PARKED[0], y=PARKED[1])
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
