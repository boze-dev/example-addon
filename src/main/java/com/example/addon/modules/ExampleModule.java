package com.example.addon.modules;

import com.example.addon.impl.ExampleAddonModule;
import com.example.addon.impl.ExampleAddonToggle;
import dev.boze.api.Globals;
import dev.boze.api.event.EventPlayerUpdate;
import meteordevelopment.orbit.EventHandler;

public class ExampleModule extends ExampleAddonModule {

    private final ExampleAddonToggle toggle = new ExampleAddonToggle("ExampleToggle", "An example toggle", true);

    public ExampleModule() {
        super("ExampleModule", "ExampleModule", "Example module");
        elements.add(toggle);
    }

    @EventHandler
    private void onPlayerUpdate(EventPlayerUpdate event) {
        Globals.getChatHelper().sendMsg("Example module's toggle value: " + toggle.getValue());
    }
}
