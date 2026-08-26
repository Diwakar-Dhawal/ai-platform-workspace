"use client";

import React from "react";

/**
 * Lightweight markdown renderer — no external dependencies.
 * Handles: headings (## ###), bold (**text**), bullets (* item),
 * numbered lists (1. item), inline code (`code`), and paragraph breaks.
 *
 * Intentionally minimal: avoids adding react-markdown as a dependency.
 */
export function MarkdownContent({ content }: { content: string }) {
  const blocks = parseBlocks(content);

  return (
    <div className="text-sm leading-relaxed">
      {blocks.map((block, i) => {
        switch (block.type) {
          case "heading":
            return (
              <div
                key={i}
                className={`font-semibold mt-3 mb-1 ${
                  block.level === 2 ? "text-base" : "text-sm"
                }`}
              >
                {renderInline(block.text)}
              </div>
            );

          case "bullet":
            return (
              <ul key={i} className="list-disc list-inside ml-2 my-1 space-y-0.5">
                {block.items.map((item, j) => (
                  <li key={j} className="text-sm">
                    {renderInline(item)}
                  </li>
                ))}
              </ul>
            );

          case "numbered":
            return (
              <ol key={i} className="list-decimal list-inside ml-2 my-1 space-y-0.5">
                {block.items.map((item, j) => (
                  <li key={j} className="text-sm">
                    {renderInline(item)}
                  </li>
                ))}
              </ol>
            );

          case "code":
            return (
              <pre
                key={i}
                className="my-2 rounded-md bg-black/40 border border-white/10 p-3 overflow-x-auto"
              >
                <code className="text-xs text-green-300 font-mono">
                  {block.text}
                </code>
              </pre>
            );

          default:
            return (
              <p key={i} className="my-1">
                {renderInline(block.text)}
              </p>
            );
        }
      })}
    </div>
  );
}

// ─── Block parsing ──────────────────────────────────────────

type Block =
  | { type: "heading"; level: number; text: string }
  | { type: "bullet"; items: string[] }
  | { type: "numbered"; items: string[] }
  | { type: "code"; text: string }
  | { type: "paragraph"; text: string };

function parseBlocks(text: string): Block[] {
  const lines = text.split("\n");
  const blocks: Block[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];
    const trimmed = line.trim();

    // Code block
    if (trimmed.startsWith("```")) {
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith("```")) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // skip closing ```
      blocks.push({ type: "code", text: codeLines.join("\n") });
      continue;
    }

    // Heading
    const headingMatch = trimmed.match(/^(#{2,4})\s+(.*)/);
    if (headingMatch) {
      blocks.push({
        type: "heading",
        level: headingMatch[1].length,
        text: headingMatch[2],
      });
      i++;
      continue;
    }

    // Bullet list (* or - or •)
    if (/^[\*\-•]\s+/.test(trimmed)) {
      const items: string[] = [];
      while (i < lines.length && /^[\*\-•]\s+/.test(lines[i].trim())) {
        items.push(lines[i].trim().replace(/^[\*\-•]\s+/, ""));
        i++;
      }
      blocks.push({ type: "bullet", items });
      continue;
    }

    // Numbered list (1. 2. etc)
    if (/^\d+\.\s+/.test(trimmed)) {
      const items: string[] = [];
      while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) {
        items.push(lines[i].trim().replace(/^\d+\.\s+/, ""));
        i++;
      }
      blocks.push({ type: "numbered", items });
      continue;
    }

    // Empty line — skip
    if (trimmed === "") {
      i++;
      continue;
    }

    // Paragraph — collect consecutive non-empty, non-special lines
    const paraLines: string[] = [];
    while (
      i < lines.length &&
      lines[i].trim() !== "" &&
      !/^#{2,4}\s/.test(lines[i].trim()) &&
      !/^[\*\-•]\s+/.test(lines[i].trim()) &&
      !/^\d+\.\s+/.test(lines[i].trim()) &&
      !lines[i].trim().startsWith("```")
    ) {
      paraLines.push(lines[i].trim());
      i++;
    }
    blocks.push({ type: "paragraph", text: paraLines.join(" ") });
  }

  return blocks;
}

// ─── Inline rendering ──────────────────────────────────────

function renderInline(text: string): React.ReactNode[] {
  // Process: **bold**, `code`, [text](url)
  const parts: React.ReactNode[] = [];
  let remaining = text;
  let key = 0;

  while (remaining.length > 0) {
    // Bold
    const boldMatch = remaining.match(/\*\*(.+?)\*\*/);
    // Code
    const codeMatch = remaining.match(/`(.+?)`/);
    // Link
    const linkMatch = remaining.match(/\[([^\]]+)\]\(([^)]+)\)/);

    // Find earliest match
    const matches = [
      { type: "bold" as const, match: boldMatch },
      { type: "code" as const, match: codeMatch },
      { type: "link" as const, match: linkMatch },
    ].filter((m) => m.match);

    if (matches.length === 0) {
      parts.push(remaining);
      break;
    }

    // Pick the match with the earliest index
    const earliest = matches.reduce((min, m) => {
      const idx = m.match!.index!;
      return idx < min.match!.index! ? m : min;
    });

    const idx = earliest.match!.index!;
    if (idx > 0) {
      parts.push(remaining.substring(0, idx));
    }

    if (earliest.type === "bold") {
      parts.push(
        <strong key={key++} className="font-semibold text-white/95">
          {earliest.match![1]}
        </strong>
      );
    } else if (earliest.type === "code") {
      parts.push(
        <code
          key={key++}
          className="rounded bg-black/40 px-1 py-0.5 text-xs text-green-300 font-mono"
        >
          {earliest.match![1]}
        </code>
      );
    } else if (earliest.type === "link") {
      parts.push(
        <a
          key={key++}
          href={earliest.match![2]}
          target="_blank"
          rel="noopener noreferrer"
          className="text-rose-400 underline hover:text-rose-300"
        >
          {earliest.match![1]}
        </a>
      );
    }

    remaining = remaining.substring(idx + earliest.match![0].length);
  }

  return parts;
}
