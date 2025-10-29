package com.example.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.boze.api.addon.AddonCommand;
import dev.boze.api.utility.ChatHelper;
import dev.boze.api.utility.input.Bind;
import dev.boze.api.client.module.BaseModule;
import dev.boze.api.option.BindOption;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.Option;
import dev.boze.api.option.ParentOption;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import net.minecraft.command.CommandSource;

import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PrintOptionsCommand extends AddonCommand {
    public static final PrintOptionsCommand INSTANCE = new PrintOptionsCommand();

    private PrintOptionsCommand() {
        super("printoptions", "Print a module's options and their current values");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", BaseModule.BaseModuleArgument.module())
                .executes(context -> {
                    BaseModule module = BaseModule.BaseModuleArgument.getModule(context, "module");

                    List<Option<?>> options = module.getOptions();

                    if (options.isEmpty()) {
                        ChatHelper.sendMsg("printoptions", module.getTitle() + " has no options.");
                        return SINGLE_SUCCESS;
                    }

                    StringBuilder message = new StringBuilder();
                    message.append(module.getTitle()).append(" Options:\n");

                    for (int i = 0; i < options.size(); i++) {
                        Option<?> option = options.get(i);
                        message.append("  ").append(option.getFullName()).append(": ");

                        if (option instanceof ToggleOption toggleOption) {
                            message.append(toggleOption.getValue() ? "Enabled" : "Disabled");
                        } else if (option instanceof SliderOption sliderOption) {
                            message.append(String.format("%.2f (%.2f - %.2f)",
                                sliderOption.getValue(), sliderOption.min, sliderOption.max));
                        } else if (option instanceof ModeOption<?> modeOption) {
                            message.append(modeOption.getValueName());
                        } else if (option instanceof BindOption bindOption) {
                            Bind bind = bindOption.getValue();
                            if (bind.getBind() == -1) {
                                message.append("None");
                            } else {
                                message.append(bind.isButton() ? "Mouse " : "Key ").append(bind.getBind());
                            }
                        } else if (option instanceof ParentOption) {
                            message.append("Parent Container");
                        } else {
                            // Fallback for unknown option types
                            message.append(option.getValue().toString());
                        }

                        if (i < options.size() - 1) {
                            message.append("\n");
                        }
                    }

                    ChatHelper.sendMsg("printoptions", message.toString());
                    return SINGLE_SUCCESS;
                }));
    }
}
