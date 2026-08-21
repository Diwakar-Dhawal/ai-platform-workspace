"use client";

import { useState } from "react";
import {
  MessageSquarePlus,
  Trash2,
  Video,
  Play,
  Search,
  Menu,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Input } from "@/components/ui/input";
import { useChatStore } from "@/stores/chatStore";
import { useIngestStore } from "@/stores/ingestStore";
import { IngestModal } from "@/components/ingest/IngestModal";

export function Sidebar() {
  const sessions = useChatStore((s) => s.sessions);
  const activeSessionId = useChatStore((s) => s.activeSessionId);
  const setActiveSession = useChatStore((s) => s.setActiveSession);
  const removeSession = useChatStore((s) => s.removeSession);
  const [showIngestModal, setShowIngestModal] = useState(false);
  const [search, setSearch] = useState("");
  const [collapsed, setCollapsed] = useState(false);

  // Group sessions by sourceUrl (video)
  const grouped = sessions
    .filter(
      (s) =>
        !search ||
        s.title.toLowerCase().includes(search.toLowerCase()) ||
        s.sourceUrl.toLowerCase().includes(search.toLowerCase())
    )
    .reduce(
      (acc, session) => {
        const key = session.sourceUrl;
        if (!acc[key]) {
          acc[key] = { title: session.title, sessions: [] };
        }
        acc[key].sessions.push(session);
        return acc;
      },
      {} as Record<string, { title: string; sessions: typeof sessions }>
    );

  return (
    <>
      <div
        className={`flex flex-col border-r border-white/10 bg-[#171717] transition-all duration-300 ${
          collapsed ? "w-16" : "w-72"
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-3">
          {!collapsed && (
            <h1 className="text-sm font-semibold text-white/90">
              InsightTube
            </h1>
          )}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setCollapsed(!collapsed)}
            className="h-8 w-8 text-white/60 hover:text-white hover:bg-white/10"
          >
            {collapsed ? <Menu className="h-4 w-4" /> : <X className="h-4 w-4" />}
          </Button>
        </div>

        {!collapsed && (
          <>
            {/* New chat button */}
            <div className="px-3 pb-2">
              <Button
                onClick={() => setShowIngestModal(true)}
                className="w-full justify-start gap-2 bg-white/10 text-white hover:bg-white/20"
                variant="ghost"
              >
                <MessageSquarePlus className="h-4 w-4" />
                New Chat
              </Button>
            </div>

            {/* Search */}
            <div className="px-3 pb-2">
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-3.5 w-3.5 text-white/40" />
                <Input
                  placeholder="Search chats..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="h-8 bg-white/5 border-white/10 text-white placeholder:text-white/40 pl-7 text-xs"
                />
              </div>
            </div>

            <Separator className="bg-white/10" />

            {/* Chat history */}
            <ScrollArea className="flex-1 px-2 py-2">
              {Object.entries(grouped).map(([url, group]) => (
                <div key={url} className="mb-3">
                  {/* Video header */}
                  <div className="flex items-center gap-2 px-2 py-1">
                    <Video className="h-3 w-3 text-blue-400 shrink-0" />
                    <span className="text-xs text-white/50 truncate">
                      {group.title || "Processing..."}
                    </span>
                  </div>

                  {/* Sessions under this video */}
                  {group.sessions.map((session) => (
                    <div
                      key={session.id}
                      onClick={() => setActiveSession(session.id)}
                      className={`group flex items-center gap-2 rounded-md px-2 py-1.5 cursor-pointer transition-colors ${
                        activeSessionId === session.id
                          ? "bg-white/15 text-white"
                          : "text-white/60 hover:bg-white/8 hover:text-white/80"
                      }`}
                    >
                      <Play className="h-3 w-3 shrink-0" />
                      <span className="text-xs truncate flex-1">
                        {session.title || "Chat"}
                      </span>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-5 w-5 opacity-0 group-hover:opacity-100 text-white/40 hover:text-red-400"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeSession(session.id);
                        }}
                      >
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    </div>
                  ))}
                </div>
              ))}

              {sessions.length === 0 && (
                <div className="px-4 py-8 text-center">
                  <Video className="h-8 w-8 text-white/20 mx-auto mb-2" />
                  <p className="text-xs text-white/40">
                    No chats yet.
                    <br />
                    Paste a YouTube link to start.
                  </p>
                </div>
              )}
            </ScrollArea>
          </>
        )}
      </div>

      {/* Ingest Modal */}
      <IngestModal
        open={showIngestModal}
        onOpenChange={setShowIngestModal}
      />
    </>
  );
}
