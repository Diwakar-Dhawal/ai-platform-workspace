import { create } from "zustand";
import type { ChatSession, ChatMessage } from "@/lib/types";
import * as api from "@/lib/api";

interface ChatStore {
  // Sessions
  sessions: ChatSession[];
  activeSessionId: string | null;
  sessionsLoading: boolean;

  // Messages (per session)
  messages: Record<string, ChatMessage[]>;
  sendingMessage: boolean;

  // Actions
  loadSessions: () => Promise<void>;
  setActiveSession: (sessionId: string | null) => void;
  sendMessage: (message: string) => Promise<void>;
  addSession: (session: ChatSession) => void;
  removeSession: (sessionId: string) => void;
  getActiveMessages: () => ChatMessage[];
}

export const useChatStore = create<ChatStore>((set, get) => ({
  sessions: [],
  activeSessionId: null,
  sessionsLoading: false,
  messages: {},
  sendingMessage: false,

  loadSessions: async () => {
    set({ sessionsLoading: true });
    try {
      const sessions = await api.listSessions();
      set({ sessions, sessionsLoading: false });
    } catch (e) {
      console.error("Failed to load sessions:", e);
      set({ sessionsLoading: false });
    }
  },

  setActiveSession: (sessionId) => {
    set({ activeSessionId: sessionId });
  },

  sendMessage: async (content: string) => {
    const { activeSessionId, messages } = get();
    if (!activeSessionId) return;

    // Add user message
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: "user",
      content,
      timestamp: new Date().toISOString(),
    };

    const sessionMessages = messages[activeSessionId] || [];
    set({
      messages: {
        ...messages,
        [activeSessionId]: [...sessionMessages, userMessage],
      },
      sendingMessage: true,
    });

    try {
      const response = await api.sendChatMessage(content, activeSessionId);

      const assistantMessage: ChatMessage = {
        id: crypto.randomUUID(),
        role: "assistant",
        content: response.answer,
        timestamp: new Date().toISOString(),
        sources: response.sources,
      };

      const currentMessages = get().messages[activeSessionId] || [];
      set({
        messages: {
          ...get().messages,
          [activeSessionId]: [...currentMessages, assistantMessage],
        },
        sendingMessage: false,
      });
    } catch (e) {
      console.error("Failed to send message:", e);
      set({ sendingMessage: false });
    }
  },

  addSession: (session) => {
    set((state) => ({
      sessions: [session, ...state.sessions],
    }));
  },

  removeSession: async (sessionId) => {
    try {
      await api.deleteSession(sessionId);
      set((state) => ({
        sessions: state.sessions.filter((s) => s.id !== sessionId),
        activeSessionId:
          state.activeSessionId === sessionId ? null : state.activeSessionId,
      }));
    } catch (e) {
      console.error("Failed to delete session:", e);
    }
  },

  getActiveMessages: () => {
    const { activeSessionId, messages } = get();
    if (!activeSessionId) return [];
    return messages[activeSessionId] || [];
  },
}));
