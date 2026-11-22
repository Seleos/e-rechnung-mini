# Changelog

All notable changes to this project are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

Add every user-visible change to the "Unreleased" section. The release workflow turns that section into
the notes of the new version.

## [Unreleased]

## [0.10.0] - 2026-11-22

### Added

- Command line for recurring invoices, with commands in the form `<entity> <action>`: `invoice create` copies
  an earlier invoice (`--from`) or starts from a customer's pre-filled services (`--customer`), with the next
  number, the previous month as service period and the same validation as in the window. `invoice list`,
  `customer list` and `company list` show what is available, `--dry-run` shows an invoice without saving it.

### Changed

- The invoice footer has five columns, as in a classic business letter: company, managing director, bank,
  phone/e-mail/website and register court/number with VAT ID. The phone number is now printed there too.
- The invoice list can be sorted by every column: number, date, customer, net and gross amount. A double click on
  an invoice opens its PDF.

### Fixed

- SVG logos without their own width and height were drawn far too small; they now keep their size.
- The PDF properties named the producer as "E-Rechnung Mini (via mustangproject.org ${project.version})"; they now
  show the app with its version, the same in all metadata of the file.

## [0.9.1] - 2026-11-17

### Changed

- Updated the user interface theme (AtlantaFX 3) and the build tools.

## [0.9.0] - 2026-11-17

### Added

- Create ZUGFeRD invoices (EN 16931) as PDF/A-3 with embedded XML, with a live preview.
- Validation of every invoice before it is saved: PDF/A-3, XML schema and EN 16931 business rules.
- Several issuing companies, each with logo, footer data, number range and output folder.
- Customers with VAT ID, tax case and their own services and rates.
- Tax cases: domestic 19 % and 7 %, EU reverse charge, third country, small business (§ 19 UStG).
- Invoice layout as an XSLT stylesheet that can be replaced per company. The standard layout follows the
  German business letter standard DIN 5008 (form B) with fold marks.
- List of created invoices with "use as template".
- Invoice language German or English, set per customer, including dates and the mandatory tax notes.
- Currency per customer, euro by default. Invoices with VAT stay in euro.
- E-mail subject and text per customer with placeholders such as month, year and a signature block,
  copied to the clipboard after an invoice is created.
- Placeholders for dates, month and year in line item texts, the text below the invoice and the e-mail
  template.
- Services can be marked as pre-filled, so a new invoice only needs the quantities.
- Logo preview in the company settings.
- User interface in German and English.
- Installers for macOS and Windows with an app icon.
