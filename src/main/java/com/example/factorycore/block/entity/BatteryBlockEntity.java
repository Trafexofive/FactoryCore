package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryBlockEntity extends BlockEntity {
    private final BatteryStorage energyStorage = new BatteryStorage(100000, 1000, 1000);

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.BATTERY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        if (level.isClientSide)
            return;

        if (be.energyStorage.getEnergyStored() > 0) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.below(), Direction.UP);
            if (target != null && target.canReceive()) {
                int toPush = be.energyStorage.extractEnergy(1000, true);
                int accepted = target.receiveEnergy(toPush, false);
                be.energyStorage.extractEnergy(accepted, false);
                if (accepted > 0)
                    be.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
    }

    public BatteryStorage getEnergyStorage() {
        return energyStorage;
    }

    public static class BatteryStorage extends EnergyStorage {
        public BatteryStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = energy;
        }
    }
}
