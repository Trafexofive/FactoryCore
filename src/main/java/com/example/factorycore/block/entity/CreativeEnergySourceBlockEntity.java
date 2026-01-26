package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CreativeEnergySourceBlockEntity extends BlockEntity {
    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return maxExtract; }
        @Override public int getEnergyStored() { return 1000000; }
        @Override public int getMaxEnergyStored() { return 1000000; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    public CreativeEnergySourceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.CREATIVE_ENERGY_SOURCE.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.CREATIVE_ENERGY_SOURCE.get(),
            (be, side) -> be.energy
        );
    }
}
