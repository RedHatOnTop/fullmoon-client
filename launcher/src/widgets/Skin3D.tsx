import { useEffect, useRef } from "react";
import { SkinViewer, WalkingAnimation, IdleAnimation } from "skinview3d";

type Props = {
  skin?: string;
  cape?: string | null;
  width?: number;
  height?: number;
  walk?: boolean;
  rotate?: boolean;
  /** initial y rotation, radians. PI shows the player's back — where capes are. */
  angle?: number;
  zoom?: number;
  /** accessible name — the figure is content (the player), not decoration */
  label?: string;
};

/* DPR ceiling: a 300×380 canvas at devicePixelRatio 2+ is four times the
   fragments for detail nobody can see on a hairline-UI. */
const MAX_PIXEL_RATIO = 1.75;
/* how long a still figure keeps rendering after something happened — a drag,
   a cape swap — before the loop is allowed to sleep */
const STILL_LINGER_MS = 1400;

export default function Skin3D({
  skin = "/skins/blackcow.png",
  cape = null,
  width = 260,
  height = 380,
  walk = false,
  rotate = true,
  angle = 0,
  zoom = 0.82,
  label,
}: Props) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const viewer = useRef<SkinViewer | null>(null);

  useEffect(() => {
    if (!canvas.current) return;
    const v = new SkinViewer({
      canvas: canvas.current,
      width,
      height,
      skin,
      /* cap at the viewer level, not renderer.setPixelRatio — the composer's
         render targets and FXAA resolution scale off this value, and the
         viewer's own devicePixelRatio listener would quietly undo a
         renderer-only cap on monitor change */
      pixelRatio: Math.min(window.devicePixelRatio || 1, MAX_PIXEL_RATIO),
    });
    v.autoRotate = rotate;
    v.autoRotateSpeed = 0.55;
    v.playerWrapper.rotation.y = angle;
    v.zoom = zoom;
    v.fov = 42;
    v.animation = walk ? new WalkingAnimation() : new IdleAnimation();
    v.controls.enableZoom = false;
    v.controls.enablePan = false;
    viewer.current = v;

    /* A still figure has no reason to burn a render loop: skinview3d drives a
       rAF per viewer at display refresh rate, and four of these on a screen
       is a GPU bill for pixels that never change. The loop sleeps unless the
       figure is animated, the tab is visible, or somebody is actively
       dragging it. */
    let linger = 0;
    const wake = (ms = STILL_LINGER_MS) => {
      if (!viewer.current) return;
      viewer.current.renderPaused = false;
      window.clearTimeout(linger);
      if (!(viewer.current.autoRotate || animatedRef.current)) {
        linger = window.setTimeout(() => {
          if (viewer.current && !document.hidden) viewer.current.renderPaused = true;
        }, ms);
      }
    };
    const onVis = () => (document.hidden ? (viewer.current!.renderPaused = true) : wake());
    const onPointer = () => wake();
    canvas.current.addEventListener("pointerdown", onPointer);
    canvas.current.addEventListener("pointermove", onPointer);
    document.addEventListener("visibilitychange", onVis);
    /* 1.4s, not a shorter splash: loadSkin/loadCape are async image loads and
       a timer that fires before the texture lands freezes an empty canvas */
    wake(STILL_LINGER_MS);

    return () => {
      window.clearTimeout(linger);
      document.removeEventListener("visibilitychange", onVis);
      canvas.current?.removeEventListener("pointerdown", onPointer);
      canvas.current?.removeEventListener("pointermove", onPointer);
      v.dispose();
      viewer.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* whether the figure moves on its own — a ref so the pointer wake can ask
     without re-binding the listeners */
  const animatedRef = useRef(rotate || walk);
  animatedRef.current = rotate || walk;

  /* animated figures own the loop; still figures get one wake to repaint and
     then sleep again */
  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    v.autoRotate = rotate;
    v.animation = walk ? new WalkingAnimation() : new IdleAnimation();
    v.renderPaused = false;
    if (!(rotate || walk)) {
      const t = window.setTimeout(() => {
        if (viewer.current && !document.hidden) viewer.current.renderPaused = true;
      }, STILL_LINGER_MS);
      return () => window.clearTimeout(t);
    }
  }, [rotate, walk]);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    if (cape) v.loadCape(cape);
    else v.resetCape();
    /* a cape arriving is a new pixel the sleeping loop has to draw */
    v.renderPaused = false;
    if (!(v.autoRotate || animatedRef.current)) {
      const t = window.setTimeout(() => {
        if (viewer.current && !document.hidden) viewer.current.renderPaused = true;
      }, STILL_LINGER_MS);
      return () => window.clearTimeout(t);
    }
  }, [cape]);

  return <canvas ref={canvas} role={label ? "img" : undefined} aria-label={label} style={{ width, height, display: "block" }} />;
}
