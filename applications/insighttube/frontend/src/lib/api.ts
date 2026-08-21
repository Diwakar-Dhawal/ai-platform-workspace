// API client for backend communication

import type {
  IngestProgress,
  IngestResponse,
  ChatResponse,
  ChatSession,
} from "./types";

// All paths use /tube-service prefix which maps to context-path on Tube Service
const TUBE_BASE = "/tube-service";

async function apiFetch<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const url = `${TUBE_BASE}${path}`;

  const token =
    typeof window !== "undefined" ? localStorage.getItem("access_token") : null;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options?.headers as Record<string, string> || {}),
  };

  console.log(`[API] ${options?.method || "GET"} ${url}`);

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = await response.text();
    console.error(`[API] Error ${response.status}: ${error}`);
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
    "/api/v1/content/ingest",
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
    `/api/v1/content/sessions/${sessionId}/status`
  );
}

// ─── SSE Progress Stream ───

export function createProgressStream(
  sessionId: string,
  onProgress: (progress: IngestProgress) => void,
  onDone: (progress: IngestProgress) => void,
  onError: (error: string) => void
): EventSource {
  const url = `${TUBE_BASE}/api/v1/content/sessions/${sessionId}/stream`;

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
    "/api/v1/content/chat",
    {
      method: "POST",
      body: JSON.stringify({ message, contentSessionId }),
    }
  );
}

// ─── Sessions ───

export async function listSessions(): Promise<ChatSession[]> {
  return apiFetch<ChatSession[]>(
    "/api/v1/content/sessions"
  );
}

export async function getSession(
  sessionId: string
): Promise<ChatSession> {
  return apiFetch<ChatSession>(
    `/api/v1/content/sessions/${sessionId}`
  );
}

export async function deleteSession(
  sessionId: string
): Promise<void> {
  await apiFetch(`/api/v1/content/sessions/${sessionId}`, {
    method: "DELETE",
  });
}
