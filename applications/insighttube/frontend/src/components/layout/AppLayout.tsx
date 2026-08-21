"use client";

import { useEffect } from "react";
import { Sidebar } from "@/components/sidebar/Sidebar";
import { ChatArea } from "@/components/chat/ChatArea";
import { IngestBar } from "@/components/ingest/IngestBar";
import { useChatStore } from "@/stores/chatStore";
import { useIngestStore } from "@/stores/ingestStore";

export function AppLayout() {
  const loadSessions = useChatStore((s) => s.loadSessions);
  const isIngesting = useIngestStore((s) => s.isIngesting);

  useEffect(() => {
    loadSessions();

    // Listen for ingest completion to refresh sessions
    const handleIngestComplete = () => {
      loadSessions();
    };
    window.addEventListener("ingest-completed", handleIngestComplete);
    return () =>
      window.removeEventListener("ingest-completed", handleIngestComplete);
  }, [loadSessions]);

  return (
    <div className="flex h-screen overflow-hidden dark">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Chat area */}
        <ChatArea />

        {/* Ingest progress bar (shown during ingestion) */}
        {isIngesting && <IngestBar />}
      </div>
    </div>
  );
}
