import { useState } from "react";
import TitleBar from "./components/TitleBar";
import Sidebar, { type Tab } from "./components/Sidebar";
import PlayBar from "./components/PlayBar";
import LaunchOverlay from "./components/LaunchOverlay";
import Home from "./screens/Home";
import Mods from "./screens/Mods";
import Cosmetics from "./screens/Cosmetics";
import Servers from "./screens/Servers";
import Settings from "./screens/Settings";

const tabs: Tab[] = ["home", "mods", "cosmetics", "servers", "settings"];
const initialTab = (): Tab => {
  const h = location.hash.replace("#", "") as Tab;
  return tabs.includes(h) ? h : "home";
};

export default function App() {
  const [tab, setTab] = useState<Tab>(initialTab);
  const [launching, setLaunching] = useState(() => new URLSearchParams(location.search).has("launch"));
  const go = (t: Tab) => {
    setTab(t);
    history.replaceState(null, "", "#" + t);
  };

  const screen = {
    home: <Home />,
    mods: <Mods />,
    cosmetics: <Cosmetics />,
    servers: <Servers />,
    settings: <Settings />,
  }[tab];

  return (
    <div className="app">
      <div className="app-grain" />
      <TitleBar />
      <div className="body">
        <Sidebar tab={tab} onTab={go} />
        <main key={tab} style={{ minHeight: 0, display: "flex", flexDirection: "column" }}>
          {screen}
        </main>
      </div>
      <PlayBar busy={launching} onPlay={() => setLaunching(true)} />
      {launching && <LaunchOverlay onClose={() => setLaunching(false)} />}
    </div>
  );
}
