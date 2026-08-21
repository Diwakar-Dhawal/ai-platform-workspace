"use client";

import { User, Bot } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { MarkdownContent } from "./MarkdownContent";
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
        {/* Message content — render markdown for AI, plain for user */}
        {isUser ? (
          <div className="text-sm leading-relaxed whitespace-pre-wrap">
            {message.content}
          </div>
        ) : (
          <MarkdownContent content={message.content} />
        )}

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
  // Display-only badge — video ID is not available in the source object,
  // so we can't construct a clickable YouTube link. Citations in the
  // text itself (e.g. [Video at 5:30]) serve as references.
  return (
    <Badge
      variant="outline"
      className="bg-blue-500/10 border-blue-500/30 text-blue-400 text-xs gap-1"
    >
      ▶ {source.timestamp}
    </Badge>
  );
}
