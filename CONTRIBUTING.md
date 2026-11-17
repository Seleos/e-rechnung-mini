# Contributing

Thank you for helping. Bug reports, ideas and pull requests are welcome.

## Reporting a problem

Open an issue and describe what you did, what you expected and what happened instead. Please remove real
customer and bank data from screenshots and files before you attach them.

Security problems are reported privately, see [SECURITY.md](SECURITY.md).

## Changing the code

1. Read [AGENTS.md](AGENTS.md). It holds the rules for language, code style, architecture and tests.
2. You need JDK 25 or newer. Nothing else has to be installed.
3. Run the app with `./mvnw compile exec:java`.
4. Before you open a pull request, run `./mvnw verify`. It runs the tests and every quality check and
   must pass. `./mvnw spotless:apply` fixes formatting findings.
5. Add your change to the "Unreleased" section of [CHANGELOG.md](CHANGELOG.md) if users will notice it.

6. Write commit messages as [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), for
   example `fix(core): render invoices one at a time`. The allowed types and scopes are listed in AGENTS.md.

Keep pull requests small and focused on one topic. Changes to invoice generation need a test that
validates the generated file.

## Translations

A new language is one file: copy `src/main/resources/einvoice/messages_en.properties`, translate it and
add a constant to `einvoice.i18n.Language`.

## Licence

By contributing you agree that your contribution is licensed under the Apache License 2.0.
