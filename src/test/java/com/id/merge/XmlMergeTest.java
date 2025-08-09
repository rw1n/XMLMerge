package com.id.merge;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlMergeTest {
    @Test
    public void testMerge_addNewElement() throws Exception {
        String oldXml = """
            <root>
                <item id=\"1\">A</item>
            </root>""";

        String newXml = """
            <root>
                <item id=\"1\">A</item>
                <item id=\"2\">B</item>
            </root>""";

        String customerXml = """
            <root>
                <item id=\"1\">A</item>
            </root>""";

        SAXBuilder builder = new SAXBuilder();
        Document oldDoc = builder.build(new StringReader(oldXml));
        Document newDoc = builder.build(new StringReader(newXml));
        Document custDoc = builder.build(new StringReader(customerXml));

        XmlMergeService service = new XmlMergeService();
        XmlMergeService.MergeResult result = service.merge(oldDoc, newDoc, custDoc);

        Element root = result.merged.getRootElement();
        List<Element> items = root.getChildren("item");

        assertEquals(2, items.size());
        assertEquals("1", items.get(0).getAttributeValue("id"));
        assertEquals("2", items.get(1).getAttributeValue("id"));
        assertTrue(result.conflicts.isEmpty());
    }

    @Test
    public void testMerge_detectConflict() throws Exception {
        String oldXml = """
            <root>
                <item id=\"1\">A</item>
            </root>""";

        String newXml = """
            <root>
                <item id=\"1\">X</item>
            </root>""";

        String customerXml = """
            <root>
                <item id=\"1\">B</item>
            </root>""";

        SAXBuilder builder = new SAXBuilder();
        Document oldDoc = builder.build(new StringReader(oldXml));
        Document newDoc = builder.build(new StringReader(newXml));
        Document custDoc = builder.build(new StringReader(customerXml));

        XmlMergeService service = new XmlMergeService();
        XmlMergeService.MergeResult result = service.merge(oldDoc, newDoc, custDoc);

        Element root = result.merged.getRootElement();
        List<Element> items = root.getChildren("item");

        // Kunde hat Wert B, baseline_new hat Wert X → Konflikt, es wird nichts geändert
        assertEquals("B", items.get(0).getText());
        assertEquals(1, result.conflicts.size());
        assertEquals("root/item[1]", result.conflicts.get(0).getPath());
    }

    @Test
    public void testMerge_removeElement() throws Exception {
        String oldXml = """
            <root>
                <item id=\"1\"/>
                <item id=\"2\"/>
            </root>""";

        String newXml = """
            <root>
                <item id=\"1\"/>
            </root>""";

        String customerXml = """
            <root>
                <item id=\"1\"/>
                <item id=\"2\"/>
            </root>""";

        SAXBuilder builder = new SAXBuilder();
        Document oldDoc = builder.build(new StringReader(oldXml));
        Document newDoc = builder.build(new StringReader(newXml));
        Document custDoc = builder.build(new StringReader(customerXml));

        XmlMergeService service = new XmlMergeService();
        XmlMergeService.MergeResult result = service.merge(oldDoc, newDoc, custDoc);

        Element root = result.merged.getRootElement();
        List<Element> items = root.getChildren("item");

        assertEquals(1, items.size());
        assertEquals("1", items.get(0).getAttributeValue("id"));
        assertTrue(result.conflicts.isEmpty());
    }
}
