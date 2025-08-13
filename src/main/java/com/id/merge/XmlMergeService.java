package com.id.merge;

import com.id.diff.XmlDiffService;
import com.id.model.Change;
import com.id.model.ChangeSet;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.*;
import java.util.stream.Collectors;

public class XmlMergeService {

    public static class MergeResult {
        public final Document merged;
        public final List<Change> conflicts;

        public MergeResult(Document merged, List<Change> conflicts) {
            this.merged = merged;
            this.conflicts = conflicts;
        }
    }

    public MergeResult merge(Document baselineOld, Document baselineNew, Document customer) {
        Element oldRoot = baselineOld.getRootElement();
        Element newRoot = baselineNew.getRootElement();
        Element customerRoot = customer.getRootElement();

        ChangeSet customerChanges = XmlDiffService.compareElements(oldRoot, customerRoot, "root");
        Set<String> customerPaths = customerChanges.getChanges().stream()
                .map(Change::getPath)
                .collect(Collectors.toSet());

        ChangeSet baselineChanges = XmlDiffService.compareElements(oldRoot, newRoot, "root");

        List<Change> conflicts = new ArrayList<>();
        for (Change change : baselineChanges.getChanges()) {
            if (customerPaths.contains(change.getPath())) {
                conflicts.add(change);
            } else {
                applyChange(customerRoot, change);
            }
        }

        return new MergeResult(customer, conflicts);
    }

    private void applyChange(Element root, Change change) {
        Element parent = findParentByPath(root, change.getPath());
        if (parent == null) return;

        String name = extractElementName(change.getPath());
        // WICHTIG: Index nur aus dem letzten Segment ziehen (z.B. "item[2]")
        int nth = extractIndex(lastSegment(change.getPath())) - 1; // 0-basiert

        switch (change.getType()) {
            case ADD -> {
                Element toAdd = change.getNewElement().clone();
                int insertPos = findGlobalInsertPosition(parent, name, nth);
                if (insertPos < 0) parent.addContent(toAdd);
                else parent.addContent(insertPos, toAdd);
            }
            case REMOVE -> {
                Element target = findNthChildByName(parent, name, nth);
                if (target != null) parent.removeContent(target);
            }
            case MODIFY -> {
                Element target = findNthChildByName(parent, name, nth);
                if (target != null) {
                    int globalIdx = parent.indexOf(target);
                    parent.setContent(globalIdx, change.getNewElement().clone());
                }
            }
        }
    }

    private String lastSegment(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private Element findNthChildByName(Element parent, String name, int nth) {
        int count = 0;
        for (org.jdom2.Content c : parent.getContent()) {
            if (c instanceof Element e && e.getName().equals(name)) {
                if (count == nth) return e;
                count++;
            }
        }
        return null;
    }

    // Ermittelt die globale Einfügeposition im Parent-Content für das n-te gleichnamige Element
    private int findGlobalInsertPosition(Element parent, String name, int nth) {
        if (nth <= 0) {
            // vor das erste gleichnamige (oder ans Ende, falls keines existiert)
            for (int i = 0; i < parent.getContentSize(); i++) {
                org.jdom2.Content c = parent.getContent(i);
                if (c instanceof Element e && e.getName().equals(name)) return i;
            }
            return parent.getContentSize();
        }
        int count = 0;
        for (int i = 0; i < parent.getContentSize(); i++) {
            org.jdom2.Content c = parent.getContent(i);
            if (c instanceof Element e && e.getName().equals(name)) {
                if (count == nth - 1) return i + 1; // hinter das (nth-1)-te
                count++;
            }
        }
        return parent.getContentSize();
    }

    private Element findParentByPath(Element root, String fullPath) {
        String[] parts = fullPath.split("/");
        Element current = root;
        for (int i = 1; i < parts.length - 1; i++) {
            String raw = parts[i];
            String name = raw.replaceAll("\\[\\d+\\]", "");
            int index = extractIndex(raw) - 1;
            List<Element> children = current.getChildren(name);
            if (children.isEmpty() || index >= children.size()) return null;
            current = children.get(index);
        }
        return current;
    }

    private String extractElementName(String path) {
        String[] parts = path.split("/");
        String last = parts[parts.length - 1];
        return last.replaceAll("\\[\\d+\\]", "");
    }

    private int extractIndex(String part) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*\\[(\\d+)]").matcher(part);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 1;
    }
}