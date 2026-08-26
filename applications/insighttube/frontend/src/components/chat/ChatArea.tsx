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
      <div className="flex flex-1 flex-col items-center justify-center bg-gradient-to-b from-[#0c0c0e] to-[#101014]">
        <div className="text-center">
          <div className="rounded-2xl bg-gradient-to-br from-rose-500/10 to-orange-500/10 p-5 border border-rose-500/5 mx-auto mb-4 w-fit">
            <Video className="h-10 w-10 text-rose-400/80" />
          </div>
          <h2 className="text-xl font-semibold text-white/80 mb-2 tracking-tight">
            InsightTube
          </h2>
          <p className="text-sm text-slate-500 max-w-md leading-relaxed">
            Paste a YouTube link to start chatting with any video, playlist, or
            channel. Ask questions, find timestamps, and explore content
            conversationally.
          </p>
          <div className="mt-8 flex flex-col gap-2.5 text-xs text-slate-600">
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-500/50" />
              &quot;What does this video say about authentication?&quot;
            </div>
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-orange-500/50" />
              &quot;Find the part where they discuss database design&quot;
            </div>
            <div className="flex items-center gap-2 justify-center">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500/50" />
              &quot;Summarize the key takeaways from this lecture&quot;
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col bg-[#0c0c0e] min-h-0">
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
            <div className="flex items-center gap-2 text-white/30">
              <div className="flex gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-400/60 animate-bounce [animation-delay:0ms]" />
                <span className="w-1.5 h-1.5 rounded-full bg-orange-400/60 animate-bounce [animation-delay:150ms]" />
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400/60 animate-bounce [animation-delay:300ms]" />
              </div>
              <span className="text-xs">Thinking...</span>
            </div>
          )}

          {/* Bottom spacer so last message isn't hidden behind input */}
          <div className="h-2" />
        </div>
      </div>

      {/* Input — fixed at bottom, never jumps */}
      <div className="shrink-0 border-t border-white/5 bg-[#0c0c0e] p-4">
        <ChatInput />
      </div>
    </div>
  );
}
