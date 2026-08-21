// TypeScript types matching backend DTOs

export interface ChatSession {
  id: string;
  contentSessionId?: string;
  title: string;
  sourceUrl: string;
  contentType: string;
  chunkCount: number;
  videoCount: number;
  status: "pending" | "processing" | "completed" | "failed";
  userId: string;
}

export interface IngestProgress {
  contentSessionId: string;
  status:
    | "PENDING"
    | "EXTRACTING_TRANSCRIPT"
    | "CHUNKING"
    | "EMBEDDING"
    | "STORING"
    | "COMPLETED"
    | "FAILED";
  currentStep: string;
  percentComplete: number;
  totalChunks: number;
  chunksEmbedded: number;
  chunksStored: number;
  videoCount: number;
  segmentCount: number;
  title: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
  estimatedTimeRemainingSeconds: number | null;
}

export interface IngestResponse {
  status: string;
  contentSessionId: string;
  title: string;
  sourceUrl: string;
  chunkCount: number;
  message: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: string;
  sources?: TimestampSource[];
}

export interface TimestampSource {
  timestamp: string;
  timestampSeconds: number;
  videoTitle: string;
  channelName: string;
}

export interface ChatRequest {
  message: string;
  contentSessionId?: string;
}

export interface ChatResponse {
  answer: string;
  model: string;
  sources?: TimestampSource[];
}
