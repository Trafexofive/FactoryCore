package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.ui.FactoryUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements net.minecraft.world.MenuProvider, com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUI, com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder {
    protected final EnergyStorage energyStorage;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.BATTERY.get(), pos, state);
        this.energyStorage = new EnergyStorage(1000000, 1000000, 1000000) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int accepted = super.receiveEnergy(maxReceive, simulate);
                if (accepted > 0 && !simulate) setChanged();
                return accepted;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (extracted > 0 && !simulate) setChanged();
                return extracted;
            }
        };
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal("Battery");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.example.factorycore.menu.BatteryMenu((net.minecraft.world.inventory.MenuType)com.example.factorycore.registry.CoreMenus.BATTERY_MENU.get(), windowId, playerInventory, this);
    }

    @Override
    public ModularUI createUI(Player player) {
        UI ui = UI.empty();
        ui.getRootElement().style(s -> s.background(MCSprites.RECT));
        ui.getRootElement().addChild(new Label().setValue(getDisplayName()).layout(l -> FactoryUI.margin(l, 5f, 0f, 5f, 0f)));
        
        ui.getRootElement().addChild(new ProgressBar().bindDataSource(FactoryUI.supplier(() -> (float) energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored()))
                .layout(l -> FactoryUI.apply(l, 83f, 20f, 10f, 50f)));
        
        return ModularUI.of(ui, player);
    }

    @Override
    public ModularUI createUI(com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder holder) {
        return createUI(holder.player);
    }

    @Override
    public boolean isStillValid(net.minecraft.world.entity.player.Player player) {
        return !isRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        if (level.isClientSide) return;

        // Automatically pull from floor below if not full
        if (be.energyStorage.getEnergyStored() < be.energyStorage.getMaxEnergyStored()) {
            IEnergyStorage floor = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.below(), Direction.UP);
            if (floor != null && floor.canExtract()) {
                int space = be.energyStorage.getMaxEnergyStored() - be.energyStorage.getEnergyStored();
                int extracted = floor.extractEnergy(Math.min(space, 10000), false);
                be.energyStorage.receiveEnergy(extracted, false);
            }
        }
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
            // Use internal set to bypass setChanged() during load
            this.energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (pkt.getTag() != null) loadAdditional(pkt.getTag(), registries);
    }
}