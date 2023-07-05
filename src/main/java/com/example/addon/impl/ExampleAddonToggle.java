package com.example.addon.impl;

import com.google.gson.JsonObject;
import dev.boze.api.addon.gui.AddonToggle;
import dev.boze.api.config.Serializable;

public class ExampleAddonToggle implements AddonToggle, Serializable<ExampleAddonToggle> {

    private final String name;
    private final String description;

    private boolean value;

    public ExampleAddonToggle(String name, String description, boolean defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    @Override
    public boolean getValue() {
        return value;
    }

    @Override
    public void setValue(boolean newValue) {
        this.value = newValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("value", value);
        return object;
    }

    @Override
    public ExampleAddonToggle fromJson(JsonObject jsonObject) {
        value = jsonObject.get("value").getAsBoolean();
        return this;
    }
}
