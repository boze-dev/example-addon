package com.example.addon;

import com.example.addon.commands.PrintModuleCommand;
import com.example.addon.commands.PrintOptionsCommand;
import com.example.addon.modules.*;
import dev.boze.api.BozeInstance;
import dev.boze.api.addon.Addon;

public class ExampleAddon extends Addon {

    public static final String ID = "example-addon";
    public static final String NAME = "ExampleAddon";
    public static final String DESCRIPTION = "Example addon for boze.dev utility mod";
    public static final String VERSION = "1.0.0";

    public ExampleAddon() {
        super(ID, NAME, DESCRIPTION, VERSION);
    }

    @Override
    public boolean initialize() {
        // Register commands - demonstrate command API
        dispatcher.registerCommand(PrintModuleCommand.INSTANCE);
        dispatcher.registerCommand(PrintOptionsCommand.INSTANCE);

        // Register modules - demonstrate different API features
        modules.add(ExampleSurround.INSTANCE);
        modules.add(ExampleWorldRender.INSTANCE);
        modules.add(ExampleBlockHighlight.INSTANCE);

        // Register client module extensions - demonstrate extension API
        extensions.add(new ExampleExtension());

        // Register package for event handler
        BozeInstance.INSTANCE.registerPackage("com.example.addon");

        return true;
    }
}
