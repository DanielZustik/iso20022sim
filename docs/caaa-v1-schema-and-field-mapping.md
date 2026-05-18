# ISO 20022 `caaa` V1 Schema and Field Mapping Decisions

This document captures the schema-source and field-mapping decisions for version 1 of the **Acquirer Simulator**.

## Official source and package

The official source is the ISO 20022 message catalogue page for **Acceptor to Acquirer Card Transactions**:

- https://www.iso20022.org/iso-20022-message-definitions

For reproducible local work, use these official downloads from that catalogue:

- Business-area package (ZIP): `https://www.iso20022.org/business-area/41/download`
- Request XSD (`caaa.001.001.15`): `https://www.iso20022.org/message/23442/download`
- Response XSD (`caaa.002.001.15`): `https://www.iso20022.org/message/23443/download`
- Message Definition Reports (MDR ZIP): `https://www.iso20022.org/message/mdr/23442/download` and `https://www.iso20022.org/message/mdr/23443/download`

Observed content-disposition filenames on 2026-05-18:

- `caaa.001.001.15.xsd`
- `caaa.002.001.15.xsd`
- `archive_business_area_acceptor_to_acquirer_card_transactions_904714f224.zip`
- `archive__8d54cecf56.zip` (MDR bundle)

## Request-field mapping for Approval Rules (`caaa.001.001.15`)

Root path:

- `/Document/AccptrAuthstnReq/AuthstnReq`

Version 1 rule inputs:

1. Amount rule input
- XPath: `/Document/AccptrAuthstnReq/AuthstnReq/Tx/TxDtls/TtlAmt`
- Type: `ImpliedCurrencyAndAmount` (decimal, 5 fraction digits, total digits 18, minInclusive 0)
- Currency context: `/Document/AccptrAuthstnReq/AuthstnReq/Tx/TxDtls/Ccy` (`ActiveCurrencyCode`, optional)

2. Card identifier rule input
- Primary XPath: `/Document/AccptrAuthstnReq/AuthstnReq/Envt/Card/PlainCardData/PAN`
- Type: `Min8Max28NumericText`
- Fallback (when PAN is not present in clear text): `/Document/AccptrAuthstnReq/AuthstnReq/Envt/Card/MskdPAN`

3. Acceptor or merchant identifier rule input
- Merchant identifier XPath: `/Document/AccptrAuthstnReq/AuthstnReq/Envt/Mrchnt/Id/Id`
- Merchant identifier type chain: `Organisation45 -> GenericIdentification192 -> Id (Max35Text)`
- Acceptor (POI) identifier XPath: `/Document/AccptrAuthstnReq/AuthstnReq/Envt/POI/Id/Id`
- Acceptor identifier type chain: `PointOfInteraction16 -> GenericIdentification177 -> Id (Max35Text)`

## Response-field mapping for authorisation outcomes (`caaa.002.001.15`)

Root path:

- `/Document/AccptrAuthstnRspn/AuthstnRspn`

Authorisation decision mapping:

1. Decision code
- XPath: `/Document/AccptrAuthstnRspn/AuthstnRspn/TxRspn/AuthstnRslt/RspnToAuthstn/Rspn`
- Type: `Response9Code`
- Supported values in schema: `APPR`, `DECL`, `PART`, `SUSP`, `TECH`
- V1 mapping decision:
  - Approved Authorisation: `APPR`
  - Declined Authorisation (rule-based): `DECL`

2. Approval code
- XPath: `/Document/AccptrAuthstnRspn/AuthstnRspn/TxRspn/AuthstnRslt/AuthstnCd`
- Type: `Max8Text`
- V1 mapping decision: populate for approved responses.

3. Rule-based decline reason
- Primary reason XPath: `/Document/AccptrAuthstnRspn/AuthstnRspn/TxRspn/AuthstnRslt/RspnToAuthstn/RspnRsn`
- Additional detail XPath: `/Document/AccptrAuthstnRspn/AuthstnRspn/TxRspn/AuthstnRslt/RspnToAuthstn/AddtlRspnInf`
- Types: `Max35Text` and `Max140Text`
- V1 mapping decision: include deterministic matched-rule identifiers/messages in these fields for declined responses.

## Assumptions and limitations

1. `TtlAmt` is used as the amount-rule source for version 1. Other amount elements (for example `ReqdAmt` or detailed breakdown fields) are out of scope for v1 rule evaluation.
2. Card identifier extraction may require fallback handling because clear PAN may be absent in some valid inputs.
3. The schema allows several response decision codes (`PART`, `SUSP`, `TECH`) that are not used by v1 business rules.
4. `RspnRsn` is free text (`Max35Text`) rather than an enum in this message version; therefore simulator decline-reason normalization is an implementation convention.

## ADR consistency

These decisions remain consistent with ADR 0001 (`docs/adr/0001-use-strict-iso-20022-xsd-validation.md`):

- source artefacts are official ISO 20022 XSD and MDR downloads;
- request and response handling is defined against schema-valid `caaa.001.001.15` and `caaa.002.001.15`;
- field-level rule decisions are documented without introducing toy XML shortcuts.
