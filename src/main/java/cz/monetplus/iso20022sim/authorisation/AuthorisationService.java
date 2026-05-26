package cz.monetplus.iso20022sim.authorisation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Service
public class AuthorisationService {

    private static final String REQUEST_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15";
    private static final String RESPONSE_NS = "urn:iso:std:iso:20022:tech:xsd:caaa.002.001.15";
    private static final String ISO_20022_NAMESPACE_PREFIX = "urn:iso:std:iso:20022:tech:xsd:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Schema requestSchema; // zabundlovane ze spring-web depen.
    private final Schema responseSchema;

    public AuthorisationService() {
        this.requestSchema = loadSchema("xsd/caaa.001.001.15.xsd");
        this.responseSchema = loadSchema("xsd/caaa.002.001.15.xsd");
    }

    public String approveAuthorisation(String requestXml) {
        Document requestDocument = parseXml(requestXml); // well formatted xml
        ensureSupportedRequestNamespace(requestDocument);
        validateXml(requestXml, requestSchema, "Authorisation request is not schema-valid caaa.001.001.15 XML"); // valid xml

        RequestProjection requestProjection = RequestProjection.from(requestDocument);
        String responseXml = buildApprovedResponse(requestProjection);

        validateXml(responseXml, responseSchema, "Generated authorisation response is not schema-valid caaa.002.001.15 XML");
        return responseXml;
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

    private String buildApprovedResponse(RequestProjection requestProjection) {
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
            appendTextElement(responseDocument, responseToAuthorisationElement, "Rspn", "APPR");
            appendTextElement(responseDocument, authorisationResultElement, "AuthstnCd", generateApprovalCode());

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

    private static String firstElementText(Document document, String namespace, String localName, String fieldLabel) {
        NodeList nodeList = document.getElementsByTagNameNS(namespace, localName);
        if (nodeList.getLength() == 0) {
            throw new SchemaValidationException("Missing required field in request: " + fieldLabel, null);
        }
        String value = nodeList.item(0).getTextContent();
        if (value == null || value.isBlank()) {
            throw new SchemaValidationException("Field in request is blank: " + fieldLabel, null);
        }
        return value.trim();
    }

    private record RequestProjection(String protocolVersion, String exchangeId, String transactionDateTime,
                                     String transactionReference, String totalAmount) {

        private static RequestProjection from(Document requestDocument) {
            return new RequestProjection(
                    firstElementText(requestDocument, REQUEST_NS, "PrtcolVrsn", "Hdr/PrtcolVrsn"),
                    firstElementText(requestDocument, REQUEST_NS, "XchgId", "Hdr/XchgId"),
                    firstElementText(requestDocument, REQUEST_NS, "TxDtTm", "Tx/TxId/TxDtTm"),
                    firstElementText(requestDocument, REQUEST_NS, "TxRef", "Tx/TxId/TxRef"),
                    firstElementText(requestDocument, REQUEST_NS, "TtlAmt", "Tx/TxDtls/TtlAmt")
            );
        }
    }
}
