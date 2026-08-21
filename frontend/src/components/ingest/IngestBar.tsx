"use client";

import { Video, CheckCircle, XCircle, Clock } from "lucide-react";
import { useIngestStore } from "@/stores/ingestStore";

export function IngestBar() {
  const progress = useIngestStore((s) => s.progress);
  const error = useIngestStore((s) => s.error);

  if (!progress && !error) return null;

  const percent = progress?.percentComplete || 0;
  const isComplete = progress?.status === "COMPLETED";
  const isFailed = progress?.status === "FAILED" || !!error;

  return (
    <div className="border-t border-white/10 bg-[#171717] px-4 py-3">
      <div className="mx-auto max-w-3xl">
        <div className="flex items-center gap-3">
          {/* Icon */}
          {isComplete ? (
            <CheckCircle className="h-4 w-4 text-green-500 shrink-0" />
          ) : isFailed ? (
            <XCircle className="h-4 w-4 text-red-500 shrink-0" />
          ) : (
            <Video className="h-4 w-4 text-red-500 shrink-0 animate-pulse" />
          )}

          {/* Status text */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs text-white/70 truncate">
                {isFailed
                  ? error || "Ingestion failed"
                  : progress?.currentStep || "Processing..."}
              </span>
              <div className="flex items-center gap-3 shrink-0 ml-2">
                {progress?.estimatedTimeRemainingSeconds != null &&
                  !isComplete &&
                  !isFailed && (
                    <span className="flex items-center gap-1 text-[10px] text-white/40">
                      <Clock className="h-3 w-3" />
                      ~{formatTime(progress.estimatedTimeRemainingSeconds)}
                    </span>
                  )}
                <span className="text-xs font-mono text-white/50">
                  {percent}%
                </span>
              </div>
            </div>

            {/* Custom progress bar (base-ui doesn't support indicatorClassName) */}
            <div className="h-1.5 w-full rounded-full bg-white/10 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ease-out ${
                  isComplete
                    ? "bg-green-500"
                    : isFailed
                    ? "bg-red-500"
                    : "bg-gradient-to-r from-blue-500 to-cyan-400"
                }`}
                style={{ width: `${percent}%` }}
              />
            </div>

            {/* Detail stats */}
            {progress && progress.totalChunks > 0 && (
              <div className="flex items-center gap-4 mt-1.5 text-[10px] text-white/30">
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
