"use client";

import { useState, useRef, useEffect } from "react";
import { Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useChatStore } from "@/stores/chatStore";

export function ChatInput() {
  const [input, setInput] = useState("");
  const sendMessage = useChatStore((s) => s.sendMessage);
  const sendingMessage = useChatStore((s) => s.sendingMessage);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(
        textareaRef.current.scrollHeight,
        200
      )}px`;
    }
  }, [input]);

  const handleSubmit = () => {
    if (!input.trim() || sendingMessage) return;
    sendMessage(input.trim());
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="mx-auto max-w-3xl">
      <div className="relative rounded-2xl border border-white/15 bg-[#1e1e1e] focus-within:border-blue-500/50 transition-colors">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask about this video..."
          rows={1}
          className="w-full resize-none bg-transparent px-4 py-3 pr-12 text-sm text-white placeholder:text-white/40 focus:outline-none"
          disabled={sendingMessage}
        />
        <Button
          size="icon"
          onClick={handleSubmit}
          disabled={!input.trim() || sendingMessage}
          className="absolute right-2 bottom-2 h-8 w-8 rounded-full bg-blue-600 hover:bg-blue-500 disabled:opacity-30"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
      <p className="mt-2 text-center text-[10px] text-white/30">
        Press Enter to send, Shift+Enter for new line
      </p>
    </div>
  );
}
