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
        // Fallback to prevent crash, returning an empty UI holder
        return new IContainerUIHolder() {
            @Override
            public com.lowdragmc.lowdraglib2.gui.ui.ModularUI createUI(net.minecraft.world.entity.player.Player player) {
                return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(com.lowdragmc.lowdraglib2.gui.ui.UI.empty(), player);
            }
            @Override
            public boolean isStillValid(net.minecraft.world.entity.player.Player player) {
                return false;
            }
        };
    }
}
