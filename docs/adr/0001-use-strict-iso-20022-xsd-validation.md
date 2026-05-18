# Use Strict ISO 20022 XSD Validation

The simulator will accept and emit schema-valid ISO 20022 card authorisation messages for the supported `caaa` versions, rather than a simplified XML subset. This makes the app heavier to build, but it preserves the integration value of the simulator: clients exercise the real message structure they will use with an acquirer. Business approvals and declines are returned as schema-valid `caaa.002` responses, while malformed or schema-invalid requests are HTTP errors rather than declined authorisations. The required official XSD files will be committed to the repository so validation and code generation are reproducible without network access.

## Considered Options

- Full XSD validation for supported `caaa` messages.
- A pragmatic subset using real message names and namespaces.
- A custom toy XML format.
- Build-time XSD download versus committed XSD resources.

## Consequences

The implementation should be based on official XSDs and generated or schema-aware XML handling. Decline rules can still inspect only selected business fields, but the inbound and outbound documents must remain valid for the selected message versions.
