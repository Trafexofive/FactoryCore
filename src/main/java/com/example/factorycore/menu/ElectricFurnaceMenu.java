package com.example.factorycore.menu;

import com.example.factorycore.block.entity.ElectricFurnaceBlockEntity;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.registry.CoreMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    private final ElectricFurnaceBlockEntity blockEntity;
    private final ContainerData data;

    // Client Constructor
    public ElectricFurnaceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (ElectricFurnaceBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    // Server Constructor
    public ElectricFurnaceMenu(int containerId, Inventory inv, ElectricFurnaceBlockEntity entity, ContainerData data) {
        super(CoreMenus.ELECTRIC_FURNACE.get(), containerId);
        this.blockEntity = entity;
        this.data = data;
        
        ItemStackHandler handler = entity.getInventory();
        
        // Machine Input (Slot 0)
        this.addSlot(new SlotItemHandler(handler, 0, 56, 35));
        
        // Machine Output (Slot 1)
        this.addSlot(new SlotItemHandler(handler, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }
        
        this.addDataSlots(data);
    }

    public int getEnergy() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Standard shift-click logic (omitted for brevity, can add if requested)
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                .evaluate((level, pos) -> level.getBlockState(pos).is(CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get()), true);
    }
}
