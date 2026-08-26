# InsightTube Frontend

> Next.js 16 + React 19 frontend for the InsightTube application.

## Development

```bash
npm install
npm run dev
```

Opens at [http://localhost:3000](http://localhost:3000).

API calls are proxied to backend services via Next.js rewrites (see `next.config.ts`).

## Build

```bash
npm run build
npm start
```

## Lint

```bash
npm run lint
```

## Project Structure

- `src/app/` — Next.js App Router
- `src/components/` — React components (auth, chat, ingest, layout, sidebar, ui)
- `src/stores/` — Zustand state stores
- `src/lib/` — API client, TypeScript types, custom hooks, utilities
