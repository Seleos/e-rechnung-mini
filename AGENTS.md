# Project rules

Rules for everyone working on this code base, human or AI agent.

## Language

- All code is written in English: packages, classes, methods, fields, comments, Javadoc, commit messages.
- Texts shown to the user never appear as string literals in the code. They live in
  `src/main/resources/einvoice/messages_<language>.properties` and are looked up with `Messages.get(key)`.
  Every key must exist in every language file; a test enforces this.
- Texts printed on the invoice (stylesheet labels, mandatory tax notes) and the standard e-mail texts
  follow the invoice language of the customer. They live in `invoice_<language>.properties` and are read
  with `InvoiceTexts`. The stylesheet receives the `label.` and `unit.` entries as parameters.

## Clean code

- Small classes and methods with one responsibility and descriptive names. No abbreviations and no
  single-letter names, except loop indices.
- No dead code, no commented-out code, no duplicated logic.
- Fields are private. Model classes expose them through getters and setters.
- Fail early with a clear message instead of continuing with invalid data.

## Java conventions

- Follow the standard Java code conventions: 4 spaces, no tabs, braces on every block, one statement per
  line, lines up to 120 characters, no wildcard imports.
- The rules are enforced by Checkstyle (`config/checkstyle.xml`). Run `./mvnw verify` before handing work
  over; the build fails on any violation.

## Javadoc

- Every class and every public method has Javadoc in standard style: a summary sentence, then `@param`,
  `@return` and `@throws` where they apply. Plain getters and setters are exempt.
- Comments explain why, not what. Do not restate the code.

## Architecture

Dependencies point in one direction only: `ui` → `service` → `core` and `storage` → `model`. The user
interface may call `core` and `storage` directly for calculations, formatting and simple reads and writes;
everything that creates or changes an invoice goes through `service`.

| Package            | Responsibility                                                        |
|--------------------|-----------------------------------------------------------------------|
| `einvoice`         | Entry point and `AppInfo` (product name, version); usable everywhere  |
| `einvoice.i18n`    | Languages and message lookup; may be used by every other package      |
| `einvoice.model`   | Plain data classes, no logic beyond simple derived values             |
| `einvoice.core`    | Invoice logic without I/O on user data: tax, XML, PDF, validation     |
| `einvoice.storage` | Reading and writing the data folder                                   |
| `einvoice.service` | Use cases that combine core and storage                               |
| `einvoice.ui`      | JavaFX user interface, no business logic                              |

- The invoice XML is the single source of truth. The visible PDF is rendered from it via the stylesheet
  (`template.xsl`); never compute amounts a second time for the layout.
- An invoice is only saved after it passed validation, and an existing file is never overwritten.

## Documentation and data

- Documentation, research notes and reports live under `docs/`.
- No real company, customer or bank data in the repository. Samples use fictitious data. User data lives
  in the data folder outside the repository.

- The product name and version are defined once, in `pom.xml`. Code reads them through `AppInfo`; never
  write the product name into code or resources.

## Quality gates

`./mvnw verify` runs all of the following and fails if one of them fails:

| Tool        | Checks                                                                       |
|-------------|------------------------------------------------------------------------------|
| JUnit       | Unit tests, invoice generation with validation, one test through the real UI |
| Checkstyle  | Code conventions and Javadoc on the public API (`config/checkstyle.xml`)     |
| Spotless    | Import order and whitespace; fix with `./mvnw spotless:apply`                |
| SpotBugs    | Likely bugs; accepted findings are listed in `config/spotbugs-exclude.xml`   |
| JaCoCo      | At least 80 % line coverage outside the user interface                       |
| Javadoc     | API documentation builds without warnings                                    |
| License     | List of bundled libraries and their licences; fails if a licence is missing  |
| CycloneDX   | Software bill of materials for every build                                   |

Two slower checks are not part of `verify`. They run weekly in `.github/workflows/quality.yml` and on
demand:

| Tool   | Checks                                                                 | Command                      |
|--------|------------------------------------------------------------------------|------------------------------|
| PIT    | Mutation tests: at least 70 % of the small code changes are noticed    | `./mvnw -Pmutation test`     |
| Grype  | Known vulnerabilities in the bundled libraries, read from the SBOM     | `grype sbom:target/bom.json` |

- Do not lower a threshold or add an exclusion to make a build pass; fix the code, or explain the
  exception in the configuration file.
- The same build runs on Linux, macOS and Windows in `.github/workflows/build.yml`.

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<optional scope>): <summary in imperative mood, lower case, no full stop>
```

- Types: `feat` (new feature), `fix` (bug fix), `docs`, `test`, `refactor`, `build`, `ci`, `chore`.
- The scope is optional. If one is given, it must be one of these:

  | Scope      | Covers                                                             |
  |------------|--------------------------------------------------------------------|
  | `core`     | Tax rules, input checks, XML generation, PDF rendering, validation |
  | `model`    | Data classes                                                       |
  | `storage`  | Data folder and settings of the installation                       |
  | `service`  | Use cases that combine core and storage                            |
  | `ui`       | JavaFX user interface                                              |
  | `i18n`     | Languages and translated texts                                     |
  | `template` | Invoice layout (`template.xsl`)                                    |
  | `site`     | Landing page                                                       |
  | `deps`     | Library and plugin updates                                         |
  | `release`  | Version, changelog and installers                                  |

  A change that spans several areas has no scope. A new scope is added to this table and to the check in
  `.github/workflows/build.yml` first.
- A breaking change gets `!` after the type or scope and a `BREAKING CHANGE:` footer.
- Examples: `feat(ui): add language switch`, `fix(core): render invoices one at a time`.
- The build workflow rejects pull requests whose commits do not follow this format or use another scope.

## Changelog

- Every change that users will notice gets a line under "Unreleased" in `CHANGELOG.md`, written for
  users, not for developers. The release workflow turns that section into the release notes.

## Tests

- Changes to invoice generation need a test that validates the generated file (PDF/A-3, schema and
  EN 16931 business rules) for every tax case.
- The app generates and validates with the same library. `IndependentRulesTest` therefore checks the XML
  against the official EN 16931 rule files as well; keep it passing for every tax case.
- Logic gets unit tests next to it; shared fixtures live in `TestData` and use fictitious data.
- `InvoiceImageTest` compares the rendered invoice with reference images under
  `src/test/resources/einvoice/reference`. Update them only for an intended layout change, and look at
  the new images before committing them.
- `RandomInvoiceRulesTest` uses a fixed seed. If it fails, the message names the invoice; turn that case
  into a normal test before fixing it.
