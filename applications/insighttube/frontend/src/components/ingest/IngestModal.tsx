"use client";

import { useState, useEffect } from "react";
import { Video, ArrowRight, Loader2, CheckCircle, XCircle, Clock } from "lucide-react";
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
        // Reset after close
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
    // Don't close — stay open to show progress
  };

  const percent = progress?.percentComplete || 0;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="bg-[#1e1e1e] border-white/10 text-white max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Video className="h-5 w-5 text-red-500" />
            {isActive
              ? "Ingesting Content..."
              : isComplete
              ? "Ingestion Complete!"
              : isFailed
              ? "Ingestion Failed"
              : "Add YouTube Content"}
          </DialogTitle>
          <DialogDescription className="text-white/50">
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
                  className="bg-white/5 border-white/15 text-white placeholder:text-white/30"
                  autoFocus
                />
                {error && (
                  <p className="mt-1.5 text-xs text-red-400">{error}</p>
                )}
              </div>

              <div className="text-xs text-white/40 space-y-1">
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
                className="w-full bg-blue-600 hover:bg-blue-500 text-white"
              >
                Start Ingesting
                <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </>
          )}

          {/* Progress view */}
          {isActive && (
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <Loader2 className="h-5 w-5 text-blue-400 animate-spin shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-white/80 truncate">
                    {progress?.currentStep || "Starting..."}
                  </p>
                  {progress?.title && (
                    <p className="text-xs text-white/40 truncate mt-0.5">
                      {progress.title}
                    </p>
                  )}
                </div>
              </div>

              {/* Progress bar */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs text-white/50">
                    {progress?.chunksEmbedded || 0}/{progress?.totalChunks || 0} chunks
                  </span>
                  <div className="flex items-center gap-2">
                    {progress?.estimatedTimeRemainingSeconds != null && (
                      <span className="flex items-center gap-1 text-[10px] text-white/40">
                        <Clock className="h-3 w-3" />
                        ~{formatTime(progress.estimatedTimeRemainingSeconds)}
                      </span>
                    )}
                    <span className="text-xs font-mono text-white/50">{percent}%</span>
                  </div>
                </div>
                <div className="h-2 w-full rounded-full bg-white/10 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-blue-500 to-cyan-400 transition-all duration-500 ease-out"
                    style={{ width: `${percent}%` }}
                  />
                </div>
              </div>

              {/* Stats */}
              {progress && progress.totalChunks > 0 && (
                <div className="flex items-center gap-4 text-[10px] text-white/30">
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
              <CheckCircle className="h-12 w-12 text-green-500" />
              <p className="text-sm text-white/70">
                Content is ready! You can now close this and start chatting.
              </p>
            </div>
          )}

          {/* Error view */}
          {isFailed && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <XCircle className="h-5 w-5 text-red-400 shrink-0" />
                <p className="text-sm text-red-400">
                  {ingestError || "Ingestion failed"}
                </p>
              </div>
              <Button
                onClick={() => {
                  reset();
                  setUrl("");
                }}
                className="w-full bg-white/10 hover:bg-white/20 text-white"
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
