package com.example.addon;

import com.example.addon.commands.PrintModuleCommand;
import com.example.addon.commands.PrintOptionsCommand;
import com.example.addon.modules.*;
import dev.boze.api.BozeInstance;
import dev.boze.api.addon.Addon;
import dev.boze.api.render.ClientColor;
import dev.boze.api.render.ColorMaker;

public class ExampleAddon extends Addon {

    public static final String ID = "example-addon";
    public static final String NAME = "ExampleAddon";
    public static final String DESCRIPTION = "Example addon for boze.dev utility mod";
    public static final String VERSION = "1.0.0";

    // Branding the example's chat output uses in place of [Boze] (see the print commands).
    public static final String BRAND = "Example";
    public static final ClientColor BRAND_COLOR = ColorMaker.staticColor(90, 200, 255);

    public ExampleAddon() {
        super(ID, NAME, DESCRIPTION, VERSION);
    }

    @Override
    public boolean initialize() {
        // Give this addon's modules their own GUI category instead of the shared "Addons" one
        createCategory("Example");

        // Register commands - demonstrate command API
        dispatcher.registerCommand(PrintModuleCommand.INSTANCE);
        dispatcher.registerCommand(PrintOptionsCommand.INSTANCE);

        // Register modules - demonstrate different API features
        modules.add(ExampleSurround.INSTANCE);
        modules.add(ExampleWorldRender.INSTANCE);
        modules.add(ExampleBlockHighlight.INSTANCE);
        modules.add(ExampleBreak.INSTANCE);
        modules.add(ExampleMiner.INSTANCE);
        modules.add(ExampleInfoHud.INSTANCE);
        modules.add(ExampleHat.INSTANCE);
        modules.add(ExampleRange.INSTANCE);

        // Register client module extensions - demonstrate extension API
        extensions.add(new ExampleExtension());
        extensions.add(new ExampleAutoMineExtension());

        // Register package for event handler
        BozeInstance.INSTANCE.registerPackage("com.example.addon");

        return true;
    }
}
