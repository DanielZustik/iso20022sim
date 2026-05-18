package cz.monetplus.iso20022sim.validation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringReader;

@Component
public class AuthorisationRequestValidator {

    static final String SUPPORTED_NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.15";
    private static final String NAMESPACE_PREFIX = "urn:iso:std:iso:20022:tech:xsd:caaa.001.001.";

    private final Schema schema;

    public AuthorisationRequestValidator() {
        this.schema = loadSchema();
    }

    public void validate(String xml) {
        Document document = parse(xml);
        String namespaceUri = document.getDocumentElement().getNamespaceURI();
        if (!SUPPORTED_NAMESPACE.equals(namespaceUri)) {
            if (namespaceUri != null && namespaceUri.startsWith(NAMESPACE_PREFIX)) {
                throw new UnsupportedMessageVersionException(
                        "Unsupported ISO 20022 message/version namespace: " + namespaceUri
                );
            }
            throw new SchemaInvalidXmlException(
                    "Expected ISO 20022 namespace " + SUPPORTED_NAMESPACE + " but received: " + namespaceUri,
                    null
            );
        }

        validateAgainstSchema(document);
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            disableExternalEntities(dbf);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXParseException ex) {
            throw new MalformedXmlException(
                    "XML parsing failed at line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ": " + ex.getMessage(),
                    ex
            );
        } catch (SAXException | IOException | RuntimeException ex) {
            throw new MalformedXmlException("XML parsing failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new MalformedXmlException("XML parsing failed due to parser configuration error.", ex);
        }
    }

    private void validateAgainstSchema(Document document) {
        try {
            schema.newValidator().validate(new DOMSource(document));
        } catch (SAXParseException ex) {
            throw new SchemaInvalidXmlException(
                    "XML does not conform to caaa.001.001.15 schema at line " + ex.getLineNumber()
                            + ", column " + ex.getColumnNumber() + ": " + ex.getMessage(),
                    ex
            );
        } catch (SAXException | IOException ex) {
            throw new SchemaInvalidXmlException("XML does not conform to caaa.001.001.15 schema: " + ex.getMessage(), ex);
        }
    }

    private Schema loadSchema() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return schemaFactory.newSchema(new ClassPathResource("xsd/caaa.001.001.15.xsd").getURL());
        } catch (SAXException | IOException ex) {
            throw new IllegalStateException("Failed to load request schema for caaa.001.001.15.", ex);
        }
    }

    private void disableExternalEntities(DocumentBuilderFactory dbf) {
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to configure secure XML parser.", ex);
        }
    }
}
