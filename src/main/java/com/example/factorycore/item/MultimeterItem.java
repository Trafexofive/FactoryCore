package com.example.factorycore.item;

import com.example.factorycore.block.entity.AbstractFactoryMultiblockBlockEntity;
import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

public class MultimeterItem extends Item {
    public MultimeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockEntity be = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);

        // Header
        player.sendSystemMessage(
                Component.literal("═══════════════════════════════").withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(
                Component.literal("  ⚡ MULTIMETER READING").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("  Position: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(
                Component.literal("═══════════════════════════════").withStyle(ChatFormatting.DARK_AQUA));

        // Block Info
        String blockName = state.getBlock().getName().getString();
        player.sendSystemMessage(Component.literal("Block: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(blockName).withStyle(ChatFormatting.YELLOW)));

        // Block State Properties
        if (!state.getValues().isEmpty()) {
            MutableComponent propsLine = Component.literal("Properties: ").withStyle(ChatFormatting.GRAY);
            state.getValues().forEach((prop, value) -> {
                propsLine.append(
                        Component.literal(prop.getName() + "=" + value + " ").withStyle(ChatFormatting.DARK_GRAY));
            });
            player.sendSystemMessage(propsLine);
        }

        // Energy Check (Capability)
        IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, context.getClickedFace());
        if (energy != null) {
            int stored = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();
            float percent = max > 0 ? (stored * 100f / max) : 0;

            ChatFormatting color;
            if (percent >= 75)
                color = ChatFormatting.GREEN;
            else if (percent >= 25)
                color = ChatFormatting.YELLOW;
            else
                color = ChatFormatting.RED;

            player.sendSystemMessage(Component.literal("Energy: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(formatEnergy(stored)).withStyle(color))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(formatEnergy(max)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" FE").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" (" + String.format("%.1f", percent) + "%)")
                            .withStyle(ChatFormatting.GRAY)));
        }

        // Block Entity Info
        if (be != null) {
            player.sendSystemMessage(Component.literal("Type: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(be.getClass().getSimpleName()).withStyle(ChatFormatting.WHITE)));

            // Multiblock Info
            if (be instanceof AbstractFactoryMultiblockBlockEntity multiblock) {
                boolean formed = multiblock.isFormed();
                player.sendSystemMessage(Component.literal("Multiblock: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(formed
                                ? Component.literal("✓ FORMED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                : Component.literal("✗ INCOMPLETE").withStyle(ChatFormatting.RED)));
                
                if (formed) {
                    // Show basic stats (can be expanded per BE)
                    if (be instanceof com.example.factorycore.block.entity.ElectricFurnaceBlockEntity) {
                        player.sendSystemMessage(Component.literal("  Consumption: ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal("100 FE/t").withStyle(ChatFormatting.WHITE)));
                        player.sendSystemMessage(Component.literal("  Speed: ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal("1.0x").withStyle(ChatFormatting.WHITE)));
                    }
                }
            }

            // Electrical Pole Info
            if (be instanceof ElectricalPoleBlockEntity pole) {
                int connections = pole.getConnections().size();
                player.sendSystemMessage(Component.literal("Connections: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(connections + "/5").withStyle(
                                connections >= 5 ? ChatFormatting.YELLOW : ChatFormatting.WHITE)));

                // Show connected positions
                if (!pole.getConnections().isEmpty()) {
                    for (BlockPos conn : pole.getConnections()) {
                        double dist = Math.sqrt(pos.distSqr(conn));
                        player.sendSystemMessage(Component.literal("  → ")
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal(conn.toShortString()).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" (" + String.format("%.1f", dist) + " blocks)")
                                        .withStyle(ChatFormatting.GRAY)));
                    }
                }
            }
        }

        // Network Info
        ElectricalNetwork network = FactoryNetworkManager.get(level).getNetworkAt(pos);
        if (network != null) {
            player.sendSystemMessage(Component.literal("Network ID: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("#" + network.getId()).withStyle(ChatFormatting.LIGHT_PURPLE)));
            player.sendSystemMessage(Component.literal("Network Size: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(network.getMembers().size() + " blocks")
                            .withStyle(ChatFormatting.WHITE)));
        }

        player.sendSystemMessage(
                Component.literal("═══════════════════════════════").withStyle(ChatFormatting.DARK_AQUA));

        return InteractionResult.SUCCESS;
    }

    private String formatEnergy(int energy) {
        if (energy >= 1_000_000_000) {
            return String.format("%.2fG", energy / 1_000_000_000.0);
        } else if (energy >= 1_000_000) {
            return String.format("%.2fM", energy / 1_000_000.0);
        } else if (energy >= 1_000) {
            return String.format("%.1fK", energy / 1_000.0);
        }
        return String.valueOf(energy);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.literal("Right-click blocks to diagnose").withStyle(ChatFormatting.GRAY));
        tooltipComponents
                .add(Component.literal("Shows: Energy, Multiblock, Network").withStyle(ChatFormatting.DARK_GRAY));
    }
}
