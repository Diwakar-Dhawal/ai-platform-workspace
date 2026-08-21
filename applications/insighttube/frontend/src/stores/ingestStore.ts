import { create } from "zustand";
import type { IngestProgress } from "@/lib/types";
import * as api from "@/lib/api";

interface IngestState {
  // Current ingest
  isIngesting: boolean;
  currentSessionId: string | null;
  progress: IngestProgress | null;
  ingestUrl: string;
  error: string | null;

  // Actions
  setIngestUrl: (url: string) => void;
  startIngest: (url: string) => Promise<void>;
  stopIngest: () => void;
  reset: () => void;
}

export const useIngestStore = create<IngestState>((set, get) => ({
  isIngesting: false,
  currentSessionId: null,
  progress: null,
  ingestUrl: "",
  error: null,

  setIngestUrl: (url) => set({ ingestUrl: url }),

  startIngest: async (url: string) => {
    set({ isIngesting: true, error: null, ingestUrl: url });

    try {
      const response = await api.startIngest(url);
      const sessionId = response.contentSessionId;

      set({ currentSessionId: sessionId });

      // Start polling for progress
      pollProgress(sessionId, set, get);
    } catch (e) {
      set({
        isIngesting: false,
        error: e instanceof Error ? e.message : "Ingest failed",
      });
    }
  },

  stopIngest: () => {
    set({
      isIngesting: false,
      currentSessionId: null,
      progress: null,
    });
  },

  reset: () => {
    set({
      isIngesting: false,
      currentSessionId: null,
      progress: null,
      ingestUrl: "",
      error: null,
    });
  },
}));

// Poll progress every 2 seconds
function pollProgress(
  sessionId: string,
  set: (partial: Partial<IngestState>) => void,
  get: () => IngestState
) {
  const interval = setInterval(async () => {
    try {
      const progress = await api.getIngestProgress(sessionId);
      set({ progress });

      // Stop polling when done
      if (progress.status === "COMPLETED" || progress.status === "FAILED") {
        clearInterval(interval);
        set({ isIngesting: false });

        // Always dispatch so sidebar refreshes (completed OR failed)
        window.dispatchEvent(new CustomEvent("ingest-completed", {
          detail: { sessionId },
        }));
      }
    } catch (e) {
      console.error("Failed to poll progress:", e);
      // Don't stop polling on 404 — progress may not be initialized yet
      if (e instanceof Error && e.message.includes("404")) {
        console.log("Progress not yet available, retrying...");
        return;
      }
      clearInterval(interval);
      set({
        isIngesting: false,
        error: "Lost connection to server",
      });
    }
  }, 2000);
}
