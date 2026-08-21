// API client for backend communication

import type {
  IngestProgress,
  IngestResponse,
  ChatResponse,
  ChatSession,
} from "./types";

const API_BASE = "";  // Use Next.js proxy (rewrites in next.config.ts)

async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const url = `${API_BASE}${path}`;

  // Get token from localStorage (or wherever auth stores it)
  const token =
    typeof window !== "undefined" ? localStorage.getItem("access_token") : null;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options?.headers as Record<string, string> || {}),
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`API error ${response.status}: ${error}`);
  }

  return response.json();
}

// ─── Ingest ───

export async function startIngest(
  url: string,
  languages: string[] = ["en"]
): Promise<IngestResponse> {
  return apiFetch<IngestResponse>(
    "/tube-service/api/v1/content/ingest",
    {
      method: "POST",
      body: JSON.stringify({ url, languages }),
    }
  );
}

export async function getIngestProgress(
  sessionId: string
): Promise<IngestProgress> {
  return apiFetch<IngestProgress>(
    `/tube-service/api/v1/content/sessions/${sessionId}/status`
  );
}

// ─── SSE Progress Stream ───

export function createProgressStream(
  sessionId: string,
  onProgress: (progress: IngestProgress) => void,
  onDone: (progress: IngestProgress) => void,
  onError: (error: string) => void
): EventSource {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("access_token") : null;

  // Note: EventSource doesn't support custom headers
  // For SSE with auth, we'd need to use query param or a different approach
  const url = `${API_BASE}/tube-service/api/v1/content/sessions/${sessionId}/stream`;

  const eventSource = new EventSource(url);

  eventSource.addEventListener("progress", (event) => {
    try {
      const progress = JSON.parse(event.data) as IngestProgress;
      onProgress(progress);
    } catch (e) {
      console.error("Failed to parse progress event:", e);
    }
  });

  eventSource.addEventListener("done", (event) => {
    try {
      const progress = JSON.parse(event.data) as IngestProgress;
      onDone(progress);
      eventSource.close();
    } catch (e) {
      console.error("Failed to parse done event:", e);
    }
  });

  eventSource.addEventListener("error", (event) => {
    if (event instanceof MessageEvent) {
      onError(event.data);
    } else {
      onError("SSE connection lost");
    }
    eventSource.close();
  });

  eventSource.onerror = () => {
    onError("SSE connection error");
    eventSource.close();
  };

  return eventSource;
}

// ─── Chat ───

export async function sendChatMessage(
  message: string,
  contentSessionId?: string
): Promise<ChatResponse> {
  return apiFetch<ChatResponse>(
    "/tube-service/api/v1/content/chat",
    {
      method: "POST",
      body: JSON.stringify({ message, contentSessionId }),
    }
  );
}

// ─── Sessions ───

export async function listSessions(): Promise<ChatSession[]> {
  return apiFetch<ChatSession[]>(
    "/tube-service/api/v1/content/sessions"
  );
}

export async function getSession(
  sessionId: string
): Promise<ChatSession> {
  return apiFetch<ChatSession>(
    `/tube-service/api/v1/content/sessions/${sessionId}`
  );
}

export async function deleteSession(
  sessionId: string
): Promise<void> {
  await apiFetch(`/tube-service/api/v1/content/sessions/${sessionId}`, {
    method: "DELETE",
  });
}
