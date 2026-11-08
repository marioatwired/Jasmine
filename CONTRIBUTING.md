# Contributing to Jasmine

Thanks for considering a contribution.

## Before you start

- Check existing [issues](https://github.com/marioatwired/Jasmine/issues) and [discussions](https://github.com/marioatwired/Jasmine/discussions).
- For security issues, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
- Keep changes focused. One feature or fix per pull request.

## Development setup

Requirements:

- JDK 25
- Maven 3.8+ (or the Maven Wrapper: `./mvnw` / `mvnw.cmd`)

```bash
mvn clean verify
mvn package
java -Dnogui=true --enable-native-access=ALL-UNNAMED -jar target/jasmine-0.4.0-All.jar
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the full workflow.

## Branches and commits

Use short-lived branches from `master`:

- `feature/<slug>`
- `fix/<slug>`
- `chore/<slug>`
- `deps/<slug>`
- `release/<version>`

Use conventional commits when possible:

```text
feat: add playlist import
fix: handle missing voice channel
chore: update dependencies
docs: document proxy settings
refactor: split queue helpers
test: cover repeat modes
```

## Pull requests

- Run `mvn verify` before opening the PR.
- Describe the problem, the solution, and how you tested it.
- Update documentation when behavior or configuration changes.
- Do not remove or rewrite existing copyright headers. New files can use:

```text
Copyright 2026 Mario (marioatwired)
Licensed under the Apache License, Version 2.0.
```

## Configuration changes

If you add or change a config key:

1. Update `src/main/resources/reference.conf`.
2. Update `ConfigOption` and, if needed, a migration under `config/migration/versions`.
3. Update `docs/CONFIG.md`.
4. Add or update tests for loading, migration, and validation.

## License

By contributing, you agree that your contributions are licensed under the
Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
