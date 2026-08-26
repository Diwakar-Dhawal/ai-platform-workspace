"use client";

import { useState, useRef, useCallback } from "react";
import { Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useChatStore } from "@/stores/chatStore";

export function ChatInput() {
  const [input, setInput] = useState("");
  const sendMessage = useChatStore((s) => s.sendMessage);
  const sendingMessage = useChatStore((s) => s.sendingMessage);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = useCallback(() => {
    if (!input.trim() || sendingMessage) return;
    sendMessage(input.trim());
    setInput("");
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }
  }, [input, sendingMessage, sendMessage]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    // Auto-resize but cap at max height
    const el = e.target;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  };

  return (
    <div className="mx-auto max-w-3xl">
      <div className="relative rounded-2xl border border-white/5 bg-[#18181c] focus-within:border-rose-500/40 transition-all duration-200 shadow-lg shadow-black/20">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder="Ask about this video..."
          rows={1}
          className="w-full resize-none bg-transparent px-4 py-3 pr-12 text-sm text-white placeholder:text-slate-600 focus:outline-none min-h-[44px] max-h-[160px]"
          disabled={sendingMessage}
        />
        <Button
          size="icon"
          onClick={handleSubmit}
          disabled={!input.trim() || sendingMessage}
          className="absolute right-2 bottom-2 h-8 w-8 rounded-full bg-gradient-to-r from-rose-600 to-orange-600 hover:from-rose-500 hover:to-orange-500 disabled:opacity-20 shadow-md shadow-rose-500/20 transition-all duration-200"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
      <p className="mt-2 text-center text-[10px] text-slate-600">
        Press Enter to send, Shift+Enter for new line
      </p>
    </div>
  );
}
