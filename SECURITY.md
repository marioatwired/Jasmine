# Security Policy

## Supported versions

Only the latest Jasmine release receives security fixes.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Use GitHub's private vulnerability reporting:

https://github.com/marioatwired/Jasmine/security/advisories/new

Include:

- Affected version and commit.
- Steps to reproduce.
- Impact and any known workaround.
- Whether the issue is already public.

You will receive an acknowledgement as soon as possible. Please allow time for
a fix and a coordinated disclosure before publishing details.

## Secrets

Never commit a Discord bot token, OAuth refresh token, proxy credentials, or
`config.txt`. The repository ignores runtime configuration files by default.
