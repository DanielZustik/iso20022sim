# PRD: ISO 20022 Acquirer Simulator v1

Labels: `ready-for-agent`

## Problem Statement

Teams integrating Card Acceptor or Switch systems need a local **Acquirer Simulator** that behaves like an acquirer for ISO 20022 card authorisation messages. The current project is only a minimal Maven skeleton, so there is no working way to submit a schema-valid **Authorisation Request**, receive a schema-valid **Authorisation Response**, test approvals and declines, inspect recent submissions, or exercise invalid-request behavior before integrating with a real acquirer.

## Solution

Build a working local Spring Boot MVC **Acquirer Simulator**. The simulator exposes a canonical **Authorisation API** that accepts `caaa.001.001.15` XML and returns `caaa.002.001.15` XML for valid business approvals and declines. It also provides a **Manual Testing UI** with curated **Sample Requests**, response display, and an in-memory **Request Log**. The simulator uses strict official XSD validation, startup **Simulator Configuration** for **Approval Rules**, and deterministic behavior suitable for automated tests and manual integration experiments.

## User Stories

1. As a Switch developer, I want to POST a `caaa.001.001.15` **Authorisation Request**, so that I can test acquirer-facing integration without a real acquirer.
2. As a Card Acceptor developer, I want a schema-valid `caaa.002.001.15` **Authorisation Response**, so that my client code exercises the real ISO 20022 response shape.
3. As an integration tester, I want approved authorisations to return `200 OK`, so that successful business behavior is easy to automate.
4. As an integration tester, I want declined authorisations to return `200 OK`, so that a **Declined Authorisation** is treated as a valid business outcome.
5. As an integration tester, I want malformed XML to return `400 Bad Request`, so that transport parsing failures are distinct from business declines.
6. As an integration tester, I want schema-invalid XML to return `422 Unprocessable Entity`, so that **Invalid Request** behavior is distinct from **Declined Authorisation** behavior.
7. As an integration tester, I want unsupported ISO 20022 message versions to return `422 Unprocessable Entity`, so that version mismatches are explicit.
8. As a simulator operator, I want **Approval Rules** loaded from **Simulator Configuration**, so that I can change simulator behavior without editing Java code.
9. As a simulator operator, I want a configurable amount threshold decline rule, so that I can force high-value authorisations to decline.
10. As a simulator operator, I want a configurable denied card identifier rule, so that I can force specific cards to decline.
11. As a simulator operator, I want a configurable denied acceptor or merchant identifier rule, so that I can force specific acceptors to decline.
12. As a Switch developer, I want unmatched requests to approve by default, so that normal happy-path testing stays simple.
13. As a Card Acceptor developer, I want approved responses to include an **Approval Code**, so that my flow can verify approval data extraction.
14. As a tester, I want declined responses to include a reason tied to the matched **Approval Rule**, so that test failures are explainable.
15. As a manual tester, I want an HTML **Manual Testing UI**, so that I can submit XML without a separate HTTP client.
16. As a manual tester, I want to load a built-in approved **Sample Request**, so that I can quickly verify the happy path.
17. As a manual tester, I want to load a built-in amount-decline **Sample Request**, so that I can quickly verify threshold declines.
18. As a manual tester, I want to load a built-in denied-card **Sample Request**, so that I can quickly verify card denylist declines.
19. As a manual tester, I want to load a built-in denied-acceptor **Sample Request**, so that I can quickly verify acceptor denylist declines.
20. As a manual tester, I want to load an invalid XML or schema-invalid **Sample Request**, so that I can verify technical rejection behavior.
21. As a manual tester, I want the UI to show response status and body, so that I can inspect simulator behavior immediately.
22. As a simulator operator, I want an in-memory **Request Log**, so that I can inspect recent submissions without a database.
23. As a simulator operator, I want the **Request Log** to show timestamps, extracted request identifiers where available, selected business fields, decision, matched rule, and response summary, so that debugging is practical.
24. As a developer, I want official XSDs committed into the repository, so that builds and tests do not depend on network downloads.
25. As a developer, I want response XML validated against the `caaa.002.001.15` XSD, so that the simulator does not emit hand-built invalid XML.
26. As a developer, I want request parsing and decision logic separated, so that ISO validation complexity does not leak into **Approval Rule** tests.
27. As a developer, I want the **Manual Testing UI** to drive the same **Authorisation API** behavior, so that manual and automated tests exercise the same simulator boundary.
28. As a future maintainer, I want version 1 to exclude completion, capture, cancellation, reversal, reconciliation, and batch exchange, so that the first slice remains focused.

## Implementation Decisions

- Build the application as a Spring Boot MVC service on Java 17.
- Use a raw XML HTTP **Authorisation API** as the canonical integration boundary.
- Use a server-rendered **Manual Testing UI**, likely Thymeleaf or equivalent, for manual request submission and inspection.
- Support only `caaa.001.001.15` **Authorisation Request** and `caaa.002.001.15` **Authorisation Response** in version 1.
- Enforce strict XSD validation using official ISO 20022 XSD resources committed to the repository.
- Do not use toy XML, simplified ISO-like XML, or hand-built response strings.
- Business approval and decline outcomes return schema-valid `caaa.002.001.15`.
- Malformed XML, schema-invalid XML, and unsupported message versions are **Invalid Requests**, not **Declined Authorisations**.
- Use startup **Simulator Configuration** for **Approval Rules** and default decision.
- Keep rule editing out of the **Manual Testing UI** for version 1.
- Implement a deep ISO Message Boundary module responsible for parsing, schema validation, unsupported-version detection, field extraction needed by rules, and response rendering.
- Implement a deep Authorisation Decision Engine module responsible for applying ordered **Approval Rules** and returning an **Authorisation Decision**.
- Implement an Authorisation API module responsible for HTTP status mapping and XML request/response exchange.
- Implement a Manual Testing UI module responsible for loading **Sample Requests**, submitting XML, displaying responses, and showing the **Request Log**.
- Implement a Request Log module as in-memory recent-submission storage only, not durable transaction storage.
- Implement a Sample Request Catalogue containing approved, amount-decline, denied-card, denied-acceptor, and invalid request examples.
- Respect ADR 0001: strict ISO 20022 XSD validation with committed XSD resources.
- Respect ADR 0002: Spring Boot MVC with server-rendered UI.

## Testing Decisions

- Tests should verify externally observable behavior: HTTP statuses, XML validity, decisions, response content, and request-log behavior. They should not assert private implementation details or generated binding internals.
- Test the ISO Message Boundary with schema-valid `caaa.001.001.15`, malformed XML, schema-invalid XML, and unsupported message/version inputs.
- Test response generation by validating emitted `caaa.002.001.15` XML against the official XSD.
- Test the Authorisation Decision Engine with default approval, amount threshold decline, denied card decline, denied acceptor decline, and rule ordering.
- Test the Authorisation API with approved `200 OK`, declined `200 OK`, malformed `400 Bad Request`, schema-invalid `422 Unprocessable Entity`, and unsupported version `422 Unprocessable Entity`.
- Test that **Invalid Requests** do not produce **Authorisation Responses**.
- Test that valid approved requests produce an **Approval Code**.
- Test that valid declined requests include a decision reason tied to the matched **Approval Rule**.
- Test the Sample Request Catalogue by ensuring each curated valid sample exercises its intended simulator decision.
- Test the Request Log through public service/API behavior: recent valid submissions are recorded with useful inspection fields, and invalid submissions are represented consistently if the UI needs to display them.
- Use a smoke-level test for the **Manual Testing UI** to ensure it renders, exposes sample choices, submits to the simulator path, and displays response/log data.
- There is little prior test structure in this Maven skeleton, so the first implementation should establish Spring Boot test conventions for controller/API tests and isolated unit tests for decision logic.

## Out of Scope

- Sending authorisation requests as a **Card Acceptor** or **Switch**.
- Acting as an issuer, clearing system, beneficiary bank, or customer payment service provider.
- `pain.*`, `pacs.*`, or non-card ISO 20022 messages.
- Completion, capture, cancellation, reversal, reconciliation, settlement, clearing, account posting, and batch exchange.
- Multi-version `caaa` support.
- Persistent database storage.
- Durable audit logging.
- Editing **Approval Rules** from the **Manual Testing UI**.
- SOAP transport unless a future real integration requires it.
- Frontend SPA or separate JavaScript build system.
- Production security, authentication, authorization, and multi-user operation.

## Further Notes

- The exact source package for official ISO 20022 XSDs still needs to be selected before implementation.
- The exact `caaa.002.001.15` fields and codes for approvals and each decline reason need to be mapped from the official schema/message documentation.
- The exact `caaa.001.001.15` fields used for card identifier and acceptor or merchant identifier rules need to be selected from the schema.
- The first implementation should prefer narrow, testable modules so future support for additional `caaa` messages can be added without rewriting the **Authorisation API** or **Manual Testing UI**.
