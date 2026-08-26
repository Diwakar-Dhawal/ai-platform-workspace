import { create } from "zustand";
import type { IngestProgress } from "@/lib/types";
import * as api from "@/lib/api";

// Module-level cleanup — avoids the chain-of-patched-resets bug.
// Each call to startIngest overwrites this, so only the latest resources
// are cleaned up. Previous resources are cleaned up first.
let activeCleanup: (() => void) | null = null;

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

      // Clean up any previous ingest resources (prevents leak on repeated calls)
      if (activeCleanup) {
        activeCleanup();
        activeCleanup = null;
      }

      // Use SSE stream for real-time progress updates
      let eventSource: EventSource | null = null;

      // Set up a polling fallback — SSE might not connect immediately after ingest starts
      // We poll aggressively at first, then less frequently once SSE is connected
      let fallbackInterval: ReturnType<typeof setInterval> | null = null;
      let sseConnected = false;

      const pollProgress = async () => {
        try {
          const progress = await api.getIngestProgress(sessionId);
          set({ progress });

          if (progress.status === "COMPLETED" || progress.status === "FAILED") {
            cleanup();
            set({ isIngesting: false });
            window.dispatchEvent(new CustomEvent("ingest-completed", {
              detail: { sessionId },
            }));
          }
        } catch {
          // Progress may not exist yet, retry
        }
      };

      const cleanup = () => {
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
        if (fallbackInterval) {
          clearInterval(fallbackInterval);
          fallbackInterval = null;
        }
        if (activeCleanup === cleanup) {
          activeCleanup = null;
        }
      };

      // Register as the active cleanup so reset() can call it
      activeCleanup = cleanup;

      // Start with fast polling (every 500ms) until SSE connects
      fallbackInterval = setInterval(pollProgress, 500);

      // Wait 1 second then try SSE — backend needs a moment to set up the stream
      setTimeout(() => {
        if (sseConnected || get().currentSessionId !== sessionId) return;

        try {
          eventSource = api.createProgressStream(
            sessionId,
            // onProgress — real-time update
            (progress) => {
              sseConnected = true;
              set({ progress });

              // Once SSE is connected, slow down the fallback polling
              if (fallbackInterval) {
                clearInterval(fallbackInterval);
                fallbackInterval = setInterval(pollProgress, 5000);
              }
            },
            // onDone
            (progress) => {
              set({ progress, isIngesting: false });
              cleanup();
              window.dispatchEvent(new CustomEvent("ingest-completed", {
                detail: { sessionId },
              }));
            },
            // onError
            (errorMsg) => {
              // If SSE fails, keep polling — it's our fallback
              console.warn("SSE stream error, falling back to polling:", errorMsg);
              if (eventSource) {
                eventSource.close();
                eventSource = null;
              }
            }
          );
        } catch {
          // SSE setup failed, keep polling
        }
      }, 1000);

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
    if (activeCleanup) {
      activeCleanup();
      activeCleanup = null;
    }
    set({
      isIngesting: false,
      currentSessionId: null,
      progress: null,
      ingestUrl: "",
      error: null,
    });
  },
}));
