# ISO 20022 Card Acceptor Simulator

This context defines the language for a simple ISO 20022 card payments simulator. The simulator focuses on card acceptor or switch communication with an acquirer using acceptor-to-acquirer card transaction messages.

## Language

**Card Acceptor**:
A merchant-side party or terminal environment that initiates card transaction messages toward an acquirer.
_Avoid_: Customer, debtor, payment initiator

**Switch**:
An intermediary system that routes card acceptor transaction messages toward an acquirer.
_Avoid_: Clearing system, counterparty bank

**Acquirer**:
The card transaction party that receives acceptor-to-acquirer messages and returns authorisation outcomes.
_Avoid_: Beneficiary bank, PSP, issuer

**Acquirer Simulator**:
A test double for an acquirer that receives acceptor authorisation requests and returns authorisation responses.
_Avoid_: Card acceptor simulator, switch simulator

**Authorisation Request**:
A `caaa.001.001.15` message asking the acquirer to authorise a card transaction.
_Avoid_: Payment order, transfer request

**Authorisation Response**:
A `caaa.002.001.15` message returning the acquirer's approval or decline decision for an authorisation request.
_Avoid_: Payment status report, settlement response

**Schema-Valid ISO Message**:
An ISO 20022 XML message that validates against the official XSD for its message version.
_Avoid_: Toy XML, ISO-like XML, partial envelope

**Authorisation API**:
The canonical HTTP interface that receives an authorisation request XML document and returns an authorisation response XML document.
_Avoid_: HTML form, file upload UI

**Manual Testing UI**:
An HTML interface for submitting sample authorisation requests and inspecting generated responses.
_Avoid_: Integration API, canonical simulator protocol

**Invalid Request**:
A submitted XML document that cannot be processed as a schema-valid supported authorisation request.
_Avoid_: Declined transaction

**Declined Authorisation**:
A valid authorisation request that the acquirer refuses according to simulator rules.
_Avoid_: Invalid request, validation error

**Approval Rule**:
A deterministic simulator rule that decides whether a valid authorisation request is approved or declined.
_Avoid_: Validation rule, schema rule

**Simulator Configuration**:
Startup configuration that defines the simulator's approval rules and default decision.
_Avoid_: UI-edited rule state, Java-only hardcoded rules

**Authorisation Decision**:
The simulator's internal approval or decline outcome for a valid authorisation request.
_Avoid_: HTTP status, XML validation result

**Approval Code**:
A generated code included in an approved authorisation response.
_Avoid_: Request ID, transaction ID

**Request Log**:
An in-memory record of recent authorisation submissions and their simulator outcomes for manual inspection.
_Avoid_: Audit log, transaction store, settlement history

**Sample Request**:
A curated authorisation request example used by the manual UI and automated tests to demonstrate simulator behavior.
_Avoid_: Production fixture, generated random request

## Relationships

- A **Card Acceptor** or **Switch** sends card transaction messages to an **Acquirer**.
- An **Acquirer** returns authorisation outcomes to the **Card Acceptor** or **Switch**.
- The app is an **Acquirer Simulator**, not a sender-side **Card Acceptor** or **Switch** simulator.
- Version 1 supports **Authorisation Request** `caaa.001.001.15` and **Authorisation Response** `caaa.002.001.15` only.
- Version 1 accepts and emits **Schema-Valid ISO Messages** rather than a pragmatic XML subset.
- The **Authorisation API** is the canonical simulator boundary.
- The **Manual Testing UI** exists to drive and inspect the **Authorisation API**, not to define a separate simulator protocol.
- An **Invalid Request** is rejected before authorisation logic and is not a **Declined Authorisation**.
- A **Declined Authorisation** is produced only from a valid **Authorisation Request**.
- Version 1 uses ordered **Approval Rules** with default approval when no decline rule matches.
- **Approval Rules** are defined in **Simulator Configuration** and loaded at application startup.
- Each valid **Authorisation Request** produces one **Authorisation Decision**.
- Each **Authorisation Decision** is rendered as a schema-valid **Authorisation Response**.
- An approved **Authorisation Decision** includes an **Approval Code**.
- An **Invalid Request** returns an HTTP error rather than an **Authorisation Response**.
- The **Request Log** is for debugging and manual inspection, not durable transaction storage.
- **Sample Requests** demonstrate approval, rule-based declines, and invalid request handling.
- The first version does not model customer credit transfers, clearing, settlement, or account posting.
- Version 1 does not model completion, capture, cancellation, reversal, reconciliation, or batch exchange.

## Example Dialogue

> **Dev:** "Is this a `pain.001` customer payment submission?"
> **Domain expert:** "No. This is **Card Acceptor** or **Switch** communication with an **Acquirer**, starting with acceptor authorisation messages."
>
> **Dev:** "Should the app send authorisation requests?"
> **Domain expert:** "No. The app is the **Acquirer Simulator**. It receives requests and returns responses."
>
> **Dev:** "Should we implement completion advice now?"
> **Domain expert:** "No. Version 1 only handles an **Authorisation Request** and returns an **Authorisation Response**."
>
> **Dev:** "Can we use a simplified XML shape for the request?"
> **Domain expert:** "No. The simulator should accept **Schema-Valid ISO Messages** for the supported message versions."
>
> **Dev:** "Is the HTML page the integration surface?"
> **Domain expert:** "No. The **Authorisation API** is the integration surface; the **Manual Testing UI** is only for convenient manual tests."
>
> **Dev:** "If the XML is malformed, should we return a declined authorisation?"
> **Domain expert:** "No. That is an **Invalid Request**. A **Declined Authorisation** only happens after the request is valid."
>
> **Dev:** "Can users change decline rules from the HTML page?"
> **Domain expert:** "No. **Approval Rules** come from **Simulator Configuration** loaded at startup."
>
> **Dev:** "Does every XML submission return `caaa.002`?"
> **Domain expert:** "No. Only a valid **Authorisation Request** produces an **Authorisation Response**. An **Invalid Request** returns an HTTP error."
>
> **Dev:** "Can we use the request history as an audit trail?"
> **Domain expert:** "No. The **Request Log** is in-memory and only supports manual inspection in version 1."
>
> **Dev:** "Are sample XML files throwaway demo data?"
> **Domain expert:** "No. **Sample Requests** document expected simulator behavior and should also back automated tests."

## Approval Rules

- Decline when the amount exceeds the configured threshold.
- Decline when the card identifier matches a configured denylist.
- Decline when the acceptor or merchant identifier matches a configured denylist.
- Approve when no decline rule matches.

## HTTP Contract

- Approved authorisations return `200 OK` with a schema-valid **Authorisation Response**.
- Declined authorisations return `200 OK` with a schema-valid **Authorisation Response**.
- Malformed XML returns `400 Bad Request`.
- Schema-invalid XML returns `422 Unprocessable Entity`.
- Unsupported ISO 20022 message versions return `422 Unprocessable Entity`.

## Flagged Ambiguities

- "ISO 20022 transaction" was initially ambiguous between customer credit transfers and card payments; resolved: this project is card acceptor-to-acquirer communication.
- "simulator" was resolved to mean **Acquirer Simulator**, not a sender-side **Card Acceptor** or **Switch** simulator.
- "simple XML" was resolved to mean full XSD-valid ISO 20022 XML for the supported messages, not a hand-made simplified schema.
