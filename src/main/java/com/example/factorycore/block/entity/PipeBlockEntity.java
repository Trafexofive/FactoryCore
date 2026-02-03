package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PipeBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int transferCooldown = 0;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.PIPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        if (level.isClientSide) return;

        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        boolean workDone = false;

        // 1. Pull Logic (Try to fill the pipe)
        if (be.inventory.getStackInSlot(0).isEmpty()) {
            workDone = be.tryPull(level, pos);
        }

        // 2. Push Logic (Try to empty the pipe)
        if (!be.inventory.getStackInSlot(0).isEmpty()) {
            workDone |= be.tryPush(level, pos);
        }

        if (workDone) {
            be.transferCooldown = 4; // Transfer speed: 1 item every 0.2 seconds
            be.setChanged();
        }
    }

    private boolean tryPull(Level level, BlockPos pos) {
        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(dirs); // Randomize pull order for fairness

        BlockState state = level.getBlockState(pos);

        for (Direction dir : dirs) {
            // Only pull if this side is set to Extract
            net.minecraft.world.level.block.state.properties.BooleanProperty prop = com.example.factorycore.block.PipeBlock.getExtractProperty(dir);
            if (!state.hasProperty(prop) || !state.getValue(prop)) continue;

            BlockPos targetPos = pos.relative(dir);
            BlockEntity targetBE = level.getBlockEntity(targetPos);
            if (targetBE instanceof PipeBlockEntity) continue; // Don't pull from other pipes (push only)

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack extractable = handler.extractItem(i, 1, true);
                    if (!extractable.isEmpty()) {
                        ItemStack remainder = inventory.insertItem(0, extractable, false);
                        if (remainder.isEmpty()) {
                            handler.extractItem(i, 1, false);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean tryPush(Level level, BlockPos pos) {
        ItemStack stack = inventory.getStackInSlot(0);
        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(dirs);

        for (Direction dir : dirs) {
            BlockPos targetPos = pos.relative(dir);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
            if (handler != null) {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (remainder.getCount() < stack.getCount()) {
                    inventory.setStackInSlot(0, remainder);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    public IItemHandler getInventory() {
        return inventory;
    }
}
