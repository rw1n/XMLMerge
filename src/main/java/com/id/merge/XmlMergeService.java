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
        Element targetParent = findParentByPath(root, change.getPath());
        if (targetParent == null) return;

        String nodeName = extractElementName(change.getPath());
        int index = extractIndex(change.getPath()) - 1;

        switch (change.getType()) {
            case ADD -> {
                Element toAdd = change.getNewElement().clone();
                List<Element> children = targetParent.getChildren(nodeName);
                if (index >= 0 && index <= children.size()) {
                    children.add(index, toAdd);
                    targetParent.removeChildren(nodeName);
                    targetParent.addContent(children);
                } else {
                    targetParent.addContent(toAdd);
                }
            }
            case REMOVE -> {
                List<Element> children = targetParent.getChildren(nodeName);
                if (index >= 0 && index < children.size()) {
                    targetParent.removeContent(children.get(index));
                }
            }
            case MODIFY -> {
                List<Element> children = targetParent.getChildren(nodeName);
                if (index >= 0 && index < children.size()) {
                    targetParent.setContent(targetParent.indexOf(children.get(index)), change.getNewElement().clone());
                }
            }
        }
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