package com.id.diff;

import com.id.model.*;
import org.jdom2.Element;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class XmlDiffService {

    // "id" als Schlüsselattribut
    private static final List<String> CANDIDATE_KEYS = List.of("id");

    /**
     * Vergleicht zwei Elemente rekursiv und erzeugt ein ChangeSet.
     * Kinderlisten werden pro Tagname via LCS gematcht (soft),
     * Inhaltsänderungen (Attribute/Text) werden danach als MODIFY erkannt (strict).
     */
    public static ChangeSet compareElements(Element oldElement, Element newElement, String path) {
        ChangeSet changes = new ChangeSet();

        // Unterschiedlicher Tagname → harter MODIFY auf dieser Ebene und Abbruch
        if (!oldElement.getName().equals(newElement.getName())) {
            changes.addChange(new Change(ChangeType.MODIFY, path, oldElement, newElement));
            return changes;
        }

        // 1) Eigenen Inhalt vergleichen (Attribute reihenfolgeunabhängig + Text)
        boolean selfModified = !attrsToMap(oldElement).equals(attrsToMap(newElement))
                || !Objects.equals(oldElement.getTextNormalize(), newElement.getTextNormalize());
        if (selfModified) {
            changes.addChange(new Change(ChangeType.MODIFY, path, oldElement, newElement));
        }

        // 2) Kinder gruppieren (pro Tagname) und LCS pro Gruppe fahren
        Map<String, List<Element>> oldGroups = groupChildrenByName(oldElement);
        Map<String, List<Element>> newGroups = groupChildrenByName(newElement);

        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(oldGroups.keySet());
        allNames.addAll(newGroups.keySet());

        for (String name : allNames) {
            List<Element> oldList = oldGroups.getOrDefault(name, List.of());
            List<Element> newList = newGroups.getOrDefault(name, List.of());

            List<DiffOp> lcs = computeLcsDiff(oldList, newList);

            int iOld = 0, iNew = 0;
            for (DiffOp op : lcs) {
                String subPath = path + "/" + name + "[" + (op.index() + 1) + "]";
                switch (op.type()) {
                    case ADD -> {
                        Element added = newList.get(iNew++);
                        changes.addChange(new Change(ChangeType.ADD, subPath, null, added));
                    }
                    case REMOVE -> {
                        Element removed = oldList.get(iOld++);
                        changes.addChange(new Change(ChangeType.REMOVE, subPath, removed, null));
                    }
                    case MATCH -> {
                        Element o = oldList.get(iOld++);
                        Element n = newList.get(iNew++);
                        // Wichtig: immer rekursiv in Kinder gehen, auch wenn sich o/n in Attributen/Text unterscheiden
                        ChangeSet sub = compareElements(o, n, subPath);
                        for (Change c : sub.getChanges()) {
                            changes.addChange(c);
                        }
                    }
                }
            }
        }

        return changes;
    }

    // ===== LCS auf Basis stabiler Element-Signaturen (ohne Kinder, ohne Text) =====

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
                ops.add(new DiffOp(DiffType.MATCH, j));
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                ops.add(new DiffOp(DiffType.REMOVE, i));
                i++;
            } else {
                ops.add(new DiffOp(DiffType.ADD, j));
                j++;
            }
        }
        while (i < m) ops.add(new DiffOp(DiffType.REMOVE, i++));
        while (j < n) ops.add(new DiffOp(DiffType.ADD, j++));

        return ops;
    }

    // ===== Gleichheit/Signatur =====

    // Strikter Inhaltsvergleich (für MODIFY-Erkennung auf aktueller Ebene)
    private static boolean elementsEqual(Element a, Element b) {
        if (!a.getName().equals(b.getName())) return false;
        if (!attrsToMap(a).equals(attrsToMap(b))) return false; // Attributreihenfolge egal
        return Objects.equals(a.getTextNormalize(), b.getTextNormalize());
    }

    // Tolerantes Matching für LCS: gleicher Name + gleiche Signatur (id bevorzugt, sonst Attr-Hash)
    private static boolean elementsSoftEqual(Element a, Element b) {
        if (!a.getName().equals(b.getName())) return false;
        return signatureKey(a).equals(signatureKey(b));
    }

    private static String signatureKey(Element e) {
        // 1) Bevorzugt id
        String id = e.getAttributeValue("id");
        if (id != null) return e.getName() + "|id=" + id;
        // 2) Fallback: Hash nur über sortierte Attribute (ohne Text, ohne Kinder)
        return e.getName() + "|attrsHash=" + attrsHash(e);
    }

    private static String attrsHash(Element e) {
        String s = attrsToMap(e).entrySet().stream()
                .map(en -> en.getKey() + "=" + en.getValue())
                .collect(Collectors.joining(";"));
        return sha256(s);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // Attribute reihenfolgeunabhängig (sortiert)
    private static Map<String, String> attrsToMap(Element e) {
        if (e == null) return Collections.emptyMap();
        Map<String, String> m = new TreeMap<>();
        e.getAttributes().forEach(a -> m.put(a.getName(), a.getValue()));
        return m;
    }

    private static Map<String, List<Element>> groupChildrenByName(Element element) {
        Map<String, List<Element>> map = new LinkedHashMap<>();
        for (Element child : element.getChildren()) {
            map.computeIfAbsent(child.getName(), k -> new ArrayList<>()).add(child);
        }
        return map;
    }
}