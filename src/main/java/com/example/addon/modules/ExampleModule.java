package com.example.addon.modules;

import dev.boze.api.Globals;
import dev.boze.api.addon.module.ToggleableModule;
import dev.boze.api.event.EventPlayerUpdate;
import dev.boze.api.setting.SettingToggle;
import meteordevelopment.orbit.EventHandler;

public class ExampleModule extends ToggleableModule {

    private final SettingToggle toggle = new SettingToggle("ExampleToggle", "An example toggle", true);

    public ExampleModule() {
        super("ExampleModule", "An example addon module");
        elements.add(toggle);
    }

    @EventHandler
    private void onPlayerUpdate(EventPlayerUpdate event) {
        Globals.getChatHelper().sendMsg("Example module's toggle value: " + toggle.getValue());
    }
}
