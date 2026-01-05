package com.mycompany.xmljsonconverter;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

public class ManualConverterService {

    public String xmlToJson(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(false);

        Document doc = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        Element root = doc.getDocumentElement();

        Map<String, Object> json = new LinkedHashMap<>();
        json.put(root.getTagName(), elementToValue(root));

        return JsonWriter.pretty(json);
    }

    private Object elementToValue(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();

        NamedNodeMap attrs = element.getAttributes();
        if (attrs != null && attrs.getLength() > 0) {
            Map<String, Object> attrMap = new LinkedHashMap<>();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node a = attrs.item(i);
                attrMap.put(a.getNodeName(), a.getNodeValue());
            }
            map.put("@attributes", attrMap);
        }

        NodeList children = element.getChildNodes();
        boolean hasElementChild = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                Element child = (Element) n;

                String tag = child.getTagName();
                Object value = elementToValue(child);

                if (map.containsKey(tag)) {
                    Object existing = map.get(tag);

                    if (existing instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        List<Object> l = (List<Object>) list;
                        l.add(value);
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(existing);
                        list.add(value);
                        map.put(tag, list);
                    }
                } else {
                    map.put(tag, value);
                }
            }
        }

        String directText = getDirectText(element).trim();

        if (!hasElementChild) {
           
            if (map.containsKey("@attributes")) {
                if (!directText.isEmpty()) map.put("#text", directText);
                return map;
            }
  
            return directText;
        }

        if (!directText.isEmpty()) {
            map.put("#text", directText);
        }

        return map;
    }

    private String getDirectText(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                String t = n.getTextContent();
                if (t != null && !t.isBlank()) {
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }

    public String jsonToXml(String json, String rootName) throws Exception {
        Object parsed = new SimpleJsonParser(json).parse();
        return xmlFromParsed(rootName, parsed);
    }

    public String jsonToXmlAutoRoot(String json) throws Exception {
        Object parsed = new SimpleJsonParser(json).parse();

        if (parsed instanceof Map<?, ?> map && map.size() == 1) {
            String rootName = map.keySet().iterator().next().toString();
            Object rootValue = map.values().iterator().next();
            return xmlFromParsed(rootName, rootValue);
        }

        return xmlFromParsed("root", parsed);
    }

    private String xmlFromParsed(String rootName, Object rootValue) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(buildXml(rootName, rootValue, 0));
        return xml.toString();
    }

    private String buildXml(String name, Object value, int indent) {
        String pad = "  ".repeat(Math.max(0, indent));

        if (value == null) {
            return pad + "<" + name + "/>\n";
        }

        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                sb.append(buildXml(name, item, indent));
            }
            return sb.toString();
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return pad + "<" + name + ">" + escapeXml(String.valueOf(value)) + "</" + name + ">\n";
        }

        if (!(value instanceof Map<?, ?>)) {
            return pad + "<" + name + ">" + escapeXml(String.valueOf(value)) + "</" + name + ">\n";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;

        StringBuilder attrs = new StringBuilder();
        Object attrObj = map.get("@attributes");
        if (attrObj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> a = (Map<String, Object>) attrObj;
            for (var e : a.entrySet()) {
                attrs.append(" ")
                        .append(e.getKey())
                        .append("=\"")
                        .append(escapeXml(String.valueOf(e.getValue())))
                        .append("\"");
            }
        }

        Object textObj = map.get("#text");
        String text = (textObj == null) ? null : String.valueOf(textObj);

        if (text != null && text.trim().isEmpty()) {
            text = null;
        }

        List<String> childKeys = new ArrayList<>();
        for (String k : map.keySet()) {
            if (k.equals("@attributes") || k.equals("#text")) continue;
            childKeys.add(k);
        }

        if (childKeys.isEmpty()) {
            if (text == null || text.isBlank()) {
                return pad + "<" + name + attrs + "/>\n";
            }
            return pad + "<" + name + attrs + ">" + escapeXml(text) + "</" + name + ">\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(pad).append("<").append(name).append(attrs).append(">");

        if (text != null && !text.isBlank()) sb.append(escapeXml(text));
        sb.append("\n");

        for (String ck : childKeys) {
            Object cv = map.get(ck);

            if (cv instanceof List<?> list) {
                for (Object item : list) {
                    sb.append(buildXml(ck, item, indent + 1));
                }
            } else {
                sb.append(buildXml(ck, cv, indent + 1));
            }
        }

        sb.append(pad).append("</").append(name).append(">\n");
        return sb.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static class JsonWriter {
        static String pretty(Object v) {
            StringBuilder sb = new StringBuilder();
            write(v, sb, 0);
            sb.append("\n");
            return sb.toString();
        }

        private static void write(Object v, StringBuilder sb, int indent) {
            if (v == null) { sb.append("null"); return; }
            if (v instanceof String s) { sb.append("\"").append(escape(s)).append("\""); return; }
            if (v instanceof Number || v instanceof Boolean) { sb.append(v); return; }

            if (v instanceof List<?> list) {
                sb.append("[");
                if (!list.isEmpty()) sb.append("\n");
                for (int i = 0; i < list.size(); i++) {
                    sb.append("  ".repeat(indent + 1));
                    write(list.get(i), sb, indent + 1);
                    if (i < list.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                if (!list.isEmpty()) sb.append("  ".repeat(indent));
                sb.append("]");
                return;
            }

            if (v instanceof Map<?, ?> mapAny) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) mapAny;

                sb.append("{");
                if (!map.isEmpty()) sb.append("\n");
                int k = 0;
                for (var e : map.entrySet()) {
                    sb.append("  ".repeat(indent + 1));
                    sb.append("\"").append(escape(e.getKey())).append("\": ");
                    write(e.getValue(), sb, indent + 1);
                    if (k < map.size() - 1) sb.append(",");
                    sb.append("\n");
                    k++;
                }
                if (!map.isEmpty()) sb.append("  ".repeat(indent));
                sb.append("}");
                return;
            }

            sb.append("\"").append(escape(String.valueOf(v))).append("\"");
        }

        private static String escape(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
