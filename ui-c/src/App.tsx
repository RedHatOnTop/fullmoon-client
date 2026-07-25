import { useEffect } from "react";
import { TitleBar } from "./components/TitleBar";
import { Sidebar } from "./components/Sidebar";
import { PlayDock } from "./components/PlayDock";
import { ProgressDock, Toasts } from "./components/Docks";
import { Logo } from "./components/Logo";
import { HomeScreen } from "./screens/Home";
import { InstancesScreen } from "./screens/Instances";
import { ModsScreen } from "./screens/Mods";
import { CosmeticsScreen } from "./screens/Cosmetics";
import { AccountsScreen } from "./screens/Accounts";
import { SettingsScreen } from "./screens/Settings";
import { ConsoleScreen } from "./screens/Console";
import { useStore } from "./state/store";
import { useT } from "./i18n";

const SCREENS = {
  home: HomeScreen,
  instances: InstancesScreen,
  mods: ModsScreen,
  cosmetics: CosmeticsScreen,
  accounts: AccountsScreen,
  settings: SettingsScreen,
  console: ConsoleScreen,
} as const;

export default function App() {
  const { ready, screen, settings } = useStore();
  const { setLang } = useT();

  /* keep the i18n provider in sync with the persisted setting */
  useEffect(() => {
    if (settings) setLang(settings.language);
  }, [settings, setLang]);

  /* cursor-tracked glow — feeds --mx/--my to any hovered [data-glow] */
  useEffect(() => {
    let raf = 0;
    const onMove = (e: PointerEvent) => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        const el = (e.target as Element | null)?.closest?.("[data-glow]");
        if (!el) return;
        const r = el.getBoundingClientRect();
        (el as HTMLElement).style.setProperty("--mx", `${e.clientX - r.left}px`);
        (el as HTMLElement).style.setProperty("--my", `${e.clientY - r.top}px`);
      });
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    return () => {
      window.removeEventListener("pointermove", onMove);
      cancelAnimationFrame(raf);
    };
  }, []);

  if (!ready) {
    return (
      <div className="app-splash">
        <Logo size={44} withWord={false} />
      </div>
    );
  }

  const Screen = SCREENS[screen];

  return (
    <div className="app">
      <TitleBar />
      <div className="backdrop" aria-hidden>
        <span className="blob blob-1" />
        <span className="blob blob-2" />
        <span className="blob blob-3" />
        <div className="grid-lines" />
        <div className="grain" />
      </div>
      <div className="shell">
        <Sidebar />
        <div className="main">
          <main className="content">
            <div className="content-inner screen-enter" key={screen}>
              <Screen />
            </div>
          </main>
          <PlayDock />
        </div>
      </div>
      <Toasts />
      <ProgressDock />
    </div>
  );
}
