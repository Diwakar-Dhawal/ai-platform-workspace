"use client";

import { useEffect, useRef } from "react";
import { MessageSquare, Video } from "lucide-react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useChatStore } from "@/stores/chatStore";
import { Message } from "./Message";
import { ChatInput } from "./ChatInput";

export function ChatArea() {
  const activeSessionId = useChatStore((s) => s.activeSessionId);
  const messages = useChatStore((s) => s.messages);
  const sendingMessage = useChatStore((s) => s.sendingMessage);
  const scrollRef = useRef<HTMLDivElement>(null);

  const activeMessages = activeSessionId ? messages[activeSessionId] || [] : [];

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
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
    <div className="flex flex-1 flex-col bg-[#0d0d0d]">
      {/* Messages */}
      <ScrollArea className="flex-1 px-4" ref={scrollRef}>
        <div className="mx-auto max-w-3xl py-6 space-y-6">
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
        </div>
      </ScrollArea>

      {/* Input */}
      <div className="border-t border-white/10 bg-[#0d0d0d] p-4">
        <ChatInput />
      </div>
    </div>
  );
}
