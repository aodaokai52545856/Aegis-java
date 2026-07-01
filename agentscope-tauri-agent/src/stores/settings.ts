import { defineStore } from "pinia";
import { ref } from "vue";

const SETTINGS_KEY = "aegis.settings";

export interface AppSettings {
  serviceBaseUrl: string;
}

function loadSettings(): AppSettings {
  const raw = localStorage.getItem(SETTINGS_KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as Partial<AppSettings>;
      return {
        serviceBaseUrl: parsed.serviceBaseUrl ?? "http://127.0.0.1:8010",
      };
    } catch {
      /* fall through */
    }
  }
  return { serviceBaseUrl: "http://127.0.0.1:8010" };
}

export const useSettingsStore = defineStore("settings", () => {
  const settings = ref<AppSettings>(loadSettings());

  function save(next: Partial<AppSettings>) {
    settings.value = { ...settings.value, ...next };
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings.value));
  }

  return { settings, save };
});
