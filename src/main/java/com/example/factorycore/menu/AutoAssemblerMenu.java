package com.example.factorycore.menu;

import com.example.factorycore.block.entity.AutoAssemblerBlockEntity;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.registry.CoreMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AutoAssemblerMenu extends AbstractContainerMenu {
    private final AutoAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public AutoAssemblerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (AutoAssemblerBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public AutoAssemblerMenu(int containerId, Inventory inv, AutoAssemblerBlockEntity entity, ContainerData data) {
        super(CoreMenus.AUTO_ASSEMBLER.get(), containerId);
        this.blockEntity = entity;
        this.data = data;

        ItemStackHandler handler = entity.getInventory();

        // Add 3x3 crafting grid (slots 0-8)
        int gridStartX = 30;
        int gridStartY = 17;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(handler, row * 3 + col, gridStartX + col * 18, gridStartY + row * 18));
            }
        }

        // Add output slot (slot 9)
        this.addSlot(new SlotItemHandler(handler, 9, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output slot - items can't be placed in it
            }
        });

        // Player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Define slot ranges
            int machineSlots = 10; // 9 crafting slots + 1 output slot
            int playerInventoryStart = machineSlots;
            int playerHotbarStart = playerInventoryStart + 27; // 27 inventory slots
            int totalSlots = playerHotbarStart + 9; // 9 hotbar slots

            if (index < machineSlots) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(itemstack1, playerInventoryStart, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to machine
                // Try to put into crafting grid first (slots 0-8), then to hotbar if output slot
                if (index < playerHotbarStart) {
                    // From main inventory to crafting grid
                    if (!this.moveItemStackTo(itemstack1, 0, 9, false)) { // Only to crafting grid (slots 0-8)
                        return ItemStack.EMPTY;
                    }
                } else {
                    // From hotbar to crafting grid
                    if (!this.moveItemStackTo(itemstack1, 0, 9, false)) { // Only to crafting grid (slots 0-8)
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    public int getEnergy() { return data.get(0); }

    public int getProgressScale() {
        int progress = data.get(1);
        int maxProgress = 200; 
        return progress != 0 ? progress * 24 / maxProgress : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                .evaluate((level, pos) -> level.getBlockState(pos).is(CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get()), true);
    }
}
