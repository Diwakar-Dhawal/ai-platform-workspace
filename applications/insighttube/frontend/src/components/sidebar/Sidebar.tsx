"use client";

import { useState, useRef, useEffect } from "react";
import {
  MessageSquarePlus,
  Trash2,
  Video,
  Play,
  Search,
  Menu,
  X,
  LogOut,
  Pencil,
  User,
  ChevronDown,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Input } from "@/components/ui/input";
import { useChatStore } from "@/stores/chatStore";
import { useIngestStore } from "@/stores/ingestStore";
import { useAuthStore } from "@/stores/authStore";
import { IngestModal } from "@/components/ingest/IngestModal";

export function Sidebar() {
  const sessions = useChatStore((s) => s.sessions);
  const activeSessionId = useChatStore((s) => s.activeSessionId);
  const setActiveSession = useChatStore((s) => s.setActiveSession);
  const removeSession = useChatStore((s) => s.removeSession);
  const renameSession = useChatStore((s) => s.renameSession);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const [showIngestModal, setShowIngestModal] = useState(false);
  const [search, setSearch] = useState("");
  const [collapsed, setCollapsed] = useState(false);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [showProfile, setShowProfile] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);
  const renameInputRef = useRef<HTMLInputElement>(null);

  // Focus rename input
  useEffect(() => {
    if (renamingId && renameInputRef.current) {
      renameInputRef.current.focus();
      renameInputRef.current.select();
    }
  }, [renamingId]);

  // Close profile dropdown on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setShowProfile(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const startRename = (sessionId: string, currentTitle: string) => {
    setRenamingId(sessionId);
    setRenameValue(currentTitle);
  };

  const commitRename = () => {
    if (renamingId && renameValue.trim()) {
      renameSession(renamingId, renameValue.trim());
    }
    setRenamingId(null);
    setRenameValue("");
  };

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
        className={`flex flex-col border-r border-white/5 bg-[#141417] transition-all duration-300 ${
          collapsed ? "w-16" : "w-72"
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-3">
          {!collapsed && (
            <h1 className="text-sm font-semibold text-white/90 tracking-tight">
              InsightTube
            </h1>
          )}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setCollapsed(!collapsed)}
            className="h-8 w-8 text-white/50 hover:text-white hover:bg-white/5"
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
                className="w-full justify-start gap-2 bg-white/5 text-white/80 hover:bg-white/10 hover:text-white border border-white/5"
                variant="ghost"
              >
                <MessageSquarePlus className="h-4 w-4" />
                New Chat
              </Button>
            </div>

            {/* Search */}
            <div className="px-3 pb-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-white/30" />
                <Input
                  placeholder="Search chats..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="h-8 bg-white/5 border-white/5 text-white placeholder:text-white/30 pl-7 text-xs focus:border-rose-500/50 focus:ring-rose-500/20"
                />
              </div>
            </div>

            <Separator className="bg-white/5" />

            {/* Profile dropdown */}
            <div className="px-3 py-2" ref={profileRef}>
              <button
                onClick={() => setShowProfile(!showProfile)}
                className="flex items-center gap-2 w-full rounded-lg bg-white/5 px-2 py-1.5 hover:bg-white/10 transition-colors"
              >
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-rose-500/30 to-orange-500/30 text-[10px] text-rose-300 font-medium">
                  {user?.username?.[0]?.toUpperCase() || "U"}
                </div>
                <span className="text-xs text-white/60 truncate flex-1 text-left">
                  {user?.username || "User"}
                </span>
                <ChevronDown
                  className={`h-3 w-3 text-white/30 transition-transform ${
                    showProfile ? "rotate-180" : ""
                  }`}
                />
              </button>

              {/* Profile dropdown panel */}
              {showProfile && (
                <div className="mt-1 rounded-lg bg-[#1c1c20] border border-white/5 p-3 space-y-2">
                  <div className="flex items-center gap-2">
                    <User className="h-4 w-4 text-white/30" />
                    <div>
                      <p className="text-xs text-white/80 font-medium">
                        {user?.username}
                      </p>
                      <p className="text-[10px] text-white/40">{user?.email}</p>
                    </div>
                  </div>
                  <div className="text-[10px] text-white/30">
                    Roles: {user?.roles?.join(", ") || "USER"}
                  </div>
                  <Separator className="bg-white/5" />
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={logout}
                    className="w-full justify-start gap-2 text-red-400 hover:text-red-300 hover:bg-red-500/10 text-xs h-7"
                  >
                    <LogOut className="h-3 w-3" />
                    Sign out
                  </Button>
                </div>
              )}
            </div>

            <Separator className="bg-white/5" />

            {/* Chat history */}
            <ScrollArea className="flex-1 px-2 py-2">
              {Object.entries(grouped).map(([url, group]) => (
                <div key={url} className="mb-3">
                  {/* Video header */}
                  <div className="flex items-center gap-2 px-2 py-1">
                    <Video className="h-3 w-3 text-rose-400/70 shrink-0" />
                    <span className="text-xs text-white/40 truncate">
                      {group.title || "Processing..."}
                    </span>
                  </div>

                  {/* Sessions under this video */}
                  {group.sessions.map((session) => (
                    <div
                      key={session.id}
                      onClick={() => {
                        if (renamingId !== session.id) {
                          setActiveSession(session.id);
                        }
                      }}
                      className={`group flex items-center gap-2 rounded-lg px-2 py-1.5 cursor-pointer transition-colors ${
                        activeSessionId === session.id
                          ? "bg-white/10 text-white"
                          : "text-white/50 hover:bg-white/5 hover:text-white/70"
                      }`}
                    >
                      <Play className="h-3 w-3 shrink-0" />

                      {renamingId === session.id ? (
                        <input
                          ref={renameInputRef}
                          value={renameValue}
                          onChange={(e) => setRenameValue(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") commitRename();
                            if (e.key === "Escape") setRenamingId(null);
                          }}
                          onBlur={commitRename}
                          className="flex-1 text-xs bg-white/10 rounded px-1.5 py-0.5 text-white outline-none border border-rose-500/50"
                          onClick={(e) => e.stopPropagation()}
                        />
                      ) : (
                        <span className="text-xs truncate flex-1">
                          {session.title || "Chat"}
                        </span>
                      )}

                      {/* Action buttons */}
                      <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                        {renamingId !== session.id && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              startRename(session.id, session.title || "Chat");
                            }}
                            className="p-0.5 rounded text-white/30 hover:text-white hover:bg-white/10"
                            title="Rename"
                          >
                            <Pencil className="h-3 w-3" />
                          </button>
                        )}
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            removeSession(session.id);
                          }}
                          className="p-0.5 rounded text-white/30 hover:text-red-400 hover:bg-red-500/10"
                          title="Delete"
                        >
                          <Trash2 className="h-3 w-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ))}

              {sessions.length === 0 && (
                <div className="px-4 py-8 text-center">
                  <Video className="h-8 w-8 text-white/10 mx-auto mb-2" />
                  <p className="text-xs text-white/30">
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
      <IngestModal open={showIngestModal} onOpenChange={setShowIngestModal} />
    </>
  );
}
