import { useEffect, useRef, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, ConfirmModal, IconButton, Modal, SkinFace } from "../components/ui";
import { addOfflineAccount, core, errText, openExternal } from "../core/client";
import type { Account, AuthStatus, DeviceCodePrompt } from "../core/bindings";
import { useStore } from "../state/store";
import { useT } from "../i18n";

/* ── device-code flow ── */

function DeviceCodeModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { syncAccounts, toast } = useStore();
  const { t } = useT();
  const [prompt, setPrompt] = useState<DeviceCodePrompt | null>(null);
  const [dots, setDots] = useState(1);
  const alive = useRef(false);

  useEffect(() => {
    if (!open) return;
    alive.current = true;
    setPrompt(null);
    let timer: ReturnType<typeof setInterval>;
    let dotTimer: ReturnType<typeof setInterval>;

    const fail = (e: unknown) => {
      clearInterval(timer);
      clearInterval(dotTimer);
      toast("error", errText(e));
      onClose();
    };

    (async () => {
      let p: DeviceCodePrompt;
      try {
        p = await core.auth_begin_device_code();
      } catch (e) {
        if (alive.current) fail(e);
        return;
      }
      if (!alive.current) return;
      setPrompt(p);
      dotTimer = setInterval(() => setDots((d) => (d % 3) + 1), 500);
      timer = setInterval(async () => {
        let status: AuthStatus;
        try {
          status = await core.auth_poll(p.session);
        } catch (e) {
          if (alive.current) fail(e);
          return;
        }
        if (!alive.current) return;
        if (status.state === "error") {
          fail(status.message);
        } else if (status.state === "done") {
          clearInterval(timer);
          clearInterval(dotTimer);
          await syncAccounts();
          toast("success", t("accounts.loggedIn", { name: status.account.username }));
          onClose();
        }
      }, 2000);
    })();

    return () => {
      alive.current = false;
      if (timer) clearInterval(timer);
      if (dotTimer) clearInterval(dotTimer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  return (
    <Modal open={open} onClose={onClose} title={t("accounts.deviceTitle")} width={420}>
      {prompt ? (
        <div className="device-flow">
          <div className="device-code mono">{prompt.userCode}</div>
          <p className="device-step">
            <a
              href={prompt.verificationUri}
              onClick={(e) => {
                e.preventDefault();
                void openExternal(prompt.verificationUri);
              }}
            >
              {prompt.verificationUri}
            </a>{" "}
            {t("accounts.deviceStep")}
          </p>
          <div className="device-waiting">
            <span className="spinner" />
            <span>
              {t("accounts.waiting")}
              {".".repeat(dots)}
            </span>
          </div>
          <Button
            variant="outline"
            icon="copy"
            size="sm"
            onClick={() => {
              void navigator.clipboard?.writeText(prompt.userCode);
              toast("info", t("common.copied"));
            }}
          >
            {t("common.copy")}
          </Button>
        </div>
      ) : (
        <div className="device-flow device-loading">
          <span className="spinner" />
        </div>
      )}
    </Modal>
  );
}

/* ── add-account chooser ── */

function AddAccountModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { importOfficial, syncAccounts, toast } = useStore();
  const { t } = useT();
  const [deviceOpen, setDeviceOpen] = useState(false);
  const [busyBrowser, setBusyBrowser] = useState(false);

  const [offlineName, setOfflineName] = useState("");
  const [busyOffline, setBusyOffline] = useState(false);

  const browserLogin = async () => {
    setBusyBrowser(true);
    try {
      const acc = await core.auth_login_authcode();
      await syncAccounts();
      toast("success", t("accounts.loggedIn", { name: acc.username }));
      onClose();
    } catch (e) {
      toast("error", errText(e));
    } finally {
      setBusyBrowser(false);
    }
  };

  const addOffline = async () => {
    if (!offlineName.trim()) return;
    setBusyOffline(true);
    try {
      const acc = await addOfflineAccount(offlineName.trim());
      await syncAccounts();
      toast("success", t("accounts.loggedIn", { name: acc.username }));
      setOfflineName("");
      onClose();
    } catch (e) {
      toast("error", errText(e));
    } finally {
      setBusyOffline(false);
    }
  };

  return (
    <>
      <Modal open={open && !deviceOpen} onClose={onClose} title={t("accounts.add")} width={440}>
        <div className="add-options">
          <button className="add-option card-hover" onClick={() => setDeviceOpen(true)}>
            <span className="add-option-icon"><Icon name="zap" size={18} /></span>
            <span className="add-option-meta">
              <strong>{t("accounts.viaDevice")}</strong>
              <span>{t("accounts.viaDeviceDesc")}</span>
            </span>
            <Icon name="chevronRight" size={16} />
          </button>
          <button className="add-option card-hover" onClick={() => void browserLogin()}>
            <span className="add-option-icon"><Icon name="globe" size={18} /></span>
            <span className="add-option-meta">
              <strong>{t("accounts.viaBrowser")}</strong>
              <span>{t("accounts.viaBrowserDesc")}</span>
            </span>
            {busyBrowser ? <span className="spinner" /> : <Icon name="chevronRight" size={16} />}
          </button>
          <button
            className="add-option card-hover"
            onClick={() => {
              void importOfficial();
              onClose();
            }}
          >
            <span className="add-option-icon"><Icon name="download" size={18} /></span>
            <span className="add-option-meta">
              <strong>{t("accounts.importOfficial")}</strong>
              <span>{t("accounts.importDesc")}</span>
            </span>
            <Icon name="chevronRight" size={16} />
          </button>
        </div>

        {/* offline is a real mode, not a placeholder: singleplayer and LAN
            work, online servers do not, and the card says so */}
        <div className="add-offline">
          <label className="field-label">{t("accounts.offline")}</label>
          <div className="add-offline-row">
            <input
              className="input"
              value={offlineName}
              onChange={(e) => setOfflineName(e.target.value)}
              placeholder={t("accounts.offlinePlaceholder")}
              maxLength={16}
              onKeyDown={(e) => e.key === "Enter" && void addOffline()}
            />
            <Button
              variant="outline"
              icon="plus"
              loading={busyOffline}
              disabled={!offlineName.trim()}
              onClick={() => void addOffline()}
            >
              {t("accounts.add")}
            </Button>
          </div>
          <p className="set-hint">{t("accounts.offlineDesc")}</p>
        </div>
      </Modal>
      <DeviceCodeModal open={deviceOpen} onClose={() => { setDeviceOpen(false); onClose(); }} />
    </>
  );
}

/* ── account card ── */

function AccountCard({ account }: { account: Account }) {
  const { activeAccount, selectAccount, removeAccount, refreshAccount } = useStore();
  const { t } = useT();
  const [confirmDel, setConfirmDel] = useState(false);
  const isActive = activeAccount?.uuid === account.uuid;

  return (
    <article className={`acc-card card ${isActive ? "acc-active" : ""}`}>
      {isActive && <span className="acc-ribbon">{t("accounts.current")}</span>}
      <div className="acc-top">
        <div className="acc-face" style={{ "--h": account.skinHue }}>
          <SkinFace hue={account.skinHue} size={64} />
        </div>
        <div className="acc-meta">
          <strong>{account.username}</strong>
          <span className="acc-uuid mono">{account.uuid.slice(0, 13)}…</span>
          <Badge tone={account.source === "microsoft" ? "accent" : account.source === "imported" ? "ok" : "dim"}>
            {t(`accounts.source.${account.source}`)}
          </Badge>
        </div>
      </div>
      <div className="acc-actions">
        {!isActive && (
          <Button size="sm" variant="soft" onClick={() => void selectAccount(account.uuid)}>
            {t("accounts.switch")}
          </Button>
        )}
        <span className="inst-actions-spacer" />
        <IconButton icon="refresh" label={t("accounts.refresh")} onClick={() => void refreshAccount(account.uuid)} />
        <IconButton icon="trash" label={t("common.delete")} danger onClick={() => setConfirmDel(true)} />
      </div>
      <ConfirmModal
        open={confirmDel}
        onClose={() => setConfirmDel(false)}
        onConfirm={() => void removeAccount(account.uuid)}
        title={t("accounts.removeTitle")}
        body={t("accounts.removeConfirm")}
        confirmLabel={t("common.delete")}
      />
    </article>
  );
}

export function AccountsScreen() {
  const { accounts } = useStore();
  const { t } = useT();
  const [addOpen, setAddOpen] = useState(false);

  return (
    <div className="screen-pad">
      <div className="page-actions">
        <Button variant="primary" icon="plus" onClick={() => setAddOpen(true)}>
          {t("accounts.add")}
        </Button>
      </div>

      <div className="acc-grid stagger">
        {accounts.map((a) => (
          <AccountCard key={a.uuid} account={a} />
        ))}
        <button className="inst-new card" onClick={() => setAddOpen(true)}>
          <span className="inst-new-icon">
            <Icon name="plus" size={22} />
          </span>
          <span>{t("accounts.add")}</span>
        </button>
      </div>

      <AddAccountModal open={addOpen} onClose={() => setAddOpen(false)} />
    </div>
  );
}
