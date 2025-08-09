package com.id.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdom2.Element;

@Getter
@AllArgsConstructor
public class Change {
    private final ChangeType type;
    private final String path;
    private final Element oldElement;
    private final Element newElement;

    @Override
    public String toString() {
        return "Change{" +
                "type=" + type +
                ", path='" + path + '\'' +
                ", oldElement=" + (oldElement != null ? oldElement.getName() : "null") +
                ", newElement=" + (newElement != null ? newElement.getName() : "null") +
                '}';
    }
}