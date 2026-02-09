package com.example.factorycore.menu;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;

public class ElectricFurnaceMenu extends ModularUIContainerMenu {

    public ElectricFurnaceMenu(MenuType<?> type, int containerId, Inventory playerInventory, IContainerUIHolder uiHolder) {
        super((MenuType)type, containerId, playerInventory, uiHolder);
    }

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super((MenuType)com.example.factorycore.registry.CoreMenus.ELECTRIC_FURNACE_MENU.get(), containerId, playerInventory, getHolder(playerInventory, buf));
    }

    private static IContainerUIHolder getHolder(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.decode(buf);
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof IContainerUIHolder holder) {
            return holder;
        }
        // Fallback to prevent crash, returning a dummy UI holder with matching slot count
        return new IContainerUIHolder() {
            @Override
            public com.lowdragmc.lowdraglib2.gui.ui.ModularUI createUI(net.minecraft.world.entity.player.Player player) {
                com.lowdragmc.lowdraglib2.gui.ui.UI ui = com.lowdragmc.lowdraglib2.gui.ui.UI.empty();
                // Add 2 dummy slots for the machine
                ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot().bind(new net.neoforged.neoforge.items.ItemStackHandler(2), 0));
                ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot().bind(new net.neoforged.neoforge.items.ItemStackHandler(2), 1));
                // Add player inventory (36 slots)
                ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots());
                return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(ui, player);
            }
            @Override
            public boolean isStillValid(net.minecraft.world.entity.player.Player player) {
                return false; // Close as soon as possible
            }
        };
    }
}
