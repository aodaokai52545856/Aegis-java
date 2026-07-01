# Aegis Agent Desktop Client

Tauri 2 + Vue 3 desktop client for `agentscope-service`.

## Prerequisites

- Node.js 20+
- Rust toolchain (for `tauri dev` / `tauri build`)
- Running `agentscope-service` on port 8010
- At least one DeepSeek API key registered via Admin API

## Development

```bash
cd agentscope-tauri-agent
npm install
npm run tauri:dev
```

## Web-only dev (without Tauri shell)

```bash
npm install
npm run dev
```

Open http://localhost:1420
