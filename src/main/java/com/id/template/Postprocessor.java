package com.id.template;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Postprocessor {

    public static void enrichTemplates(Document jdomDoc) {
        int idCounter = 1;

        List<Element> templates = StreamSupport.stream(jdomDoc.getRootElement().getDescendants().spliterator(), false)
                .filter(n -> n instanceof Element && ((Element) n).getName().equals("template"))
                .map(n -> (Element) n)
                .collect(Collectors.toList());

        for (Element template : templates) {
            String original = template.getAttributeValue("original");
            template.setAttribute("type", determineType(original));
            template.setAttribute("anchor", determineAnchor(template));
            template.setAttribute("id", String.valueOf(idCounter++));
        }
    }

    // log + ui
    private static String determineType(String template) {
        if (template.contains("is_end")) return "close";
        if (template.contains("is")) return "open";
        return "single";
    }

    // eher für log + ui anzeige, keine wirklich robuste positionsbestimmung
    private static String determineAnchor(Element template) {
        Element parent = template.getParentElement();
        if (parent == null) return "unknown";

        List<Content> siblings = parent.getContent();
        int index = siblings.indexOf(template);

        for (int i = index - 1; i >= 0; i--) {
            Content previous = siblings.get(i);
            if (previous instanceof Element) {
                return ((Element) previous).getName();
            }
        }

        return parent.getName();
    }
}