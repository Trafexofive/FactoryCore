package com.example.factorycore.menu;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;

public class BatteryMenu extends ModularUIContainerMenu {

    public BatteryMenu(MenuType<?> type, int containerId, Inventory playerInventory, IContainerUIHolder uiHolder) {
        super((MenuType)type, containerId, playerInventory, uiHolder);
    }

    public BatteryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super((MenuType)com.example.factorycore.registry.CoreMenus.BATTERY_MENU.get(), containerId, playerInventory, getHolder(playerInventory, buf));
    }

    private static IContainerUIHolder getHolder(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.decode(buf);
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof IContainerUIHolder holder) {
            return holder;
        }
        return null;
    }
}
