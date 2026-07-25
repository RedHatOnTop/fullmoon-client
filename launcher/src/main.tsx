import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { StoreProvider } from "./state/store";
import { I18nProvider, type Language } from "./i18n";
import BRAND from "./brand";

import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/ui.css";
import "./styles/shell.css";
import "./styles/screens.css";

/* brand.json → CSS vars before first paint (PLAN §0/§6);
   the store effect re-derives per-theme values after mount */
const root = document.documentElement;
root.style.setProperty("--accent", BRAND.accent);
root.style.setProperty("--accent-fill", BRAND.accent);
root.style.setProperty("--accent-hover", BRAND.accentDim);

/* peek the persisted language so there's no flash of the wrong locale */
function initialLang(): Language {
  try {
    const raw = localStorage.getItem("pinion.v1.state");
    if (raw) {
      const lang = (JSON.parse(raw) as { settings?: { language?: Language } }).settings?.language;
      if (lang === "ko" || lang === "en") return lang;
    }
  } catch {
    /* first run */
  }
  return "ko";
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <I18nProvider initial={initialLang()}>
      <StoreProvider>
        <App />
      </StoreProvider>
    </I18nProvider>
  </React.StrictMode>,
);
