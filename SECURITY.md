# Security Policy

AdShield runs a `VpnService` and handles every DNS query on the device, so security issues are taken seriously.

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Instead, use GitHub's
[private vulnerability reporting](../../security/advisories/new) for this repository.

Include:
- A description of the issue and its impact
- Steps or a proof of concept to reproduce
- Affected version / commit

You should receive a response within a few days. Once a fix is released, the report will be credited (unless you prefer otherwise).

## Scope

In scope: DNS packet parsing (`DnsPacketHandler`), the VPN tunnel (`AdShieldVpnService`), blocklist download/parsing, and any data leaving the device.

Out of scope: the content of third-party blocklists — report those to the list maintainers.
