import { useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, ConfirmModal, IconButton, Modal, ProgressBar, Segmented, Slider } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";
import type { Instance } from "../core/bindings";

function relTime(iso: string | null, t: (p: string, v?: Record<string, string | number>) => string): string {
  if (!iso) return t("instances.neverPlayed");
  const diff = Date.now() - new Date(iso).getTime();
  const hours = Math.floor(diff / 3_600_000);
  if (hours < 1) return t("instances.lastPlayed", { when: t("instances.today") });
  if (hours < 24) return t("instances.lastPlayed", { when: t("instances.hoursAgo", { n: hours }) });
  return t("instances.lastPlayed", { when: t("instances.daysAgo", { n: Math.floor(hours / 24) }) });
}

function InstanceCard({ inst }: { inst: Instance }) {
  const { selectInstance, selectedInstanceId, installInstance, deleteInstance, launch, setScreen, modCatalog, versions } = useStore();
  const { t } = useT();
  const [confirmDel, setConfirmDel] = useState(false);
  const selected = inst.id === selectedInstanceId;
  const isTarget = versions.find((v) => v.id === inst.versionId)?.isTarget;

  return (
    <article
      className={`inst-card card ${selected ? "inst-selected gborder" : ""} ${inst.installing ? "inst-busy" : ""}`}
      data-glow
      onClick={() => selectInstance(inst.id)}
    >
      <div className="inst-cover" style={{ "--h": inst.iconHue }}>
        <span className="inst-cover-ver num">{inst.versionId}</span>
        <span className="inst-cover-ring" />
        <span className="dock-chip-cube inst-cover-cube" style={{ "--h": inst.iconHue }}>
          <Icon name="layers" size={15} />
        </span>
        {selected && (
          <span className="inst-selected-mark">
            <Icon name="check" size={13} strokeWidth={2.4} />
          </span>
        )}
      </div>

      <div className="inst-body">
        <div className="inst-head">
          <div className="inst-title">
            <strong>{inst.name}</strong>
            <div className="inst-badges">
              <Badge tone={isTarget ? "accent" : "dim"}>
                {inst.versionId}
                {isTarget ? ` · ${t("instances.target")}` : ""}
              </Badge>
              <Badge tone="dim">{inst.loader.toUpperCase()}</Badge>
            </div>
          </div>
        </div>

        <div className="inst-meta">
          <span>
            <Icon name="ram" size={13} />
            <span className="num">{(inst.memoryMb / 1024).toFixed(0)} GB</span>
          </span>
          <span>
            <Icon name="puzzle" size={13} />
            {t("instances.modsCount", { n: inst.installed ? modCatalog?.mods.length ?? 0 : 0 })}
          </span>
          <span>
            <Icon name="clock" size={13} />
            {relTime(inst.lastPlayedAt, t)}
          </span>
        </div>

        {inst.installing ? (
          <div className="inst-installing">
            <div className="inst-installing-row">
              <span>{t(`dock.stage.${inst.installing.stage}`)}</span>
              <span className="num">{Math.floor(inst.installing.pct)}%</span>
            </div>
            <ProgressBar pct={inst.installing.pct} />
          </div>
        ) : (
          <div className="inst-actions" onClick={(e) => e.stopPropagation()}>
            {inst.installed ? (
              <>
                <Badge tone="ok">{t("instances.installed")}</Badge>
                <span className="inst-actions-spacer" />
                <Button size="sm" variant="soft" icon="play" onClick={() => void launch(inst.id)}>
                  {t("common.play")}
                </Button>
                <IconButton icon="folder" label="open folder" onClick={() => setScreen("settings")} />
                <IconButton icon="trash" label={t("common.delete")} danger onClick={() => setConfirmDel(true)} />
              </>
            ) : (
              <>
                <Badge tone="warn">{t("instances.notInstalled")}</Badge>
                <span className="inst-actions-spacer" />
                <Button size="sm" variant="primary" icon="download" onClick={() => void installInstance(inst.id)}>
                  {t("common.install")}
                </Button>
                <IconButton icon="trash" label={t("common.delete")} danger onClick={() => setConfirmDel(true)} />
              </>
            )}
          </div>
        )}
      </div>

      <ConfirmModal
        open={confirmDel}
        onClose={() => setConfirmDel(false)}
        onConfirm={() => void deleteInstance(inst.id)}
        title={t("instances.deleteTitle")}
        body={t("instances.deleteConfirm")}
        confirmLabel={t("common.delete")}
      />
    </article>
  );
}

function CreateDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { versions, createInstance } = useStore();
  const { t } = useT();
  const [name, setName] = useState("");
  const [versionId, setVersionId] = useState("26.1.2");
  const [loader, setLoader] = useState<"fabric" | "vanilla">("fabric");
  const [memoryMb, setMemoryMb] = useState(4096);
  const [busy, setBusy] = useState(false);

  const releases = versions.filter((v) => v.type === "release");
  const snapshots = versions.filter((v) => v.type === "snapshot");

  const submit = async () => {
    if (!name.trim()) return;
    setBusy(true);
    await createInstance({
      name: name.trim(),
      versionId,
      loader,
      memoryMb,
      iconHue: Math.floor(Math.random() * 360),
    });
    setBusy(false);
    onClose();
    setName("");
  };

  return (
    <Modal open={open} onClose={onClose} title={t("instances.new")} width={460}>
      <div className="field">
        <label className="field-label">{t("instances.name")}</label>
        <input
          className="input"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t("instances.namePlaceholder")}
          autoFocus
          onKeyDown={(e) => e.key === "Enter" && void submit()}
        />
      </div>

      <div className="field">
        <label className="field-label">{t("instances.version")}</label>
        <div className="version-pick">
          <div className="version-group">
            <span className="version-group-label">{t("instances.releases")}</span>
            <div className="version-chips">
              {releases.map((v) => (
                <button
                  key={v.id}
                  className={`version-chip ${versionId === v.id ? "active" : ""}`}
                  onClick={() => setVersionId(v.id)}
                >
                  {v.id}
                  {v.isTarget && <em>{t("instances.target")}</em>}
                </button>
              ))}
            </div>
          </div>
          <div className="version-group">
            <span className="version-group-label">{t("instances.snapshots")}</span>
            <div className="version-chips">
              {snapshots.map((v) => (
                <button
                  key={v.id}
                  className={`version-chip snap ${versionId === v.id ? "active" : ""}`}
                  onClick={() => setVersionId(v.id)}
                >
                  {v.id}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="field">
        <label className="field-label">{t("instances.loader")}</label>
        <Segmented
          options={[
            { value: "fabric", label: "Fabric" },
            { value: "vanilla", label: "Vanilla" },
          ]}
          value={loader}
          onChange={setLoader}
        />
      </div>

      <div className="field">
        <label className="field-label">
          {t("instances.memory")} — <span className="num">{(memoryMb / 1024).toFixed(1)} GB</span>
        </label>
        <Slider
          min={2048}
          max={16384}
          step={512}
          value={memoryMb}
          onChange={setMemoryMb}
          marks={[2048, 4096, 8192, 16384]}
          format={(v) => `${v / 1024}G`}
        />
      </div>

      <div className="modal-actions">
        <Button variant="ghost" onClick={onClose}>
          {t("common.cancel")}
        </Button>
        <Button variant="primary" icon="plus" loading={busy} disabled={!name.trim()} onClick={() => void submit()}>
          {t("instances.create")}
        </Button>
      </div>
    </Modal>
  );
}

export function InstancesScreen() {
  const { instances } = useStore();
  const { t } = useT();
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <div className="screen-pad">
      <header className="page-head">
        <div>
          <h2>{t("instances.title")}</h2>
          <p>{t("instances.subtitle")}</p>
        </div>
        <Button variant="primary" icon="plus" onClick={() => setCreateOpen(true)}>
          {t("instances.new")}
        </Button>
      </header>

      <div className="inst-grid stagger">
        {instances.map((inst) => (
          <InstanceCard key={inst.id} inst={inst} />
        ))}
        <button className="inst-new card" onClick={() => setCreateOpen(true)}>
          <span className="inst-new-icon">
            <Icon name="plus" size={22} />
          </span>
          <span>{t("instances.new")}</span>
        </button>
      </div>

      <CreateDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </div>
  );
}
