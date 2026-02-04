package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.ui.FactoryUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class BatteryBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUI {
    protected final EnergyStorage energyStorage;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.BATTERY.get(), pos, state);
        this.energyStorage = new EnergyStorage(1000000, 1000000, 1000000); 
    }

    @Override
    public ModularUI createUI(com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder holder) {
        UI ui = UI.empty();
        ui.getRootElement().addChild(new Label().setValue(net.minecraft.network.chat.Component.literal("Battery")).layout(l -> FactoryUI.margin(l, 5f, 0f, 5f, 0f)));
        
        ui.getRootElement().addChild(new ProgressBar().bindDataSource(FactoryUI.supplier(() -> (float) energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored()))
                .layout(l -> FactoryUI.apply(l, 83f, 20f, 10f, 50f)));
        
        return ModularUI.of(ui, holder.player);
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
