package com.example.factorycore.block.entity;

import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class AbstractFactoryMultiblockBlockEntity extends BlockEntity {
    protected boolean isFormed = false;
    protected int checkTimer = 0;
    
    // Default 1 slot input, 1 slot output for simple machines
    protected final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public AbstractFactoryMultiblockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract MultiblockPattern getPattern();
    
    // Origin is where the controller is relative to the pattern
    // Usually (1, 1, 0) for a 3x3x3 front-center controller
    public abstract BlockPos getPatternOffset();

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractFactoryMultiblockBlockEntity be) {
        if (level.isClientSide) return;

        if (be.checkTimer++ >= 40) {
            be.checkTimer = 0;
            boolean wasFormed = be.isFormed;
            be.isFormed = be.checkStructure();
            if (wasFormed != be.isFormed) {
                be.onFormationChanged(be.isFormed);
            }
        }

        if (be.isFormed) {
            be.serverTick();
        }
    }

    protected boolean checkStructure() {
        if (level == null) return false;
        // The origin for the pattern matching is controllerPos - offset
        BlockPos origin = worldPosition.subtract(getPatternOffset());
        return getPattern().matches(level, origin);
    }

    protected void onFormationChanged(boolean formed) {
        setChanged();
        // Update blockstate for visual feedback?
    }

    protected abstract void serverTick();

    /**
     * Helper to get energy from the block directly below the controller.
     */
    protected IEnergyStorage getFloorEnergy() {
        if (level == null) return null;
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.below(), Direction.UP);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", isFormed);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isFormed = tag.getBoolean("Formed");
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }
}
