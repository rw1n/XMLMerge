package com.id.merge;

import com.id.model.Change;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XmlMergeFromFileTest {
    @Test
    public void testMergeWithExampleFiles() throws Exception {
        File baselineOld = new File("src/test/resources/merge-examples/baseline_old/data.xml");
        File baselineNew = new File("src/test/resources/merge-examples/baseline_new/data.xml");
        File customer    = new File("src/test/resources/merge-examples/customer/data.xml");

        SAXBuilder builder = new SAXBuilder();
        Document oldDoc = builder.build(baselineOld);
        Document newDoc = builder.build(baselineNew);
        Document custDoc = builder.build(customer);

        XmlMergeService merger = new XmlMergeService();
        XmlMergeService.MergeResult result = merger.merge(oldDoc, newDoc, custDoc);

        assertNotNull(result);
        assertNotNull(result.merged);
        assertNotNull(result.conflicts);

        System.out.println("Konflikte: " + result.conflicts.size());
        for (Change conflict : result.conflicts) {
            System.out.println("Konflikt: " + conflict);
        }

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        StringWriter writer = new StringWriter();
        outputter.output(result.merged, writer);

        System.out.println("\n--- Gemergte Customer-XML ---\n" + writer.toString());
    }
}
