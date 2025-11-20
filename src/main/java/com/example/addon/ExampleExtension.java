package com.example.addon;

import dev.boze.api.client.module.ClientModuleExtension;
import dev.boze.api.client.ModuleManager;
import dev.boze.api.event.EventPlayerUpdate;
import dev.boze.api.option.ColorOption;
import dev.boze.api.option.Option;
import dev.boze.api.option.PageOption;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import meteordevelopment.orbit.EventHandler;

/**
 * Example extension for the AutoCrystal module
 * This demonstrates how to extend client modules with additional settings and behavior
 */
public class ExampleExtension extends ClientModuleExtension {

    // Set in constructor
    public ToggleOption winRenderEnabled;
    public ColorOption winRenderColor;

    // Added to root options list
    public final ToggleOption autoWin = new ToggleOption(parent, "AutoWin", "Automatically win the game (imaginary feature)");
    public final SliderOption winDelay = new SliderOption(parent, "WinDelay", "Delay before winning", 0.0, 0.0, 10.0, 0.1, autoWin);
    public final ToggleOption winSound = new ToggleOption(parent, "WinSound", "Play sound when winning", true, autoWin);

    public ExampleExtension() {
        // Get the AutoCrystal module and pass it to the parent constructor
        super(ModuleManager.getClientModule("AutoCrystal"));

        // Find the existing Render page in AutoCrystal
        Option<?> renderPage = null;
        for (Option<?> option : parent.getOptions()) {
            if (option instanceof PageOption && option.name.equalsIgnoreCase("Render")) {
                renderPage = option;
                break;
            }
        }

        // Create win render subsettings under the existing render page
        if (renderPage != null) {
            winRenderEnabled = new ToggleOption(parent, "WinRender", "Enable win rendering", true, renderPage);
            winRenderColor = new ColorOption(parent, "WinRenderColor", "Color for win rendering", ColorMaker.staticColor(0, 255, 0), 0.1f, 1.0f, winRenderEnabled::getValue, renderPage);
        }
    }

    @Override
    public void onEnable() {
        // Add custom behavior when the extension is enabled
        // This is called when the module is enabled, after the extension is subscribed to the event bus
        System.out.println("ExampleExtension enabled!");
    }

    @Override
    public void onDisable() {
        // Add custom behavior when the extension is disabled
        // This is called when the module is disabled, before the extension is unsubscribed from the event bus
        System.out.println("ExampleExtension disabled!");
    }

    @EventHandler
    private void onPlayerUpdate(EventPlayerUpdate event) {
        // Print every player update (as requested)
        System.out.println("Player update - extension active!");
    }
}