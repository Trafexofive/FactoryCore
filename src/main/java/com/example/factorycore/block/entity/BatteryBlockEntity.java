package com.example.factorycore.block.entity;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class BatteryBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements IContainerUIHolder {
    protected final EnergyStorage energyStorage;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.BATTERY.get(), pos, state);
        // Explicitly set capacity, maxReceive, and maxExtract to 1M
        this.energyStorage = new EnergyStorage(1000000, 1000000, 1000000); 
    }

    @Override
    public ModularUI createUI(Player player) {
        return ModularUI.of(UI.empty(), player);
    }

    @Override
    public boolean isStillValid(Player player) {
        return true;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
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
}
