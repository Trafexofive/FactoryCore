package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SolarPanelBlockEntity extends BlockEntity {
    private final SolarEnergyStorage energyStorage;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.SOLAR_PANEL.get(), pos, state);
        this.energyStorage = new SolarEnergyStorage(10000, 0, 1000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        if (level.isClientSide) return;

        // 1. GENERATION LOGIC: Purely based on Sky Light
        int skyLight = level.getBrightness(LightLayer.SKY, pos.above());
        if (skyLight > 0) {
            int generated = skyLight * 4;
            be.energyStorage.generate(generated);
        }

        // 2. PUSH LOGIC
        be.pushEnergy();
    }

    private void pushEnergy() {
        if (energyStorage.getEnergyStored() <= 0) return;

        // Try pushing DOWN first (preferred)
        if (attemptPush(Direction.DOWN)) return;

        // Fallback: Try all other sides
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            if (attemptPush(dir)) break;
        }
    }

    private boolean attemptPush(Direction dir) {
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir), dir.getOpposite());
        if (target != null && target.canReceive()) {
            int toPush = energyStorage.extractEnergy(1000, true);
            int accepted = target.receiveEnergy(toPush, false);
            energyStorage.extractEnergy(accepted, false);
            return accepted > 0;
        }
        return false;
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

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    private class SolarEnergyStorage extends EnergyStorage {
        public SolarEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void generate(int amount) {
            int oldEnergy = this.energy;
            this.energy = Math.min(capacity, this.energy + amount);
            if (this.energy != oldEnergy) setChanged();
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) setChanged();
            return extracted;
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