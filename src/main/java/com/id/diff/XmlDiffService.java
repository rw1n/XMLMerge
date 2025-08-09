package com.id.diff;

import com.id.model.Change;
import com.id.model.ChangeSet;
import com.id.model.ChangeType;
import com.id.model.DiffOp;
import org.jdom2.Element;

import java.util.*;

public class XmlDiffService {

    public static ChangeSet compareElements(Element oldElement, Element newElement, String path) {
        ChangeSet changes = new ChangeSet();

        if (!oldElement.getName().equals(newElement.getName())) {
            changes.addChange(new Change(ChangeType.MODIFY, path, oldElement, newElement));
            return changes;
        }

        if (!Objects.equals(oldElement.getAttributes(), newElement.getAttributes()) ||
                !Objects.equals(oldElement.getTextNormalize(), newElement.getTextNormalize())) {
            changes.addChange(new Change(ChangeType.MODIFY, path, oldElement, newElement));
        }

        Map<String, List<Element>> oldGroups = groupChildrenByName(oldElement);
        Map<String, List<Element>> newGroups = groupChildrenByName(newElement);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(oldGroups.keySet());
        allKeys.addAll(newGroups.keySet());

        for (String name : allKeys) {
            List<Element> oldList = oldGroups.getOrDefault(name, List.of());
            List<Element> newList = newGroups.getOrDefault(name, List.of());

            List<DiffOp> diffOps = computeLcsDiff(oldList, newList);

            int oldIdx = 0, newIdx = 0;
            for (DiffOp op : diffOps) {
                String subPath = path + "/" + name + "[" + (op.index() + 1) + "]";
                switch (op.type()) {
                    case ADD -> changes.addChange(new Change(ChangeType.ADD, subPath, null, newList.get(newIdx++)));
                    case REMOVE -> changes.addChange(new Change(ChangeType.REMOVE, subPath, oldList.get(oldIdx++), null));
                    case MATCH -> {
                        Element oldEl = oldList.get(oldIdx++);
                        Element newEl = newList.get(newIdx++);
                        if (!elementsEqual(oldEl, newEl)) {
                            changes.addChange(new Change(ChangeType.MODIFY, subPath, oldEl, newEl));
                        } else {
                            changes.addAll(compareElements(oldEl, newEl, subPath));
                        }
                    }
                }
            }
        }

        return changes;
    }

    private static Map<String, List<Element>> groupChildrenByName(Element element) {
        Map<String, List<Element>> map = new LinkedHashMap<>();
        for (Element child : element.getChildren()) {
            map.computeIfAbsent(child.getName(), k -> new ArrayList<>()).add(child);
        }
        return map;
    }

    private static List<DiffOp> computeLcsDiff(List<Element> oldList, List<Element> newList) {
        int m = oldList.size(), n = newList.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (elementsSoftEqual(oldList.get(i), newList.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        List<DiffOp> ops = new ArrayList<>();
        int i = 0, j = 0;
        while (i < m && j < n) {
            if (elementsSoftEqual(oldList.get(i), newList.get(j))) {
                ops.add(new DiffOp(ChangeType.MATCH, j));
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                ops.add(new DiffOp(ChangeType.REMOVE, i));
                i++;
            } else {
                ops.add(new DiffOp(ChangeType.ADD, j));
                j++;
            }
        }
        while (i < m) ops.add(new DiffOp(ChangeType.REMOVE, i++));
        while (j < n) ops.add(new DiffOp(ChangeType.ADD, j++));

        return ops;
    }

    private static boolean elementsEqual(Element a, Element b) {
        return a.getName().equals(b.getName()) &&
                Objects.equals(a.getTextNormalize(), b.getTextNormalize()) &&
                Objects.equals(a.getAttributes(), b.getAttributes());
    }

    private static boolean elementsSoftEqual(Element a, Element b) {
        if (!a.getName().equals(b.getName())) return false;
        String idA = a.getAttributeValue("id");
        String idB = b.getAttributeValue("id");
        if (idA != null && idB != null) return idA.equals(idB);
        return true;
    }
}
