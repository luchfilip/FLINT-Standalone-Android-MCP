# Contributing to ACP

Thank you for your interest in contributing to the Android Capability Protocol.

## Getting Started

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Make your changes
4. Run tests to make sure everything passes
5. Commit with a descriptive message (see below)
6. Push to your fork and open a Pull Request

## Development Setup

### Prerequisites

- JDK 17+
- Android SDK (API 34+)
- Android Studio or IntelliJ IDEA (recommended)

### Building

```bash
# Build everything
./gradlew build

# Build individual modules
./gradlew :sdk:annotations:build
./gradlew :sdk:compiler:build
./gradlew :sdk:runtime:build
./gradlew :hub:app:assembleDebug
./gradlew :sample:musicapp:assembleDebug
```

### Running Tests

```bash
# All tests
./gradlew test

# Module-specific tests
./gradlew :sdk:annotations:test
./gradlew :sdk:compiler:test
```

## Coding Standards

- Follow existing Kotlin conventions used throughout the codebase
- Use the `.editorconfig` settings (4-space indent, UTF-8, LF line endings)
- Keep public API surfaces minimal and well-documented
- Write tests for new functionality

## Commit Messages

Use conventional commit format:

```
type(scope): short description

- Detail 1
- Detail 2
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

Scopes: `sdk`, `hub`, `sample`, `docs`

## Project Structure

| Module | Description |
|--------|-------------|
| `sdk/annotations` | ACP annotation definitions |
| `sdk/compiler` | KSP processor generating `acp-manifest.json` |
| `sdk/runtime` | Android runtime library (ContentProvider, models) |
| `hub/app` | Hub Android app (MCP server, tools, UI) |
| `sample/musicapp` | Example ACP-enabled app |

## Pull Requests

- Keep PRs focused on a single concern
- Include tests for new features or bug fixes
- Update documentation if you change public APIs
- Reference related issues in the PR description

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
