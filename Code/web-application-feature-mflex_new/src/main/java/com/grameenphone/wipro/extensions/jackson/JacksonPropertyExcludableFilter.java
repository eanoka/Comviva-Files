package com.grameenphone.wipro.extensions.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

public class JacksonPropertyExcludableFilter extends SimpleBeanPropertyFilter {
    private final JsonPropertyExcludableMapperModule module;

    public JacksonPropertyExcludableFilter(JsonPropertyExcludableMapperModule module) {
        this.module = module;
    }

    private boolean existsInStack(JsonExcludeFilteredPaths _states, Object pojo) {
        return _states.stacks.stream().anyMatch(o -> o == pojo);
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
        JsonExcludeFilteredPaths _states = module.states.get();
        String name = writer.getName();
        int travelledPathSize = 0;
        String currentTravelledPath = "";
        if (!_states.excludedPaths.isEmpty()) {
            travelledPathSize = _states.travelledParentPaths.size();
            currentTravelledPath = (travelledPathSize == 0 ? "" : (_states.travelledParentPaths.get(travelledPathSize - 1) + ".")) + name;
            _states.travelledParentPaths.add(currentTravelledPath);
        }
        boolean addedInStack = false;
        try {
            if (_states.excludedPaths.contains(currentTravelledPath)) {
                return;
            }
            if(existsInStack(_states, pojo)) {
                return;
            }
            _states.stacks.add(pojo);
            addedInStack = true;
            super.serializeAsField(pojo, jgen, provider, writer);
        } finally {
            if(addedInStack) {
                _states.stacks.remove(_states.stacks.size() - 1);
            }
            if (!_states.excludedPaths.isEmpty()) {
                _states.travelledParentPaths.remove(travelledPathSize);
            }
        }
    }
}