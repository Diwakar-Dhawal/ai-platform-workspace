"use client";

import { useState } from "react";
import { Video, ArrowRight, Loader2 } from "lucide-react";
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
    setUrl("");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="bg-[#1e1e1e] border-white/10 text-white max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Video className="h-5 w-5 text-red-500" />
            Add YouTube Content
          </DialogTitle>
          <DialogDescription className="text-white/50">
            Paste a video, playlist, or channel URL to start chatting with the
            content.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 mt-4">
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
            {isIngesting ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Ingesting...
              </>
            ) : (
              <>
                Start Ingesting
                <ArrowRight className="h-4 w-4 ml-2" />
              </>
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
