import { create } from "zustand";
import type { ChatSession, ChatMessage } from "@/lib/types";
import * as api from "@/lib/api";

// Persistence keys
const MESSAGES_KEY = "insighttube_messages";
const SESSION_KEY = "insighttube_active_session";

function loadMessages(): Record<string, ChatMessage[]> {
  if (typeof window === "undefined") return {};
  try {
    const raw = localStorage.getItem(MESSAGES_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function saveMessages(messages: Record<string, ChatMessage[]>) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(MESSAGES_KEY, JSON.stringify(messages));
  } catch {
    // Storage full — silently fail
  }
}

function loadActiveSession(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(SESSION_KEY);
}

function saveActiveSession(id: string | null) {
  if (typeof window === "undefined") return;
  if (id) localStorage.setItem(SESSION_KEY, id);
  else localStorage.removeItem(SESSION_KEY);
}

interface ChatStore {
  // Sessions
  sessions: ChatSession[];
  activeSessionId: string | null;
  sessionsLoading: boolean;

  // Messages (per session, persisted to localStorage)
  messages: Record<string, ChatMessage[]>;
  sendingMessage: boolean;

  // Actions
  loadSessions: () => Promise<void>;
  setActiveSession: (sessionId: string | null) => void;
  sendMessage: (message: string) => Promise<void>;
  addSession: (session: ChatSession) => void;
  removeSession: (sessionId: string) => void;
  renameSession: (sessionId: string, newTitle: string) => void;
  getActiveMessages: () => ChatMessage[];
}

export const useChatStore = create<ChatStore>((set, get) => ({
  sessions: [],
  activeSessionId: loadActiveSession(),
  sessionsLoading: false,
  messages: loadMessages(),
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
    saveActiveSession(sessionId);
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
    const newMessages = {
      ...messages,
      [activeSessionId]: [...sessionMessages, userMessage],
    };
    set({ messages: newMessages, sendingMessage: true });
    saveMessages(newMessages);

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
      const updatedMessages = {
        ...get().messages,
        [activeSessionId]: [...currentMessages, assistantMessage],
      };
      set({ messages: updatedMessages, sendingMessage: false });
      saveMessages(updatedMessages);
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
      const { messages } = get();
      const newMessages = { ...messages };
      delete newMessages[sessionId];
      saveMessages(newMessages);
      set((state) => ({
        sessions: state.sessions.filter((s) => s.id !== sessionId),
        activeSessionId:
          state.activeSessionId === sessionId ? null : state.activeSessionId,
        messages: newMessages,
      }));
      if (get().activeSessionId === null) saveActiveSession(null);
    } catch (e) {
      console.error("Failed to delete session:", e);
    }
  },

  renameSession: async (sessionId, newTitle) => {
    try {
      await api.renameSession(sessionId, newTitle);
      set((state) => ({
        sessions: state.sessions.map((s) =>
          s.id === sessionId ? { ...s, title: newTitle } : s
        ),
      }));
    } catch (e) {
      console.error("Failed to rename session:", e);
    }
  },

  getActiveMessages: () => {
    const { activeSessionId, messages } = get();
    if (!activeSessionId) return [];
    return messages[activeSessionId] || [];
  },
}));
