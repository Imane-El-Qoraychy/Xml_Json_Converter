package com.mycompany.xmljsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@RestController
@RequestMapping("/api")
public class ConvertController {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public ConvertController() {
        jsonMapper = new ObjectMapper();
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
    }

    @PostMapping(value = "/xml-to-json",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String xmlToJson(@RequestBody String xml) throws Exception {

        String rootName = extractXmlRootName(xml);
        JsonNode node = xmlMapper.readTree(xml);

        ObjectNode wrapped = jsonMapper.createObjectNode();
        wrapped.set(rootName, node);

        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapped);
    }

    @PostMapping(value = "/json-to-xml",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String jsonToXml(@RequestBody String json,
                            @RequestParam(defaultValue = "root") String root) throws Exception {
        JsonNode node = jsonMapper.readTree(json);
        return xmlMapper.writer().withRootName(root).writeValueAsString(node);
    }

    @PostMapping(value = "/json-to-xml-auto",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String jsonToXmlAuto(@RequestBody String json) throws Exception {
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
