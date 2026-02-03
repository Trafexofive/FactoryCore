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

public class SolarPanelBlockEntity extends BlockEntity {
    private final SolarEnergyStorage energyStorage = new SolarPanelBlockEntity.SolarEnergyStorage(1000, 0, 1000);

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        if (level.isClientSide)
            return;

        // Generation logic
        if (level.isDay() && !level.isRaining()) {
            if (level.canSeeSkyFromBelowWater(pos.above())) {
                be.energyStorage.generate(16); // Direct generation
            }
        }

        // Push logic - Push DOWN
        if (be.energyStorage.getEnergyStored() > 0) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.below(), Direction.UP);
            if (target != null && target.canReceive()) {
                int toPush = be.energyStorage.extractEnergy(1000, true); // Simulate
                int accepted = target.receiveEnergy(toPush, false); // Actual push
                be.energyStorage.extractEnergy(accepted, false); // Actual extract
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
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
    }

    // Capability provider helper (if we used the old system, but NeoForge uses
    // event/cache)
    // We'll rely on the capability registration in ModSetup if needed, but for
    // BlockEntities
    // we usually expose it via getCapability or the new API.
    // For NeoForge 1.21, we register capabilities in the Mod event bus.

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    private static class SolarEnergyStorage extends EnergyStorage {
        public SolarEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void generate(int amount) {
            this.energy = Math.min(capacity, this.energy + amount);
        }
    }
}
