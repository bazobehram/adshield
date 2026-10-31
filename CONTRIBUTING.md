# Contributing to AdShield

Thanks for your interest! Bug reports, blocklist suggestions, UI polish, translations, and engine improvements are all welcome.

## Getting started

1. Fork and clone the repo.
2. Install **Android Studio** (Ladybug or newer) or the Android SDK + **JDK 17**.
3. Build:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. Install on a device/emulator:
   ```bash
   ./scripts/deploy.sh -e        # emulator
   ./scripts/deploy.sh -d        # USB device
   ```
   On Windows use `.\scripts\deploy.ps1` with the same flags.

## Project layout

```
app/src/main/java/com/local/adshield/
├── vpn/            # VpnService, DNS wire-format parser, filter trie  (hot path)
├── data/
│   ├── local/      # Room database, DAOs, SharedPreferences
│   ├── model/      # Entities & value types
│   ├── repository/ # Blocklist download/parse, rules, query logs
│   └── worker/     # WorkManager periodic sync
├── receiver/       # BootReceiver (auto-start)
└── ui/             # Jetpack Compose screens, ViewModels, theme
```

## Ground rules

These keep AdShield fast and battery-friendly — PRs that break them will be asked to change:

- **Only DNS goes through the tunnel.** The VPN routes only `10.0.0.1/32`. Do not add routes that capture general TCP/UDP traffic.
- **No work on the packet hot path that blocks.** Database writes, logging, and network I/O for logs must stay asynchronous.
- **No telemetry, analytics SDKs, or remote servers.** Everything stays on-device. The only outbound requests are upstream DNS and blocklist downloads the user has enabled.
- **No new heavy dependencies** without discussion in an issue first.

## Code style

- Kotlin, coroutines + Flow, Jetpack Compose / Material 3.
- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html); `.editorconfig` is provided.
- Keep functions small and name things clearly; add KDoc for non-obvious logic (especially DNS byte-level code).

## Pull requests

1. Create a branch: `git checkout -b feature/short-description`.
2. Keep PRs focused — one feature or fix per PR.
3. Make sure `./gradlew :app:assembleDebug :app:lintDebug` passes (CI runs it too).
4. Add screenshots for UI changes.
5. Use clear commit messages, e.g. `fix(vpn): handle truncated DNS responses`.

## Good first issues

- Unit tests for `DnsPacketHandler` and `DomainFilterEngine`
- Translations (`res/values-xx/strings.xml`)
- DNS-over-HTTPS / DNS-over-TLS upstream support
- Per-app statistics (UID resolution)
- Import/export of custom rules

## Reporting bugs

Open an issue using the bug report template. For security issues, see [SECURITY.md](SECURITY.md).

By contributing you agree that your contributions are licensed under the [MIT License](LICENSE).
