"use client";

import { Video, CheckCircle, XCircle, Clock, FileText, Cpu, Database, Download } from "lucide-react";
import { useIngestStore } from "@/stores/ingestStore";

const STEP_ICONS: Record<string, React.ReactNode> = {
  PENDING: <Clock className="h-3.5 w-3.5 text-slate-400 animate-pulse" />,
  EXTRACTING_TRANSCRIPT: <Download className="h-3.5 w-3.5 text-amber-400 animate-pulse" />,
  CHUNKING: <FileText className="h-3.5 w-3.5 text-blue-400 animate-pulse" />,
  EMBEDDING: <Cpu className="h-3.5 w-3.5 text-purple-400 animate-pulse" />,
  STORING: <Database className="h-3.5 w-3.5 text-emerald-400 animate-pulse" />,
  COMPLETED: <CheckCircle className="h-3.5 w-3.5 text-emerald-400" />,
  FAILED: <XCircle className="h-3.5 w-3.5 text-red-400" />,
};

const STEP_LABELS: Record<string, string> = {
  PENDING: "Queued",
  EXTRACTING_TRANSCRIPT: "Extracting transcript",
  CHUNKING: "Splitting into segments",
  EMBEDDING: "Generating embeddings",
  STORING: "Storing vectors",
  COMPLETED: "Complete",
  FAILED: "Failed",
};

export function IngestBar() {
  const progress = useIngestStore((s) => s.progress);
  const error = useIngestStore((s) => s.error);

  if (!progress && !error) return null;

  const percent = progress?.percentComplete || 0;
  const isComplete = progress?.status === "COMPLETED";
  const isFailed = progress?.status === "FAILED" || !!error;
  const currentStep = progress?.status || "PENDING";

  return (
    <div className="border-t border-white/5 bg-slate-900/80 backdrop-blur-sm px-4 py-3">
      <div className="mx-auto max-w-3xl">
        <div className="flex items-center gap-3">
          {/* Step Icon */}
          {STEP_ICONS[currentStep] || <Video className="h-3.5 w-3.5 text-rose-400 animate-pulse" />}

          {/* Status + Progress */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-xs text-slate-300 truncate font-medium">
                {isFailed
                  ? error || "Ingestion failed"
                  : STEP_LABELS[currentStep] || progress?.currentStep || "Processing..."}
              </span>
              <div className="flex items-center gap-3 shrink-0 ml-2">
                {progress?.estimatedTimeRemainingSeconds != null &&
                  !isComplete &&
                  !isFailed && (
                    <span className="flex items-center gap-1 text-[10px] text-slate-500">
                      <Clock className="h-3 w-3" />
                      ~{formatTime(progress.estimatedTimeRemainingSeconds)}
                    </span>
                  )}
                <span className="text-xs font-mono text-slate-400 tabular-nums">
                  {percent}%
                </span>
              </div>
            </div>

            {/* Progress bar */}
            <div className="h-1.5 w-full rounded-full bg-white/5 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-700 ease-out ${
                  isComplete
                    ? "bg-emerald-500"
                    : isFailed
                    ? "bg-red-500"
                    : "bg-gradient-to-r from-rose-500 via-orange-500 to-amber-500"
                }`}
                style={{ width: `${percent}%` }}
              />
            </div>

            {/* Step indicators */}
            {!isComplete && !isFailed && (
              <div className="flex items-center gap-1 mt-2">
                {["EXTRACTING_TRANSCRIPT", "CHUNKING", "EMBEDDING", "STORING"].map((step, i) => {
                  const stepOrder = ["PENDING", "EXTRACTING_TRANSCRIPT", "CHUNKING", "EMBEDDING", "STORING", "COMPLETED"];
                  const currentIdx = stepOrder.indexOf(currentStep);
                  const stepIdx = stepOrder.indexOf(step);
                  const isDone = stepIdx < currentIdx;
                  const isActive = stepIdx === currentIdx;

                  return (
                    <div key={step} className="flex items-center gap-1">
                      <div
                        className={`w-1.5 h-1.5 rounded-full transition-colors duration-300 ${
                          isDone
                            ? "bg-emerald-400"
                            : isActive
                            ? "bg-orange-400 animate-pulse"
                            : "bg-slate-600"
                        }`}
                      />
                      {i < 3 && (
                        <div
                          className={`w-4 h-px transition-colors duration-300 ${
                            isDone ? "bg-emerald-400/40" : "bg-slate-700"
                          }`}
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* Detail stats */}
            {progress && progress.totalChunks > 0 && (
              <div className="flex items-center gap-4 mt-2 text-[10px] text-slate-500">
                <span>
                  Videos: {progress.videoCount}
                </span>
                <span>
                  Embedded: {progress.chunksEmbedded}/{progress.totalChunks}
                </span>
                <span>
                  Stored: {progress.chunksStored}/{progress.totalChunks}
                </span>
                {progress.title && (
                  <span className="truncate">
                    Title: {progress.title}
                  </span>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function formatTime(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}m ${secs}s`;
}
