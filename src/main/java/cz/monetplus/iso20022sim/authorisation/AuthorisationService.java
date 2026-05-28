package cz.monetplus.iso20022sim.authorisation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@Service
public class AuthorisationService {

    private static final String REQUEST_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15";
    private static final String RESPONSE_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.002.001.15";
    private static final String ISO_20022_NAMESPACE_PREFIX = "urn:iso:std:iso:20022:tech:xsd:";
    private static final String RULE_AMOUNT_THRESHOLD = "AMOUNT_THRESHOLD";
    private static final String RULE_DENIED_CARD_IDENTIFIER = "DENIED_CARD_IDENTIFIER";
    private static final String RULE_DENIED_ACCEPTOR_OR_MERCHANT = "DENIED_ACCEPTOR_OR_MERCHANT_ID";
    private static final String RULE_DEFAULT_DECLINE = "DEFAULT_DECISION_DECLINE";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Schema requestSchema; // zabundlovane ze spring-web depen.
    private final Schema responseSchema;
    private final SimulatorConfigurationProperties config;
    private final Set<String> deniedCardIdentifiers;
    private final Set<String> deniedMerchantIdentifiers;
    private final Set<String> deniedAcceptorIdentifiers;

    public AuthorisationService(SimulatorConfigurationProperties config) {
        this.config = config;
        this.requestSchema = loadSchema("xsd/caaa.001.001.15.xsd");
        this.responseSchema = loadSchema("xsd/caaa.002.001.15.xsd");
        this.deniedCardIdentifiers = normaliseIdentifiers(config.getRules().getDeniedCardIdentifiers()); //z configu muze prijit ledasco
        this.deniedMerchantIdentifiers = normaliseIdentifiers(config.getRules().getDeniedMerchantIdentifiers());
        this.deniedAcceptorIdentifiers = normaliseIdentifiers(config.getRules().getDeniedAcceptorIdentifiers());
    }

    public String approveAuthorisation(String requestXml) {
        Document requestDocument = parseXml(requestXml); // well formatted xml
        ensureSupportedRequestNamespace(requestDocument);
        validateXml(requestXml, requestSchema, "Authorisation request is not schema-valid caaa.001.001.15 XML"); // valid xml

        RequestProjection requestProjection = RequestProjection.from(requestDocument);
        AuthorisationDecision decision = evaluateDecision(requestProjection);
        String responseXml = buildResponse(requestProjection, decision);

        validateXml(responseXml, responseSchema, "Generated authorisation response is not schema-valid caaa.002.001.15 XML");
        return responseXml;
    }

    private AuthorisationDecision evaluateDecision(RequestProjection requestProjection) {
        BigDecimal amountThreshold = config.getRules().getAmountThreshold();
        if (amountThreshold != null) {
            BigDecimal totalAmount = parseAmount(requestProjection.totalAmount());
            if (totalAmount.compareTo(amountThreshold) > 0) {
                return AuthorisationDecision.decline(
                        RULE_AMOUNT_THRESHOLD,
                        "Declined by amount threshold rule"
                );
            }
        }

        if (requestProjection.cardIdentifier() != null
                && deniedCardIdentifiers.contains(requestProjection.cardIdentifier())) {
            return AuthorisationDecision.decline(
                    RULE_DENIED_CARD_IDENTIFIER,
                    "Declined by denied card identifier rule"
            );
        }

        if ((requestProjection.merchantIdentifier() != null
                && deniedMerchantIdentifiers.contains(requestProjection.merchantIdentifier()))
                || (requestProjection.acceptorIdentifier() != null
                && deniedAcceptorIdentifiers.contains(requestProjection.acceptorIdentifier()))) {
            return AuthorisationDecision.decline(
                    RULE_DENIED_ACCEPTOR_OR_MERCHANT,
                    "Declined by denied acceptor or merchant identifier rule"
            );
        }

        if (config.getDefaultDecision() == SimulatorConfigurationProperties.DefaultDecision.DECLINE) {
            return AuthorisationDecision.decline(
                    RULE_DEFAULT_DECLINE,
                    "Declined by configured default decision"
            );
        }

        return AuthorisationDecision.approve(generateApprovalCode());
    }

    private BigDecimal parseAmount(String amountText) {
        try {
            return new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            throw new SchemaValidationException("Invalid amount value in request: Tx/TxDtls/TtlAmt", e);
        }
    }

    private Schema loadSchema(String resourcePath) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);// defakto jen 2 language
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) { //maven / spring pri buildeni nahodi automaticky soubory v "resources" do classpath a timpadem pak lze je najit pod "xsd/caaa.001.001.15.xsd"
            if (inputStream == null) {
                throw new IllegalStateException("Missing schema resource: " + resourcePath);
            }
            return schemaFactory.newSchema(new StreamSource(inputStream)); // v XSD standardu napsany soubor vytvori inMemory validacni objekt - predpis (threadSafe, immutable)
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Failed to load schema: " + resourcePath, e);
        }
    }

    private void validateXml(String xml, Schema schema, String message) {
        try {
            schema.newValidator().validate(new StreamSource(new StringReader(xml))); //vytvoreni per run worker ktery nacte schemu a provadi samotny check oproti prijatemu textu
        } catch (SAXException e) {
            throw new SchemaValidationException(message + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to validate XML", e);
        }
    }

    private Document parseXml(String xml) { //prichazi xml min. akceptovatelne <x/>
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance(); //factory pro parsery a jeji nastaveni nize
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");// outDated
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // delame interne

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder(); //parser
            return documentBuilder.parse(new org.xml.sax.InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) { //pr. SAXexcp. chybi closing tag
            throw new MalformedXmlException("Request body is not well-formed XML", e); //konverze do me excp.
        }
    }

    private void ensureSupportedRequestNamespace(Document requestDocument) {
        String namespace = requestDocument.getDocumentElement().getNamespaceURI();
        if (namespace == null || namespace.isBlank()) {
            return;
        }
        if (!REQUEST_NS.equals(namespace) && namespace.startsWith(ISO_20022_NAMESPACE_PREFIX)) {
            throw new SchemaValidationException(
                    "Unsupported ISO 20022 message/version namespace: " + namespace
                            + ". Supported Authorisation Request namespace is " + REQUEST_NS,
                    null
            );
        }
    }

    private String buildResponse(RequestProjection requestProjection, AuthorisationDecision decision) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document responseDocument = documentBuilder.newDocument(); // pouze on ma fci tvorit elementy

            Element documentElement = responseDocument.createElementNS(RESPONSE_NS, "Document");
            responseDocument.appendChild(documentElement);

            Element wrapperElement = appendElement(responseDocument, documentElement, "AccptrAuthstnRspn");

            Element headerElement = appendElement(responseDocument, wrapperElement, "Hdr");
            appendTextElement(responseDocument, headerElement, "MsgFctn", "AUTP");
            appendTextElement(responseDocument, headerElement, "PrtcolVrsn", requestProjection.protocolVersion());
            appendTextElement(responseDocument, headerElement, "XchgId", requestProjection.exchangeId());
            appendTextElement(responseDocument, headerElement, "CreDtTm", OffsetDateTime.now(ZoneOffset.UTC).withNano(0).toString());

            Element initiatingPartyElement = appendElement(responseDocument, headerElement, "InitgPty");
            appendTextElement(responseDocument, initiatingPartyElement, "Id", "ACQUIRER_SIM");

            Element authorisationResponseElement = appendElement(responseDocument, wrapperElement, "AuthstnRspn");
            appendElement(responseDocument, authorisationResponseElement, "Envt");

            Element txElement = appendElement(responseDocument, authorisationResponseElement, "Tx");
            Element txIdElement = appendElement(responseDocument, txElement, "TxId");
            appendTextElement(responseDocument, txIdElement, "TxDtTm", requestProjection.transactionDateTime());
            appendTextElement(responseDocument, txIdElement, "TxRef", requestProjection.transactionReference());
            Element txDetailsElement = appendElement(responseDocument, txElement, "TxDtls");
            appendTextElement(responseDocument, txDetailsElement, "TtlAmt", requestProjection.totalAmount());

            Element txResponseElement = appendElement(responseDocument, authorisationResponseElement, "TxRspn");
            Element authorisationResultElement = appendElement(responseDocument, txResponseElement, "AuthstnRslt");
            Element responseToAuthorisationElement = appendElement(responseDocument, authorisationResultElement, "RspnToAuthstn");
            appendTextElement(responseDocument, responseToAuthorisationElement, "Rspn", decision.responseCode());

            if (decision.responseReason() != null) {
                appendTextElement(responseDocument, responseToAuthorisationElement, "RspnRsn", decision.responseReason());
            }
            if (decision.additionalResponseInfo() != null) {
                appendTextElement(responseDocument, responseToAuthorisationElement, "AddtlRspnInf", decision.additionalResponseInfo());
            }
            if (decision.approvalCode() != null) {
                appendTextElement(responseDocument, authorisationResultElement, "AuthstnCd", decision.approvalCode());
            }

            return toXml(responseDocument);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to build response document", e);
        }
    }

    private static Element appendElement(Document document, Element parent, String localName) {
        Element element = document.createElementNS(RESPONSE_NS, localName);
        parent.appendChild(element);
        return element;
    }

    private static void appendTextElement(Document document, Element parent, String localName, String value) {
        Element element = appendElement(document, parent, localName);
        element.setTextContent(value);
    }

    private String toXml(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer(); // bez anstavovani stylesheetu v ramci XSLT.. tedy jen cista serializace
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // hralo by roli kdyby se nepouzil StringWritter ale OutputStrem tj. ciste bajty... funkci ma jen v tom, ze uveden do "headeru" xml UTF kodovani
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // prettyPrint
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Failed to serialize response XML", e);
        }
    }

    private String generateApprovalCode() {
        int value = RANDOM.nextInt(1000000, 100000000);
        return String.valueOf(value);
    }

    private static Set<String> normaliseIdentifiers(List<String> identifiers) {
        if (identifiers == null) { //Default hodnota je nonNull, ale config muze null nastavit
            return Set.of();
        }
        return identifiers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet()); //good practice aby se runtime nahodou neprepsal resp. startUp config zustal as is forever
    }

    private static String requiredPathText(Document document, String fieldLabel, String... path) {
        String value = optionalPathText(document, path);
        if (value == null) {
            throw new SchemaValidationException("Missing required field in request: " + fieldLabel, null);
        }
        return value;
    }

    private static String optionalPathText(Document document, String... path) {
        Element element = findPathElement(document, path);
        if (element == null) {
            return null;
        }
        String value = element.getTextContent();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Element findPathElement(Document document, String... path) {
        Element current = document.getDocumentElement();
        if (current == null || !"Document".equals(current.getLocalName())) {
            return null;
        }

        for (String localName : path) {
            current = firstDirectChild(current, REQUEST_NS, localName);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Element firstDirectChild(Element parent, String namespace, String localName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                if (namespace.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName())) {
                    return element;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private record RequestProjection(
            String protocolVersion,
            String exchangeId,
            String transactionDateTime,
            String transactionReference,
            String totalAmount,
            String cardIdentifier,
            String merchantIdentifier,
            String acceptorIdentifier) {

        private static RequestProjection from(Document requestDocument) {
            return new RequestProjection(
                    requiredPathText(requestDocument, "Hdr/PrtcolVrsn", "AccptrAuthstnReq", "Hdr", "PrtcolVrsn"),
                    requiredPathText(requestDocument, "Hdr/XchgId", "AccptrAuthstnReq", "Hdr", "XchgId"),
                    requiredPathText(requestDocument, "Tx/TxId/TxDtTm", "AccptrAuthstnReq", "AuthstnReq", "Tx", "TxId", "TxDtTm"),
                    requiredPathText(requestDocument, "Tx/TxId/TxRef", "AccptrAuthstnReq", "AuthstnReq", "Tx", "TxId", "TxRef"),
                    requiredPathText(requestDocument, "Tx/TxDtls/TtlAmt", "AccptrAuthstnReq", "AuthstnReq", "Tx", "TxDtls", "TtlAmt"),
                    firstNonNull(
                            optionalPathText(requestDocument, "AccptrAuthstnReq", "AuthstnReq", "Envt", "Card", "PlainCardData", "PAN"),
                            optionalPathText(requestDocument, "AccptrAuthstnReq", "AuthstnReq", "Envt", "Card", "MskdPAN")
                    ),
                    optionalPathText(requestDocument, "AccptrAuthstnReq", "AuthstnReq", "Envt", "Mrchnt", "Id", "Id"),
                    optionalPathText(requestDocument, "AccptrAuthstnReq", "AuthstnReq", "Envt", "POI", "Id", "Id")
            );
        }
    }

    private record AuthorisationDecision(
            String responseCode,
            String approvalCode,
            String responseReason,
            String additionalResponseInfo) {

        private static AuthorisationDecision approve(String approvalCode) {
            return new AuthorisationDecision("APPR", approvalCode, null, null);
        }

        private static AuthorisationDecision decline(String responseReason, String additionalResponseInfo) {
            return new AuthorisationDecision("DECL", null, responseReason, additionalResponseInfo);
        }
    }

    private static String firstNonNull(String firstValue, String secondValue) {
        return firstValue != null ? firstValue : secondValue;
    }
}
