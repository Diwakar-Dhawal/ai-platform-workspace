"use client";

import { useState, useEffect } from "react";
import { Video, ArrowRight, Loader2, CheckCircle, XCircle, Clock, Download, FileText, Cpu, Database } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useIngestStore } from "@/stores/ingestStore";

interface IngestModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const YOUTUBE_REGEX =
  /^(https?:\/\/)?(www\.)?(youtube\.com\/(watch\?v=|playlist\?list=|channel\/)|youtu\.be\/|youtube\.com\/@)/;

const STEP_ICONS: Record<string, React.ReactNode> = {
  PENDING: <Clock className="h-4 w-4 text-slate-400" />,
  EXTRACTING_TRANSCRIPT: <Download className="h-4 w-4 text-amber-400" />,
  CHUNKING: <FileText className="h-4 w-4 text-blue-400" />,
  EMBEDDING: <Cpu className="h-4 w-4 text-purple-400" />,
  STORING: <Database className="h-4 w-4 text-emerald-400" />,
  COMPLETED: <CheckCircle className="h-4 w-4 text-emerald-400" />,
  FAILED: <XCircle className="h-4 w-4 text-red-400" />,
};

const STEP_LABELS: Record<string, string> = {
  PENDING: "Queued",
  EXTRACTING_TRANSCRIPT: "Extracting transcript from video",
  CHUNKING: "Splitting transcript into segments",
  EMBEDDING: "Generating AI embeddings",
  STORING: "Storing vectors in database",
  COMPLETED: "Complete",
  FAILED: "Failed",
};

export function IngestModal({ open, onOpenChange }: IngestModalProps) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const startIngest = useIngestStore((s) => s.startIngest);
  const isIngesting = useIngestStore((s) => s.isIngesting);
  const progress = useIngestStore((s) => s.progress);
  const ingestError = useIngestStore((s) => s.error);
  const reset = useIngestStore((s) => s.reset);

  const isComplete = progress?.status === "COMPLETED";
  const isFailed = progress?.status === "FAILED" || !!ingestError;
  const isActive = isIngesting || (progress && !isComplete && !isFailed);

  // Auto-close after completion (2s delay)
  useEffect(() => {
    if (isComplete) {
      const timer = setTimeout(() => {
        onOpenChange(false);
        setTimeout(() => reset(), 300);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [isComplete, onOpenChange, reset]);

  // Reset on close
  const handleClose = (nextOpen: boolean) => {
    if (!nextOpen && !isIngesting) {
      reset();
      setUrl("");
      setError("");
    }
    onOpenChange(nextOpen);
  };

  const handleSubmit = async () => {
    if (!url.trim()) {
      setError("Please enter a YouTube URL");
      return;
    }

    if (!YOUTUBE_REGEX.test(url)) {
      setError("Please enter a valid YouTube URL (video, playlist, or channel)");
      return;
    }

    setError("");
    await startIngest(url.trim());
  };

  const percent = progress?.percentComplete || 0;
  const currentStep = progress?.status || "PENDING";

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="bg-slate-900 border-white/10 text-white max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Video className="h-5 w-5 text-rose-500" />
            {isActive
              ? "Ingesting Content..."
              : isComplete
              ? "Ingestion Complete!"
              : isFailed
              ? "Ingestion Failed"
              : "Add YouTube Content"}
          </DialogTitle>
          <DialogDescription className="text-slate-400">
            {isActive
              ? "Processing your content. This may take a minute or two."
              : isComplete
              ? "Your content is ready to chat!"
              : isFailed
              ? "Something went wrong. Please try again."
              : "Paste a video, playlist, or channel URL to start chatting with the content."}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 mt-4">
          {/* URL input (only show when not actively ingesting) */}
          {!isActive && !isComplete && !isFailed && (
            <>
              <div>
                <Input
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    setError("");
                  }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !isIngesting) {
                      handleSubmit();
                    }
                  }}
                  placeholder="https://www.youtube.com/watch?v=..."
                  className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                  autoFocus
                />
                {error && (
                  <p className="mt-1.5 text-xs text-red-400">{error}</p>
                )}
              </div>

              <div className="text-xs text-slate-500 space-y-1">
                <p>Supported:</p>
                <ul className="list-disc list-inside space-y-0.5">
                  <li>Single video: youtube.com/watch?v=...</li>
                  <li>Playlist: youtube.com/playlist?list=...</li>
                  <li>Channel: youtube.com/@channel or /channel/...</li>
                </ul>
              </div>

              <Button
                onClick={handleSubmit}
                disabled={!url.trim() || isIngesting}
                className="w-full bg-gradient-to-r from-rose-600 to-orange-600 hover:from-rose-500 hover:to-orange-500 text-white shadow-lg shadow-rose-500/20"
              >
                Start Ingesting
                <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </>
          )}

          {/* Progress view */}
          {isActive && (
            <div className="space-y-4">
              {/* Current step with icon */}
              <div className="flex items-center gap-3">
                <div className="shrink-0 animate-pulse">
                  {STEP_ICONS[currentStep] || <Loader2 className="h-4 w-4 text-rose-400 animate-spin" />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-white/80 truncate font-medium">
                    {STEP_LABELS[currentStep] || progress?.currentStep || "Starting..."}
                  </p>
                  {progress?.title && (
                    <p className="text-xs text-slate-500 truncate mt-0.5">
                      {progress.title}
                    </p>
                  )}
                </div>
              </div>

              {/* Step indicator dots */}
              <div className="flex items-center justify-center gap-1.5">
                {["EXTRACTING_TRANSCRIPT", "CHUNKING", "EMBEDDING", "STORING"].map((step, i) => {
                  const stepOrder = ["PENDING", "EXTRACTING_TRANSCRIPT", "CHUNKING", "EMBEDDING", "STORING", "COMPLETED"];
                  const currentIdx = stepOrder.indexOf(currentStep);
                  const stepIdx = stepOrder.indexOf(step);
                  const isDone = stepIdx < currentIdx;
                  const isActiveStep = stepIdx === currentIdx;

                  return (
                    <div key={step} className="flex items-center gap-1.5">
                      <div
                        className={`w-2 h-2 rounded-full transition-all duration-500 ${
                          isDone
                            ? "bg-emerald-400 scale-110"
                            : isActiveStep
                            ? "bg-orange-400 scale-125 shadow-lg shadow-orange-400/50"
                            : "bg-slate-600"
                        }`}
                      />
                      {i < 3 && (
                        <div
                          className={`w-6 h-px transition-colors duration-500 ${
                            isDone ? "bg-emerald-400/50" : "bg-slate-700"
                          }`}
                        />
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Progress bar */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs text-slate-500">
                    {progress?.chunksEmbedded || 0}/{progress?.totalChunks || 0} chunks
                  </span>
                  <div className="flex items-center gap-2">
                    {progress?.estimatedTimeRemainingSeconds != null && (
                      <span className="flex items-center gap-1 text-[10px] text-slate-500">
                        <Clock className="h-3 w-3" />
                        ~{formatTime(progress.estimatedTimeRemainingSeconds)}
                      </span>
                    )}
                    <span className="text-xs font-mono text-slate-400 tabular-nums">{percent}%</span>
                  </div>
                </div>
                <div className="h-2 w-full rounded-full bg-white/5 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-rose-500 via-orange-500 to-amber-500 transition-all duration-700 ease-out"
                    style={{ width: `${percent}%` }}
                  />
                </div>
              </div>

              {/* Stats */}
              {progress && progress.totalChunks > 0 && (
                <div className="flex items-center gap-4 text-[10px] text-slate-500">
                  <span>Videos: {progress.videoCount}</span>
                  <span>Embedded: {progress.chunksEmbedded}/{progress.totalChunks}</span>
                  <span>Stored: {progress.chunksStored}/{progress.totalChunks}</span>
                </div>
              )}
            </div>
          )}

          {/* Success view */}
          {isComplete && (
            <div className="flex flex-col items-center py-4 space-y-3">
              <div className="rounded-full bg-emerald-500/20 p-3 border border-emerald-500/10">
                <CheckCircle className="h-8 w-8 text-emerald-400" />
              </div>
              <p className="text-sm text-slate-400">
                Content is ready! You can now close this and start chatting.
              </p>
            </div>
          )}

          {/* Error view */}
          {isFailed && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 rounded-lg bg-red-500/10 border border-red-500/20 px-3 py-2.5">
                <XCircle className="h-4 w-4 text-red-400 shrink-0" />
                <p className="text-sm text-red-400">
                  {ingestError || "Ingestion failed"}
                </p>
              </div>
              <Button
                onClick={() => {
                  reset();
                  setUrl("");
                }}
                className="w-full bg-white/5 hover:bg-white/10 text-white border border-white/10"
                variant="ghost"
              >
                Try Again
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

function formatTime(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}m ${secs}s`;
}
