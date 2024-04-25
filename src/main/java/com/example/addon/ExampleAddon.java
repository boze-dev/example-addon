package com.example.addon;

import com.google.gson.JsonObject;
import dev.boze.api.addon.Addon;
import dev.boze.api.addon.module.ToggleableModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExampleAddon extends Addon {

    public static final String ID = "example_addon";
    public static final String NAME = "Example Addon";
    public static final String DESCRIPTION = "An example addon for Boze";
    public static final String VERSION = "1.0.0";

    public static final ExampleAddon INSTANCE = new ExampleAddon();

    public static final Logger LOG = LogManager.getLogger();

    public ExampleAddon() {
        super(ID, NAME, DESCRIPTION, VERSION);
    }

    @Override
    public boolean initialize() {
        LOG.info("Initializing " + name);

        LOG.info("Successfully initialized " + name);

        return super.initialize();
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();

        JsonObject modulesObject = new JsonObject();

        for (ToggleableModule module : modules) {
            modulesObject.add(module.getName(), module.toJson());
        }

        object.add("modules", modulesObject);

        return object;
    }

    @Override
    public Addon fromJson(JsonObject jsonObject) {
        if (!jsonObject.has("modules")) {
            return this;
        }

        JsonObject modulesObject = jsonObject.getAsJsonObject("modules");

        for (ToggleableModule module : modules) {
            if (modulesObject.has(module.getName())) {
                module.fromJson(modulesObject.getAsJsonObject(module.getName()));
            }
        }

        return this;
    }
}
