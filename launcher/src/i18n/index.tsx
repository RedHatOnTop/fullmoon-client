import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import ko, { type Dict } from "./ko";
import en from "./en";

export type Language = "ko" | "en";
const DICTS: Record<Language, Dict> = { ko, en };

function resolve(dict: unknown, path: string): string {
  let node = dict as Record<string, unknown>;
  for (const key of path.split(".")) {
    const next = node?.[key];
    if (next === undefined) return path;
    node = next as Record<string, unknown>;
  }
  return node as unknown as string;
}

interface I18n {
  lang: Language;
  setLang: (l: Language) => void;
  t: (path: string, vars?: Record<string, string | number>) => string;
}

const Ctx = createContext<I18n>({
  lang: "ko",
  setLang: () => {},
  t: (p) => p,
});

export function I18nProvider({ children, initial }: { children: ReactNode; initial: Language }) {
  const [lang, setLang] = useState<Language>(initial);

  const t = useCallback(
    (path: string, vars?: Record<string, string | number>) => {
      let out = resolve(DICTS[lang], path);
      if (vars) {
        for (const [k, v] of Object.entries(vars)) {
          out = out.replaceAll(`{${k}}`, String(v));
        }
      }
      return out;
    },
    [lang],
  );

  const value = useMemo(() => ({ lang, setLang, t }), [lang, t]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useT(): I18n {
  return useContext(Ctx);
}
