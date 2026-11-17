# E-Rechnung Mini

[![Build](https://github.com/Seleos/e-rechnung-mini/actions/workflows/build.yml/badge.svg)](https://github.com/Seleos/e-rechnung-mini/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Seleos/e-rechnung-mini)](https://github.com/Seleos/e-rechnung-mini/releases/latest)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Small desktop app for writing German e-invoices in the ZUGFeRD format (EN 16931). Pick the issuing
company and the customer, add line items, and the app produces a validated PDF/A-3 with the invoice XML
embedded. Runs locally on macOS and Windows, without a server or an account.

## Features

- Several issuing companies, each with its own logo, footer data, number range and output folder
- Customers with VAT ID and tax case: domestic 19 % / 7 %, EU reverse charge, third country
- Services with individual rates per customer, inserted into the invoice with one click
- Invoice language German or English and currency, set per customer
- Placeholders such as `{month}`, `{year}` or `{lastDayOfLastMonth}` in line item texts, the text below the
  invoice and the e-mail template
- New invoices start pre-filled: date, service period, number and the customer's default services
- E-mail subject and text per customer with placeholders, ready to copy into your mail program
- Small business option (§ 19 UStG, "Kleinunternehmer")
- Live preview of the invoice while typing
- Every invoice is validated (PDF/A-3, XML schema, EN 16931 business rules) before it is saved
- Layout after the German business letter standard DIN 5008, as an XSLT stylesheet that can be replaced
  per company
- Data stored as readable JSON files in a folder of your choice
- User interface in German and English, switchable in the settings. Further languages are one file each

Not included: sending e-mails, time tracker import, cancellation invoices and saving drafts.

## Run

Requires JDK 25 or newer.

```
./mvnw compile exec:java
```

On first start the app creates a fictitious example company in `Documents/E-Rechnung Mini`. The data
folder can be changed under Einstellungen › Ordner.

## Build, test, lint

```
./mvnw verify
```

One command runs every quality check and fails if any of them fails:

- unit tests, invoice generation for every tax case with validation, and one test through the real UI
- a second, independent check of the invoice XML against the official EN 16931 rule files
- Checkstyle (code conventions, Javadoc), Spotless (formatting) and SpotBugs (likely bugs)
- test coverage of at least 80 % outside the user interface (report in `target/site/jacoco`)
- API documentation without warnings (`target/reports/apidocs`)
- a list of all bundled libraries and their licences (`target/generated-sources/license`)
- a software bill of materials in CycloneDX format (`target/bom.json`)

Three more checks are part of that build since they are fast: 300 random invoices against the official
rules, a comparison of the rendered invoice with reference images, and the DIN 5008 positions.

Slower checks run on demand and once a week on GitHub:

```
./mvnw -Pmutation test                      # mutation tests, report in target/pit-reports
./mvnw -DskipTests package && grype sbom:target/bom.json   # known vulnerabilities, needs grype installed
```

After an intended layout change, accept the new reference images with
`./mvnw test -Dtest=InvoiceImageTest -Deinvoice.updateReferences=true`.

`./mvnw spotless:apply` fixes formatting findings. GitHub Actions runs the same build on Linux, macOS and
Windows, and Dependabot proposes library updates.

## Installers

```
./mvnw -Pdist -DskipTests package
```

This builds an installer for the operating system it runs on into `target/dist`: a `.dmg` on macOS, an
`.msi` on Windows, a `.deb` on Linux. Each bundles its own Java runtime, so users need nothing else.
On Windows the build needs the WiX Toolset.

The icon is drawn in `src/main/packaging/icon.svg`. The `.icns`, `.ico` and `.png` files next to it and
the icons in `src/main/resources/einvoice` are generated from it and have to be regenerated after a change.

## Releasing a version

Start the workflow "Release" on GitHub (Actions › Release › Run workflow) and enter the version, for
example `1.0.0`. It writes the version into `pom.xml`, runs all checks, turns the "Unreleased" section of
the changelog into the release notes, tags the commit, builds the installers for macOS (Apple silicon and
Intel) and Windows and publishes them with checksums and a bill of materials as a GitHub release.

If an installer job fails, fix the cause and start the workflow again with the same version and
"installers_only" ticked. That rebuilds the installers from the existing tag without touching the version.
The installers are built with JDK 27, because the packaging tool of older JDKs refuses macOS versions that
start with 0.

The version is defined in `pom.xml` only. The app and the installer file names read it from there.

The installers are not signed yet. Until they are, macOS and Windows warn on first start.

## How it works

1. The invoice data is turned into the invoice XML (CII) with the Mustang library.
2. The stylesheet `template.xsl` transforms that XML into a page layout, which Apache FOP renders as PDF/A-3.
3. The same XML is embedded into the PDF and the file is validated.

The visible page is always rendered from the XML, so both parts cannot differ.

## Adding a language

1. Copy `src/main/resources/einvoice/messages_en.properties` to `messages_<code>.properties` and translate it.
2. Do the same with `invoice_en.properties`, which holds the texts printed on the invoice.
3. Add a constant to `einvoice.i18n.Language`.


## Data folder

```
companies/<id>/company.json      company master data
companies/<id>/logo.*            logo
companies/<id>/template.xsl      optional own layout
companies/<id>/customers/*.json  customers with their services and rates
companies/<id>/records/*.json    data of created invoices
companies/<id>/invoices/<year>/  default output folder
```

## Landing page

The page in `site/` is published to GitHub Pages by `.github/workflows/pages.yml`. It still contains
placeholders in square brackets (repository link, contact address, legal notice links).

## Contributing

- [CONTRIBUTING.md](CONTRIBUTING.md) explains how to report problems and propose changes.
- [AGENTS.md](AGENTS.md) holds the rules for language, code style, architecture and tests.
- [CHANGELOG.md](CHANGELOG.md) lists what changed in each version.
- [SECURITY.md](SECURITY.md) explains how to report a vulnerability privately.
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) applies to everyone taking part.

## Disclaimer

This software is not tax or legal advice and comes without warranty. You are responsible for the
correctness of your invoices and for archiving them.

## License

Apache License 2.0, see [LICENSE](LICENSE). Bundled fonts: Liberation Sans (SIL Open Font License).
