"use client";

import { useEffect, useRef, useCallback } from "react";
import { Video } from "lucide-react";
import { useChatStore } from "@/stores/chatStore";
import { Message } from "./Message";
import { ChatInput } from "./ChatInput";

export function ChatArea() {
  const activeSessionId = useChatStore((s) => s.activeSessionId);
  const messages = useChatStore((s) => s.messages);
  const sendingMessage = useChatStore((s) => s.sendingMessage);
  const scrollRef = useRef<HTMLDivElement>(null);
  const shouldAutoScroll = useRef(true);

  const activeMessages = activeSessionId ? messages[activeSessionId] || [] : [];

  // Track if user scrolled up (disable auto-scroll)
  const handleScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100;
    shouldAutoScroll.current = atBottom;
  }, []);

  // Auto-scroll to bottom on new messages (only if user is at bottom)
  useEffect(() => {
    if (shouldAutoScroll.current && scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior: "smooth",
      });
    }
  }, [activeMessages.length, sendingMessage]);

  // No active session — show welcome screen
  if (!activeSessionId) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-[#0d0d0d]">
        <div className="text-center">
          <Video className="h-12 w-12 text-red-500/60 mx-auto mb-4" />
          <h2 className="text-xl font-medium text-white/80 mb-2">
            InsightTube
          </h2>
          <p className="text-sm text-white/40 max-w-md">
            Paste a YouTube link to start chatting with any video, playlist, or
            channel. Ask questions, find timestamps, and explore content
            conversationally.
          </p>
          <div className="mt-6 flex flex-col gap-2 text-xs text-white/30">
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-500/60" />
              &quot;What does this video say about authentication?&quot;
            </div>
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500/60" />
              &quot;Find the part where they discuss database design&quot;
            </div>
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-purple-500/60" />
              &quot;Summarize the key takeaways from this lecture&quot;
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col bg-[#0d0d0d] min-h-0">
      {/* Messages — scrollable container */}
      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto overflow-x-hidden"
      >
        <div className="mx-auto max-w-3xl px-4 py-6 space-y-6">
          {activeMessages.map((msg) => (
            <Message key={msg.id} message={msg} />
          ))}

          {sendingMessage && (
            <div className="flex items-center gap-2 text-white/40">
              <div className="flex gap-1">
                <span className="w-2 h-2 rounded-full bg-white/40 animate-bounce [animation-delay:0ms]" />
                <span className="w-2 h-2 rounded-full bg-white/40 animate-bounce [animation-delay:150ms]" />
                <span className="w-2 h-2 rounded-full bg-white/40 animate-bounce [animation-delay:300ms]" />
              </div>
              <span className="text-xs">Thinking...</span>
            </div>
          )}

          {/* Bottom spacer so last message isn't hidden behind input */}
          <div className="h-2" />
        </div>
      </div>

      {/* Input — fixed at bottom, never jumps */}
      <div className="shrink-0 border-t border-white/10 bg-[#0d0d0d] p-4">
        <ChatInput />
      </div>
    </div>
  );
}
