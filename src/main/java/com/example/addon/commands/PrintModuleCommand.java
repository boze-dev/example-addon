package com.example.addon.commands;

import com.example.addon.ExampleAddon;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.boze.api.addon.AddonCommand;
import dev.boze.api.utility.ChatHelper;
import dev.boze.api.client.module.BaseModule;
import net.minecraft.commands.SharedSuggestionProvider;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PrintModuleCommand extends AddonCommand {
    public static final PrintModuleCommand INSTANCE = new PrintModuleCommand();

    private PrintModuleCommand() {
        super("printmodule", "Print a module's internal state");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("module", BaseModule.BaseModuleArgument.module())
                .executes(context -> {
                    BaseModule module = BaseModule.BaseModuleArgument.getModule(context, "module");
                    ChatHelper.sendMsg(ExampleAddon.BRAND, ExampleAddon.BRAND_COLOR, "printmodule", String.format(
                            """
                                    Module Info:
                                      Name: %s
                                      Title: %s
                                      Description: %s
                                      State: %s
                                      Bind: %d
                                      Visible: %s
                                      Notify: %s
                                      Hold Bind: %s
                                      ArrayList Info: %s""",
                        module.getName(),
                        module.getTitle(),
                        module.getDescription(),
                        module.getState(),
                        module.getBind().getBind(),
                        module.isVisible(),
                        module.shouldNotify(),
                        module.isOnlyWhileHolding(),
                        module.getArrayListInfo()
                    ));
                    return SINGLE_SUCCESS;
                }));
    }
}
