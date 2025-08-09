package com.id.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ChangeSet {

    private final List<Change> changes = new ArrayList<>();

    public void addChange(Change change) {
        changes.add(change);
    }

    public void addAll(ChangeSet other) {
        this.changes.addAll(other.getChanges());
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @Override
    public String toString() {
        return "ChangeSet{" +
                "changes=" + changes +
                '}';
    }
}
