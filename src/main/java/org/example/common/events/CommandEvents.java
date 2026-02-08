package org.example.common.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.MHWeaponsMod;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("null")
public final class CommandEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mhdeco")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("setslots")
                        .then(Commands.argument("slots", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ItemStack stack = player.getMainHandItem();
                                    int[] slots = parseSlots(StringArgumentType.getString(ctx, "slots"));
                                    if (slots.length == 0) {
                                        ctx.getSource().sendFailure(Component.literal("No slots parsed. Example: /mhdeco setslots 1 2 3"));
                                        return 0;
                                    }
                                    DecorationUtil.setSlots(stack, slots);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set decoration slots: " + Arrays.toString(slots)), true);
                                    return 1;
                                })))
                .then(Commands.literal("install")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .then(Commands.argument("decoration", StringArgumentType.string())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ItemStack stack = player.getMainHandItem();
                                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                            String decoIdStr = StringArgumentType.getString(ctx, "decoration");
                                            ResourceLocation decoId = ResourceLocation.tryParse(decoIdStr);
                                            if (decoId == null || DecorationDataManager.INSTANCE.get(decoId) == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown decoration: " + decoIdStr));
                                                return 0;
                                            }
                                            boolean ok = DecorationUtil.installDecoration(stack, slot, decoId);
                                            if (!ok) {
                                                ctx.getSource().sendFailure(Component.literal("Failed to install decoration (slot invalid, size too small, or occupied)."));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Installed " + decoId + " in slot " + slot), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ItemStack stack = player.getMainHandItem();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                    boolean ok = DecorationUtil.removeDecoration(stack, slot);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("No decoration found in slot " + slot));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("Removed decoration from slot " + slot), true);
                                    return 1;
                                })))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            DecorationUtil.clearDecorations(player.getMainHandItem());
                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared all decorations on held item."), true);
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ItemStack stack = player.getMainHandItem();
                            var slots = DecorationUtil.getSlots(stack);
                            var decos = DecorationUtil.getDecorations(stack);
                            String slotStr = Arrays.toString(slots);
                            String decoStr = decos.isEmpty()
                                    ? "(none)"
                                    : decos.stream().map(d -> "slot " + d.slot() + ":" + d.id()).collect(Collectors.joining(", "));
                            ctx.getSource().sendSuccess(() -> Component.literal("Slots " + slotStr + " | Decorations " + decoStr), false);
                            return 1;
                        }))
        );
    }

    private int[] parseSlots(String raw) {
        String cleaned = raw.replace(',', ' ');
        String[] parts = cleaned.trim().split("\\s+");
        int[] slots = new int[parts.length];
        int count = 0;
        for (String p : parts) {
            if (p.isBlank()) {
                continue;
            }
            try {
                int v = Integer.parseInt(p);
                if (v > 0) {
                    slots[count++] = v;
                }
            } catch (NumberFormatException ex) {
                MHWeaponsMod.LOGGER.warn("Invalid slot size '{}'", p);
            }
        }
        return Arrays.copyOf(slots, count);
    }
}