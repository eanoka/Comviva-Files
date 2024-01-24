package com.grameenphone.wipro.extensions.jackson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonExcludeFilteredPaths {
    public Set<String> excludedPaths = new HashSet<>();
    public List<String> travelledParentPaths = new ArrayList<>();
    public List<Object> stacks = new ArrayList<>();

    @Override
    public String toString() {
        return "Excluded: " + excludedPaths + "\nTravelled: " + travelledParentPaths;
    }
}