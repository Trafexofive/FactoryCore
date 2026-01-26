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

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isFormed() {
        return isFormed;
    }

    /**
     * Ticking Logic:
     * 1. Check Structure periodicially (every 2 seconds/40 ticks) to save perf.
     * 2. If Formed -> Run serverTick() (Business Logic).
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractFactoryMultiblockBlockEntity be) {
        if (level.isClientSide) return;

        if (be.checkTimer++ >= 20) {
            be.checkTimer = 0;
            be.recheckStructure();
        }

        if (be.isFormed) {
            be.serverTick();
        }
    }

    public void recheckStructure() {
        boolean wasFormed = this.isFormed;
        this.isFormed = this.checkStructure();
        if (wasFormed != this.isFormed) {
            this.onFormationChanged(this.isFormed);
        }
    }

    /**
     * Validates the multiblock structure against the defined pattern.
     * @return true if the structure is complete and valid.
     */
    protected boolean checkStructure() {
        if (level == null) return false;
        
        BlockState state = getBlockState();
        net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }

        // The origin for the pattern matching is the controller itself.
        return getPattern().matches(level, worldPosition, facing);
    }

    protected void onFormationChanged(boolean formed) {
        setChanged();
        if (formed && level != null) {
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
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
