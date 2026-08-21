"use client";

import { User, Bot } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import type { ChatMessage, TimestampSource } from "@/lib/types";

interface MessageProps {
  message: ChatMessage;
}

export function Message({ message }: MessageProps) {
  const isUser = message.role === "user";

  return (
    <div className={`flex gap-3 ${isUser ? "justify-end" : "justify-start"}`}>
      {/* Avatar (AI only) */}
      {!isUser && (
        <Avatar className="h-7 w-7 shrink-0 mt-0.5">
          <AvatarFallback className="bg-blue-600 text-white text-xs">
            <Bot className="h-4 w-4" />
          </AvatarFallback>
        </Avatar>
      )}

      <div
        className={`max-w-[85%] rounded-2xl px-4 py-3 ${
          isUser
            ? "bg-blue-600 text-white"
            : "bg-[#1e1e1e] text-white/90 border border-white/5"
        }`}
      >
        {/* Message content */}
        <div className="text-sm leading-relaxed whitespace-pre-wrap">
          {message.content}
        </div>

        {/* Timestamp sources */}
        {message.sources && message.sources.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {message.sources.map((source, i) => (
              <TimestampChip key={i} source={source} />
            ))}
          </div>
        )}

        {/* Timestamp */}
        <div
          className={`mt-2 text-[10px] ${
            isUser ? "text-white/60" : "text-white/30"
          }`}
        >
          {new Date(message.timestamp).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}
        </div>
      </div>

      {/* Avatar (User only) */}
      {isUser && (
        <Avatar className="h-7 w-7 shrink-0 mt-0.5">
          <AvatarFallback className="bg-white/20 text-white text-xs">
            <User className="h-4 w-4" />
          </AvatarFallback>
        </Avatar>
      )}
    </div>
  );
}

function TimestampChip({ source }: { source: TimestampSource }) {
  const handleClick = () => {
    // Open YouTube at specific timestamp
    const videoUrl = `https://www.youtube.com/watch?v=${source.timestamp}`;
    window.open(videoUrl, "_blank");
  };

  return (
    <Badge
      variant="outline"
      className="cursor-pointer bg-blue-500/10 border-blue-500/30 text-blue-400 hover:bg-blue-500/20 transition-colors text-xs gap-1"
      onClick={handleClick}
    >
      ▶ {source.timestamp}
    </Badge>
  );
}
