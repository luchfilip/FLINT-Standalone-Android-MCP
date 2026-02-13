# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2025-06-01

### Added

- **Flint SDK Annotations** (`sdk/annotations`): Kotlin annotations (`@FlintTool`, `@FlintScreen`, `@FlintAction`, `@FlintSemanticTree`) for declaring app capabilities to the Hub.
- **Flint SDK Compiler** (`sdk/compiler`): KSP annotation processor that generates `flint-manifest.json` at build time, describing all tools, screens, and semantic actions an app exposes.
- **Flint SDK Runtime** (`sdk/runtime`): Android library providing `FlintProvider` (ContentProvider-based discovery), `FlintActionInvoker`, `FlintTreeWalker`, and data models for runtime communication between Hub and Flint-enabled apps.
- **Hub Application** (`hub/app`): Android app running an embedded Ktor MCP server over HTTP/SSE. Includes:
  - Foreground service with persistent notification
  - AccessibilityService for universal screen reading and gesture dispatch
  - NotificationListenerService for notification access
  - Built-in device tools: screenshot, tap, swipe, type, scroll, press key
  - Communication tools: SMS, calls, contacts, notifications
  - System tools: battery, Wi-Fi, Bluetooth, volume, clipboard
  - App management tools: list, launch, close
  - Flint app discovery via ContentProvider scanning
  - Automatic tool registration for Flint-enabled apps (search, navigate, read screen, invoke actions)
  - Jetpack Compose UI for status, permissions, Flint apps, and settings
- **Sample Music App** (`sample/musicapp`): Demonstrates Flint integration with annotated tools (`search`, `open_playlist`), screens (`home`, `playlist_detail`, `track_detail`, `search_results`), and semantic UI elements. Built with Jetpack Compose and Hilt.
- **Full protocol specification** (`flint-full-spec-v1.md`): Complete documentation of the Flint architecture, SDK integration guide, manifest format, and Hub behavior.
