import { useEffect, useRef } from "react";
import { SkinViewer, WalkingAnimation, IdleAnimation } from "skinview3d";

type Props = {
  skin?: string;
  cape?: string | null;
  width?: number;
  height?: number;
  walk?: boolean;
  rotate?: boolean;
  zoom?: number;
};

export default function Skin3D({
  skin = "/skins/blackcow.png",
  cape = null,
  width = 260,
  height = 380,
  walk = false,
  rotate = true,
  zoom = 0.82,
}: Props) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const viewer = useRef<SkinViewer | null>(null);

  useEffect(() => {
    if (!canvas.current) return;
    const v = new SkinViewer({ canvas: canvas.current, width, height, skin });
    v.autoRotate = rotate;
    v.autoRotateSpeed = 0.55;
    v.zoom = zoom;
    v.fov = 42;
    v.animation = walk ? new WalkingAnimation() : new IdleAnimation();
    v.controls.enableZoom = false;
    v.controls.enablePan = false;
    viewer.current = v;
    return () => {
      v.dispose();
      viewer.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    if (cape) v.loadCape(cape);
    else v.resetCape();
  }, [cape]);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    v.animation = walk ? new WalkingAnimation() : new IdleAnimation();
  }, [walk]);

  return <canvas ref={canvas} style={{ width, height, display: "block" }} />;
}
