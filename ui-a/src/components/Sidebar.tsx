import { Home, Cube, Shirt, Server, Gear } from "../lib/icons";

export type Tab = "home" | "mods" | "cosmetics" | "servers" | "settings";

const top: { id: Tab; label: string; Icon: typeof Home }[] = [
  { id: "home", label: "Home", Icon: Home },
  { id: "mods", label: "Mods", Icon: Cube },
  { id: "cosmetics", label: "Cosmetics", Icon: Shirt },
  { id: "servers", label: "Servers", Icon: Server },
];

export default function Sidebar({ tab, onTab }: { tab: Tab; onTab: (t: Tab) => void }) {
  const Btn = ({ id, label, Icon }: { id: Tab; label: string; Icon: typeof Home }) => (
    <button
      className={"railbtn" + (tab === id ? " railbtn--active" : "")}
      onClick={() => onTab(id)}
      aria-label={label}
    >
      <Icon size={20} />
      <span className="rail__label">{label}</span>
    </button>
  );
  return (
    <nav className="rail">
      {top.map((t) => (
        <Btn key={t.id} {...t} />
      ))}
      <div className="rail__grow" />
      <Btn id="settings" label="Settings" Icon={Gear} />
    </nav>
  );
}
