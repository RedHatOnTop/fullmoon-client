import { useEffect, useMemo, useState, type ReactNode } from 'react';

type View = 'home' | 'instances' | 'mods' | 'cosmetics' | 'settings';
type LaunchState = 'idle' | 'checking' | 'launching' | 'running';
type IconName = 'home' | 'grid' | 'mods' | 'shirt' | 'settings' | 'play' | 'chevron' | 'search' | 'bell' | 'plus' | 'more' | 'server' | 'download' | 'spark' | 'cpu' | 'memory' | 'folder' | 'terminal' | 'close' | 'check' | 'power' | 'shield' | 'globe' | 'copy' | 'external';

const iconPaths: Record<IconName, ReactNode> = {
  home: <><path d="m3 11 9-8 9 8"/><path d="M5 10v10h14V10M9 20v-6h6v6"/></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
  mods: <><path d="M8 2v4M16 2v4M8 18v4M16 18v4M2 8h4M18 8h4M2 16h4M18 16h4"/><rect x="6" y="6" width="12" height="12" rx="3"/><path d="m9 14 2-4 2 4 2-4"/></>,
  shirt: <path d="M8 4 4 6l-2 5 4 2v8h12v-8l4-2-2-5-4-2c-.8 2-7.2 2-8 0Z"/>,
  settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6v-.2h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z"/></>,
  play: <path d="m8 5 11 7-11 7Z"/>, chevron: <path d="m9 18 6-6-6-6"/>,
  search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>, bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
  plus: <path d="M12 5v14M5 12h14"/>, more: <><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></>,
  server: <><rect x="3" y="4" width="18" height="6" rx="2"/><rect x="3" y="14" width="18" height="6" rx="2"/><path d="M7 7h.01M7 17h.01M11 7h6M11 17h6"/></>,
  download: <><path d="M12 3v12m0 0 5-5m-5 5-5-5"/><path d="M5 21h14"/></>, spark: <path d="m12 2 1.5 6.5L20 10l-6.5 1.5L12 18l-1.5-6.5L4 10l6.5-1.5Z"/>,
  cpu: <><rect x="6" y="6" width="12" height="12" rx="2"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M1 9h3M20 9h3M1 15h3M20 15h3M10 10h4v4h-4z"/></>, memory: <><path d="M5 4v16M19 4v16M5 7h14M5 17h14"/><path d="M9 10v4M12 10v4M15 10v4"/></>,
  folder: <path d="M3 6h7l2 2h9v11H3Z"/>, terminal: <><path d="m5 7 5 5-5 5M12 17h7"/></>, close: <path d="m6 6 12 12M18 6 6 18"/>,
  check: <path d="m5 12 4 4L19 6"/>, power: <><path d="M12 2v10"/><path d="M18.4 6.6a9 9 0 1 1-12.8 0"/></>, shield: <path d="M12 3 4 6v5c0 5 3.4 8.7 8 10 4.6-1.3 8-5 8-10V6Z"/>,
  globe: <><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3.5 3 14 0 18M12 3c-3 3.5-3 14 0 18"/></>, copy: <><rect x="8" y="8" width="11" height="11" rx="2"/><path d="M16 8V5H5v11h3"/></>, external: <><path d="M14 4h6v6M20 4l-9 9"/><path d="M18 13v7H4V6h7"/></>,
};

function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return <svg className="icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden>{iconPaths[name]}</svg>;
}

const navItems: { id: View; label: string; icon: IconName }[] = [
  { id: 'home', label: 'Home', icon: 'home' }, { id: 'instances', label: 'Instances', icon: 'grid' },
  { id: 'mods', label: 'Mod library', icon: 'mods' }, { id: 'cosmetics', label: 'Wardrobe', icon: 'shirt' },
];
const viewMeta: Record<View, [string, string]> = {
  home: ['Good evening, Alex', 'Everything is ready for takeoff.'],
  instances: ['Your instances', 'Independent worlds, perfectly tuned.'],
  mods: ['Mod library', 'Performance without the guesswork.'],
  cosmetics: ['Wardrobe', 'Make every landing unmistakably yours.'],
  settings: ['Settings', 'Fine-tune your launcher and game.'],
};
const news = [
  { type: 'RELEASE', title: 'Pinion 0.8 takes flight', body: 'A sharper HUD, faster starts, and a completely rebuilt cosmetics renderer.', date: 'JUL 18', tint: 'blue' },
  { type: 'COMMUNITY', title: 'Summer build challenge', body: 'Build beyond the clouds. Three weeks, one seed, and a legendary cape.', date: 'JUL 12', tint: 'violet' },
  { type: 'PATCH NOTES', title: '26.1.2 performance pass', body: 'Frame pacing refinements and lower memory pressure on long sessions.', date: 'JUL 08', tint: 'green' },
];
const servers = [
  { name: 'Hypixel', host: 'mc.hypixel.net', players: '43,281', ping: 32, mark: 'H', color: '#e99d32' },
  { name: 'MCC Island', host: 'play.mccisland.net', players: '2,194', ping: 41, mark: 'M', color: '#e95f6c' },
  { name: 'Origin Realms', host: 'play.originrealms.com', players: '842', ping: 58, mark: 'O', color: '#61b887' },
];
const initialInstances = [
  { id: 1, name: 'Pinion — 26.1.2', kind: 'Primary', version: 'Fabric 0.18.4', played: 'Today', icon: 'P', color: '#79aaff', active: true },
  { id: 2, name: 'Vanilla worlds', kind: 'Clean room', version: 'Minecraft 1.21.8', played: '3 days ago', icon: 'V', color: '#74c69d', active: false },
  { id: 3, name: 'Create: Astral', kind: 'Modpack', version: 'Fabric 1.20.1', played: '2 weeks ago', icon: 'A', color: '#c49a6c', active: false },
];
const initialMods = [
  { id: 'pinion-hud', name: 'Pinion HUD', author: 'Pinion Labs', desc: 'A precise, composable HUD built to disappear when you do not need it.', version: '0.8.0', type: 'FIRST PARTY', enabled: true, color: '#78a9ff', glyph: 'P' },
  { id: 'sodium', name: 'Sodium', author: 'CaffeineMC', desc: 'Modern rendering engine that dramatically improves frame rates and micro-stutter.', version: '0.6.13', type: 'PERFORMANCE', enabled: true, color: '#67c98f', glyph: 'S' },
  { id: 'lithium', name: 'Lithium', author: 'CaffeineMC', desc: 'Optimizes game physics, mob AI, block ticking, and world simulation.', version: '0.15.0', type: 'PERFORMANCE', enabled: true, color: '#9c82e8', glyph: 'Li' },
  { id: 'iris', name: 'Iris Shaders', author: 'Iris Team', desc: 'Beautiful shader packs with excellent compatibility and performance.', version: '1.8.8', type: 'VISUAL', enabled: false, color: '#e8a45e', glyph: 'I' },
  { id: 'voice', name: 'Simple Voice Chat', author: 'henkelmax', desc: 'Proximity voice chat with groups and carefully tuned audio controls.', version: '2.5.35', type: 'SOCIAL', enabled: false, color: '#e36f8e', glyph: 'V' },
];
const cosmetics = [
  { id: 'aurora', name: 'Aurora Flight', type: 'WINGS', rarity: 'MYTHIC', colors: ['#6ea8ff', '#a877f5'] },
  { id: 'founder', name: "Founder's Cape", type: 'CAPE', rarity: 'LEGENDARY', colors: ['#202a45', '#6ea8ff'] },
  { id: 'ember', name: 'Ember Wings', type: 'WINGS', rarity: 'EPIC', colors: ['#fa7c64', '#f2b45f'] },
  { id: 'orbit', name: 'Low Orbit', type: 'CAPE', rarity: 'RARE', colors: ['#232b42', '#6dd6cb'] },
  { id: 'halo', name: 'Blue Halo', type: 'HEAD', rarity: 'EPIC', colors: ['#b9e8ff', '#719aff'] },
  { id: 'none', name: 'No cosmetic', type: 'CLEAR', rarity: 'COMMON', colors: ['#222836', '#343d51'] },
];

function Toggle({ checked, onChange, label }: { checked: boolean; onChange: () => void; label?: string }) {
  return <button className={`toggle ${checked ? 'on' : ''}`} onClick={onChange} aria-label={label} aria-pressed={checked}><span /></button>;
}

function BrandMark({ small = false }: { small?: boolean }) {
  return <div className={`brand-mark ${small ? 'small' : ''}`}><i className="feather f1"/><i className="feather f2"/><i className="feather f3"/><b /></div>;
}

function App() {
  const [view, setView] = useState<View>('home');
  const [launch, setLaunch] = useState<LaunchState>('idle');
  const [server, setServer] = useState<string | null>(null);
  const [consoleOpen, setConsoleOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [instances, setInstances] = useState(initialInstances);
  const [mods, setMods] = useState(initialMods);
  const [equipped, setEquipped] = useState('aurora');
  const [ram, setRam] = useState(6);
  const [telemetry, setTelemetry] = useState(false);
  const [hud, setHud] = useState({ fps: true, cps: true, keys: true, coords: false, armor: true, potions: false, ping: true });

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); setCommandOpen(v => !v); }
      if (event.key === 'Escape') { setCommandOpen(false); setAccountOpen(false); setCreateOpen(false); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  useEffect(() => { if (!toast) return; const id = window.setTimeout(() => setToast(null), 2600); return () => clearTimeout(id); }, [toast]);

  const activeMods = useMemo(() => mods.filter(m => m.enabled).length, [mods]);
  const activeCosmetic = cosmetics.find(c => c.id === equipped) ?? cosmetics[0];

  function startGame(target?: string) {
    if (launch !== 'idle') { setConsoleOpen(true); return; }
    setServer(target ?? null); setConsoleOpen(true); setLaunch('checking');
    window.setTimeout(() => setLaunch('launching'), 900);
    window.setTimeout(() => { setLaunch('running'); setToast(target ? `Connected to ${target}` : 'Minecraft is running'); }, 2100);
  }
  function stopGame() { setLaunch('idle'); setServer(null); setToast('Game session ended safely'); }
  function switchInstance(id: number) { setInstances(items => items.map(i => ({ ...i, active: i.id === id }))); setToast('Active instance changed'); }
  function addInstance() {
    setInstances(items => [...items, { id: Date.now(), name: 'New survival', kind: 'Custom', version: 'Fabric 26.1.2', played: 'Never', icon: 'N', color: '#e4a76c', active: false }]);
    setCreateOpen(false); setToast('New instance created');
  }

  function renderHome() {
    return <div className="home-page page-enter">
      <section className="hero">
        <div className="hero-grid"/><div className="hero-glow"/><div className="hero-islands"><i/><i/><i/><span/></div>
        <div className="hero-copy">
          <div className="eyebrow"><span className="live-dot"/> READY TO LAUNCH <em>26.1.2</em></div>
          <h1>Built lighter.<br/><strong>Fly further.</strong></h1>
          <p>Your worlds, sharpened. Pinion keeps every frame smooth<br/>and every adventure one click away.</p>
          <div className="hero-actions">
            <button className={`play-button state-${launch}`} onClick={() => startGame()}>
              <span className="play-icon"><Icon name={launch === 'running' ? 'terminal' : 'play'} size={20}/></span>
              <span><b>{launch === 'idle' ? 'PLAY' : launch === 'checking' ? 'VERIFYING' : launch === 'launching' ? 'LIFTING OFF' : 'OPEN CONSOLE'}</b><small>{launch === 'idle' ? 'Pinion — 26.1.2' : launch === 'running' ? 'Minecraft is running' : 'Preparing your instance…'}</small></span>
              {launch !== 'idle' && launch !== 'running' ? <i className="spinner"/> : <Icon name="chevron" size={18}/>} 
            </button>
            <button className="round-action" aria-label="Instance options"><Icon name="more"/></button>
          </div>
          <div className="hero-meta"><span><Icon name="shield" size={14}/> Files verified</span><span><Icon name="mods" size={14}/> {activeMods} mods active</span><span><Icon name="memory" size={14}/> {ram} GB allocated</span></div>
        </div>
        <div className="hero-version"><span>PINION BUILD</span><b>0.8.0</b><small>Flight channel</small></div>
      </section>

      <div className="section-heading"><div><span>QUICK PLAY</span><h2>Your servers</h2></div><button className="text-button">Manage servers <Icon name="chevron" size={15}/></button></div>
      <section className="server-row">
        {servers.map((item, index) => <article className="server-card" key={item.name} style={{ '--delay': `${index * 70}ms` } as React.CSSProperties}>
          <div className="server-mark" style={{ '--mark': item.color } as React.CSSProperties}>{item.mark}</div>
          <div className="server-info"><b>{item.name}</b><span>{item.host}</span></div>
          <div className="server-pop"><span className="signal"><i/><i/><i/></span><b>{item.players}</b><small>online</small></div>
          <button onClick={() => startGame(item.name)}><Icon name="play" size={15}/> JOIN</button>
        </article>)}
        <button className="add-server" onClick={() => setToast('Server browser opened')}><Icon name="plus"/><span>Add server</span></button>
      </section>

      <div className="section-heading news-heading"><div><span>FROM THE NEST</span><h2>Latest dispatches</h2></div><button className="text-button">View all <Icon name="chevron" size={15}/></button></div>
      <section className="news-row">
        {news.map((item, index) => <article className={`news-card ${item.tint}`} key={item.title} style={{ '--delay': `${index * 80}ms` } as React.CSSProperties}>
          <div className="news-art"><div className="pixel-sun"/><div className="pixel-land l1"/><div className="pixel-land l2"/><BrandMark small/></div>
          <div className="news-body"><div><span>{item.type}</span><time>{item.date}</time></div><h3>{item.title}</h3><p>{item.body}</p><button>Read dispatch <Icon name="chevron" size={14}/></button></div>
        </article>)}
      </section>
    </div>;
  }

  function renderInstances() {
    return <div className="page-enter content-page">
      <div className="page-tools"><div className="segmented"><button className="active">All <span>{instances.length}</span></button><button>Installed</button><button>Archived</button></div><button className="primary-small" onClick={() => setCreateOpen(true)}><Icon name="plus"/> New instance</button></div>
      <section className="instance-feature">
        <div className="instance-orb"><BrandMark/><span>26.1</span></div>
        <div><span className="kicker">ACTIVE INSTANCE</span><h2>Pinion — 26.1.2</h2><p>Optimized Fabric profile · Last played today for 2h 14m</p><div className="tag-row"><span>Fabric 0.18.4</span><span>{activeMods} mods</span><span>Verified</span></div></div>
        <div className="feature-actions"><button className="primary-small" onClick={() => startGame()}><Icon name="play"/> Play now</button><button className="icon-button"><Icon name="more"/></button></div>
      </section>
      <div className="list-title"><span>ALL INSTANCES</span><button><Icon name="folder"/> Open instances folder</button></div>
      <section className="instance-list">{instances.map(item => <article className={item.active ? 'active' : ''} key={item.id}>
        <button className="instance-main" onClick={() => switchInstance(item.id)}><span className="instance-icon" style={{ '--instance': item.color } as React.CSSProperties}>{item.icon}</span><span><b>{item.name}</b><small>{item.kind} · {item.version}</small></span></button>
        <div className="instance-size"><b>{item.id === 1 ? '1.84 GB' : item.id === 2 ? '712 MB' : item.id === 3 ? '4.21 GB' : '0 B'}</b><small>ON DISK</small></div>
        <div className="last-played"><b>{item.played}</b><small>LAST PLAYED</small></div>
        {item.active ? <span className="active-pill"><i/> ACTIVE</span> : <button className="select-button" onClick={() => switchInstance(item.id)}>Select</button>}
        <button className="icon-button"><Icon name="more"/></button>
      </article>)}</section>
      <div className="storage-bar"><Icon name="download"/><div><b>Storage</b><span><i style={{ width: '38%' }}/></span><small>6.8 GB of 18 GB launcher storage used</small></div><button>Manage</button></div>
    </div>;
  }

  function renderMods() {
    return <div className="page-enter content-page">
      <section className="mod-summary"><div><span className="kicker">ACTIVE LOADOUT</span><h2>{activeMods} mods working in formation</h2><p>Pinion checks every mod against your game version before launch.</p></div><div className="performance-score"><span>+<b>186</b>%</span><small>EST. FPS GAIN</small></div><div className="score-ring"><strong>98</strong><small>COMPATIBILITY</small></div></section>
      <div className="page-tools"><div className="segmented"><button className="active">Installed <span>{mods.length}</span></button><button>Discover</button><button>Updates <span>2</span></button></div><label className="search-field"><Icon name="search"/><input placeholder="Search your mods"/></label></div>
      <section className="mod-list">{mods.map(mod => <article key={mod.id}>
        <div className="mod-icon" style={{ '--mod': mod.color } as React.CSSProperties}>{mod.glyph}</div>
        <div className="mod-copy"><div><h3>{mod.name}</h3><span>{mod.type}</span></div><p>{mod.desc}</p><small>by {mod.author} · v{mod.version}</small></div>
        <div className="compat"><Icon name="check" size={14}/><span>26.1.2</span></div>
        <Toggle checked={mod.enabled} label={`Toggle ${mod.name}`} onChange={() => setMods(items => items.map(item => item.id === mod.id ? { ...item, enabled: !item.enabled } : item))}/>
        <button className="icon-button"><Icon name="more"/></button>
      </article>)}</section>
    </div>;
  }

  function renderCosmetics() {
    return <div className="page-enter wardrobe-page">
      <section className="player-stage">
        <div className="stage-grid"/><div className="stage-halo h1"/><div className="stage-halo h2"/>
        {equipped !== 'none' && <div className="wings" style={{ '--wing-a': activeCosmetic.colors[0], '--wing-b': activeCosmetic.colors[1] } as React.CSSProperties}><i className="wing left"/><i className="wing right"/></div>}
        <div className="player-model"><div className="head"><i/><b/></div><div className="body"><span className="logo-stitch">P</span></div><div className="arm left"/><div className="arm right"/><div className="leg left"/><div className="leg right"/></div>
        <div className="stage-controls"><button>−</button><span>Drag to rotate</span><button>+</button></div>
        <div className="equipped-label"><span>EQUIPPED</span><b>{activeCosmetic.name}</b><small>{activeCosmetic.type} · Visible to you</small></div>
      </section>
      <section className="wardrobe-panel">
        <div className="wardrobe-tabs"><button className="active">All</button><button>Capes</button><button>Wings</button><button>Headwear</button></div>
        <div className="collection-title"><div><span>YOUR COLLECTION</span><b>{cosmetics.length - 1} / 24 unlocked</b></div><button><Icon name="grid"/> Sort</button></div>
        <div className="cosmetic-grid">{cosmetics.map(item => <button className={equipped === item.id ? 'selected' : ''} key={item.id} onClick={() => { setEquipped(item.id); setToast(item.id === 'none' ? 'Cosmetics cleared' : `${item.name} equipped`); }}>
          <div className="cosmetic-art" style={{ '--cos-a': item.colors[0], '--cos-b': item.colors[1] } as React.CSSProperties}><span>{item.id === 'none' ? '×' : item.type === 'CAPE' ? '◧' : item.type === 'HEAD' ? '◯' : '◆'}</span>{equipped === item.id && <i><Icon name="check" size={12}/></i>}</div>
          <span className={`rarity ${item.rarity.toLowerCase()}`}>{item.rarity}</span><b>{item.name}</b><small>{item.type}</small>
        </button>)}</div>
        <div className="visibility-note"><Icon name="globe"/><div><b>Client-side cosmetics</b><span>Your equipped items render locally. Multiplayer visibility is coming later.</span></div><button>Learn more</button></div>
      </section>
    </div>;
  }

  function renderSettings() {
    const hudLabels: Record<keyof typeof hud, string> = { fps: 'FPS counter', cps: 'Clicks per second', keys: 'Keystrokes', coords: 'Coordinates', armor: 'Armor status', potions: 'Potion effects', ping: 'Server ping' };
    return <div className="page-enter settings-page">
      <aside className="settings-nav"><span>LAUNCHER</span><button className="active"><Icon name="settings"/> General</button><button><Icon name="cpu"/> Game & Java</button><button><Icon name="download"/> Downloads</button><span>IN-GAME</span><button><Icon name="grid"/> HUD modules</button><button><Icon name="shirt"/> Cosmetics</button><span>PINION</span><button><Icon name="shield"/> Privacy</button><button><Icon name="terminal"/> Advanced</button></aside>
      <section className="settings-content">
        <div className="settings-group"><div className="group-heading"><div><span>GENERAL</span><h2>Launcher preferences</h2></div><small>Changes save automatically</small></div>
          <div className="setting-row"><div><b>Launch behavior</b><span>What Pinion does when Minecraft starts</span></div><select defaultValue="minimize"><option value="minimize">Minimize launcher</option><option>Keep launcher open</option><option>Close launcher</option></select></div>
          <div className="setting-row"><div><b>Theme</b><span>Choose your flight deck appearance</span></div><div className="theme-picker"><button className="active"><i/> Midnight</button><button><i/> Light</button><button><i/> System</button></div></div>
          <div className="setting-row"><div><b>Language</b><span>Language used throughout Pinion</span></div><select defaultValue="English (US)"><option>English (US)</option><option>한국어</option><option>日本語</option></select></div>
        </div>
        <div className="settings-group"><div className="group-heading"><div><span>PERFORMANCE</span><h2>Memory & runtime</h2></div></div>
          <div className="setting-row slider-setting"><div><b>Allocated memory</b><span>Recommended for this instance: 4–8 GB</span></div><div className="range-wrap"><strong>{ram} GB</strong><input type="range" min="2" max="16" value={ram} onChange={e => setRam(Number(e.target.value))} style={{ '--range': `${((ram - 2) / 14) * 100}%` } as React.CSSProperties}/><div><span>2 GB</span><span>16 GB</span></div></div></div>
          <div className="setting-row"><div><b>Java runtime</b><span>Pinion-managed Java 21 · Recommended</span></div><button className="path-button"><Icon name="folder"/> C:\Pinion\runtime\java-21 <span>Change</span></button></div>
        </div>
        <div className="settings-group"><div className="group-heading"><div><span>HUD MODULES</span><h2>In-game overlay</h2></div><small>Synced with 26.1.2</small></div><div className="hud-grid">{(Object.keys(hud) as (keyof typeof hud)[]).map(key => <div key={key}><span className={`hud-mini ${key}`}>{key === 'keys' ? 'W A S D' : key.toUpperCase()}</span><b>{hudLabels[key]}</b><Toggle checked={hud[key]} onChange={() => setHud(current => ({ ...current, [key]: !current[key] }))}/></div>)}</div></div>
        <div className="settings-group"><div className="setting-row"><div><b>Anonymous diagnostics</b><span>Share crash data to help improve Pinion. Never includes worlds or personal data.</span></div><Toggle checked={telemetry} onChange={() => setTelemetry(v => !v)}/></div></div>
      </section>
    </div>;
  }

  const logs = launch === 'idle' ? ['Session ready. Waiting for launch request.'] : launch === 'checking' ? ['[Pinion] Running preflight checks…', '[Integrity] Verifying 1,284 game files', '[Java] Runtime 21.0.7 detected'] : launch === 'launching' ? ['[Integrity] All files verified ✓', '[Fabric] Loading 3 enabled mods', `[Launcher] Starting Minecraft 26.1.2${server ? ` → ${server}` : ''}`, '[Render thread/INFO] Initializing Pinion HUD'] : ['[Integrity] All files verified ✓', '[Fabric] Loaded Pinion HUD, Sodium, Lithium', `[Server thread/INFO] ${server ? `Connecting to ${server}` : 'Integrated server ready'}`, '[Render thread/INFO] OpenGL initialized · Sodium renderer active', '[Pinion] Game process is healthy · 186 FPS'];

  return <div className="app-shell">
    <div className="ambient a1"/><div className="ambient a2"/>
    <header className="titlebar"><div className="window-brand"><BrandMark small/><strong>PINION</strong><span>LAUNCHER</span></div><div className="drag-zone"/><div className="window-actions"><button aria-label="Minimize">—</button><button aria-label="Maximize">□</button><button className="window-close" aria-label="Close">×</button></div></header>
    <aside className="sidebar">
      <div className="side-nav"><span className="nav-label">FLIGHT DECK</span>{navItems.map(item => <button className={view === item.id ? 'active' : ''} key={item.id} onClick={() => setView(item.id)}><Icon name={item.icon}/><span>{item.label}</span>{item.id === 'mods' && <i className="nav-count">2</i>}</button>)}</div>
      <div className="side-bottom"><span className="nav-label">SYSTEM</span><button className={view === 'settings' ? 'active' : ''} onClick={() => setView('settings')}><Icon name="settings"/><span>Settings</span></button><div className="system-health"><i/><div><b>All systems nominal</b><span>Services operational</span></div></div></div>
    </aside>
    <main className={`main-area ${consoleOpen ? 'with-console' : ''}`}>
      <header className="topbar"><div><h1>{viewMeta[view][0]}</h1><p>{viewMeta[view][1]}</p></div><div className="top-actions"><button className="search-button" onClick={() => setCommandOpen(true)}><Icon name="search"/><span>Search anything</span><kbd>Ctrl K</kbd></button><button className="notification"><Icon name="bell"/><i/></button><div className="top-divider"/><button className="account" onClick={() => setAccountOpen(v => !v)}><span className="avatar"><i/><b/></span><span><b>AlexRivers</b><small>Microsoft account</small></span><Icon name="chevron" size={15}/></button></div></header>
      <div className="viewport">{view === 'home' ? renderHome() : view === 'instances' ? renderInstances() : view === 'mods' ? renderMods() : view === 'cosmetics' ? renderCosmetics() : renderSettings()}</div>
    </main>

    {consoleOpen && <aside className={`console-dock ${launch}`}>
      <header><div><span className="console-status"><i/>{launch === 'idle' ? 'SESSION CLOSED' : launch === 'checking' ? 'PREFLIGHT' : launch === 'launching' ? 'STARTING' : 'GAME RUNNING'}</span><b>{server ?? 'Pinion — 26.1.2'}</b></div><div><button onClick={() => navigator.clipboard?.writeText(logs.join('\n'))}><Icon name="copy" size={15}/> Copy</button>{launch === 'running' && <button className="stop-button" onClick={stopGame}><Icon name="power" size={15}/> Stop game</button>}<button className="icon-button" onClick={() => setConsoleOpen(false)}><Icon name="close" size={16}/></button></div></header>
      <div className="console-body">{logs.map((line, i) => <p key={`${line}-${i}`}><span>{new Date(Date.now() - (logs.length - i) * 380).toLocaleTimeString([], { hour12: false })}</span><code className={line.includes('✓') || line.includes('healthy') ? 'success' : ''}>{line}</code></p>)}{launch !== 'idle' && <i className="cursor"/>}</div>
      {(launch === 'checking' || launch === 'launching') && <div className="launch-progress"><i/><span>{launch === 'checking' ? 'Verifying game files' : 'Starting Java virtual machine'}</span><b>{launch === 'checking' ? '42%' : '78%'}</b></div>}
    </aside>}
    {accountOpen && <div className="account-menu popup"><div className="account-profile"><span className="avatar large"><i/><b/></span><div><b>AlexRivers</b><span>4f9c…a21d</span></div><i className="selected-check"><Icon name="check" size={12}/></i></div><button><Icon name="plus"/> Add Microsoft account</button><button><Icon name="download"/> Import from official launcher</button><hr/><button><Icon name="external"/> Manage account</button></div>}
    {commandOpen && <div className="modal-backdrop" onMouseDown={() => setCommandOpen(false)}><div className="command-palette" onMouseDown={e => e.stopPropagation()}><div className="command-search"><Icon name="search"/><input autoFocus placeholder="Where do you want to go?"/><kbd>ESC</kbd></div><span>QUICK ACTIONS</span><button onClick={() => { setCommandOpen(false); startGame(); }}><i><Icon name="play"/></i><div><b>Launch Pinion — 26.1.2</b><small>Start your active instance</small></div><kbd>↵</kbd></button>{navItems.slice(1).map(item => <button key={item.id} onClick={() => { setView(item.id); setCommandOpen(false); }}><i><Icon name={item.icon}/></i><div><b>Open {item.label}</b><small>Go to {item.label.toLowerCase()}</small></div></button>)}<footer><span><kbd>↑</kbd><kbd>↓</kbd> Navigate</span><span><kbd>↵</kbd> Select</span></footer></div></div>}
    {createOpen && <div className="modal-backdrop" onMouseDown={() => setCreateOpen(false)}><div className="create-dialog" onMouseDown={e => e.stopPropagation()}><header><div><span>NEW FLIGHT PROFILE</span><h2>Create an instance</h2></div><button onClick={() => setCreateOpen(false)}><Icon name="close"/></button></header><label>INSTANCE NAME<input autoFocus defaultValue="New survival"/></label><div className="dialog-option selected"><i><BrandMark small/></i><div><b>Pinion optimized</b><span>Minecraft 26.1.2 · Fabric · Performance bundle</span></div><Icon name="check"/></div><div className="dialog-option"><i><Icon name="grid"/></i><div><b>Clean Minecraft</b><span>Start with an unmodified installation</span></div></div><footer><button onClick={() => setCreateOpen(false)}>Cancel</button><button className="primary-small" onClick={addInstance}><Icon name="plus"/> Create instance</button></footer></div></div>}
    {toast && <div className="toast"><span><Icon name="check" size={15}/></span><div><b>All set</b><small>{toast}</small></div><button onClick={() => setToast(null)}><Icon name="close" size={14}/></button></div>}
  </div>;
}

export default App;
