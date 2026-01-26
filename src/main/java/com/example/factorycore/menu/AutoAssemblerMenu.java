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
        
        this.addSlot(new SlotItemHandler(handler, 0, 56, 35));
        this.addSlot(new SlotItemHandler(handler, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

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

    public int getEnergy() { return data.get(0); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                .evaluate((level, pos) -> level.getBlockState(pos).is(CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get()), true);
    }
}
