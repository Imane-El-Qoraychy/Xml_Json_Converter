package com.mycompany.xmljsonconverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class LocalConverterService {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public LocalConverterService() {
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
    }

    public String xmlToJson(String xml) throws Exception {
        String rootName = extractXmlRootName(xml);

        JsonNode node = xmlMapper.readTree(xml);

        ObjectNode wrapped = jsonMapper.createObjectNode();
        wrapped.set(rootName, node);

        return jsonMapper.writeValueAsString(wrapped);
    }

    public String jsonToXml(String json, String rootName) throws Exception {
        JsonNode node = jsonMapper.readTree(json);

        return xmlMapper.writer()
                .withRootName(rootName)
                .writeValueAsString(node);
    }

    public String jsonToXmlAutoRoot(String json) throws Exception {
        JsonNode node = jsonMapper.readTree(json);

        if (node.isObject() && node.size() == 1) {
            String rootName = node.fieldNames().next();
            JsonNode rootValue = node.get(rootName);

            return xmlMapper.writer()
                    .withRootName(rootName)
                    .writeValueAsString(rootValue);
        }

        return xmlMapper.writer()
                .withRootName("root")
                .writeValueAsString(node);
    }

    private String extractXmlRootName(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(false);

        Document doc = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        return doc.getDocumentElement().getTagName();
    }
}
