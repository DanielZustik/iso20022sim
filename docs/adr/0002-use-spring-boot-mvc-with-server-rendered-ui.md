# Use Spring Boot MVC with Server-Rendered UI

The simulator will be a Spring Boot MVC application with a raw XML HTTP API and a server-rendered HTML manual testing UI. This keeps the simulator deployable as one Java service, avoids frontend build tooling, and matches the need for a simple operator-facing page beside the canonical XML integration endpoint.

## Consequences

The implementation should use Spring MVC controllers for the XML API and Thymeleaf or equivalent server-side templates for the manual UI. ISO 20022 XML handling should use XSD validation and generated or schema-aware Java bindings rather than hand-built XML strings.
