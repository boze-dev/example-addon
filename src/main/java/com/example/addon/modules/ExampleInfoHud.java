package com.example.addon.modules;

import dev.boze.api.addon.AddonHudModule;
import dev.boze.api.render.ClientColor;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.render.HudLine;
import dev.boze.api.render.HudText;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Example HUD module showcasing the HUD API: an info panel of player stats, with a gray label and a
 * colored value on each line (multiple lines, multiple colors per line).
 */
public class ExampleInfoHud extends AddonHudModule {
    public static final ExampleInfoHud INSTANCE = new ExampleInfoHud();

    private static final ClientColor LABEL = ColorMaker.staticColor(170, 170, 170);

    private ExampleInfoHud() {
        super("InfoExample", "Shows player stats using the HUD API");
    }

    @Override
    public List<HudLine> getLines() {
        List<HudLine> lines = new ArrayList<>();

        Player player = Minecraft.getInstance().player;
        if (player == null) return lines;

        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();
        ClientColor healthColor = ColorMaker.staticColor(
                health > maxHealth * 0.5 ? 85 : 255,
                health > maxHealth * 0.25 ? 255 : 85,
                85);

        lines.add(HudLine.of(
                new HudText("Health", LABEL),
                new HudText(health + "/" + maxHealth, healthColor)));

        lines.add(HudLine.of(
                new HudText("Armor", LABEL),
                new HudText(String.valueOf(player.getArmorValue()), ColorMaker.staticColor(85, 170, 255))));

        lines.add(HudLine.of(
                new HudText("Hunger", LABEL),
                new HudText(String.valueOf(player.getFoodData().getFoodLevel()), ColorMaker.staticColor(255, 170, 0))));

        lines.add(HudLine.of(
                new HudText("Saturation", LABEL),
                new HudText(String.format("%.1f", player.getFoodData().getSaturationLevel()), ColorMaker.staticColor(255, 255, 85))));

        return lines;
    }

    @Override
    public boolean sortByLength() {
        return true;
    }
}
