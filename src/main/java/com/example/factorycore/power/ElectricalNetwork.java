package com.example.factorycore.power;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.HashSet;
import java.util.Set;

public class ElectricalNetwork {
    private final int id;
    private final Set<BlockPos> members = new HashSet<>();
    
    // The shared energy buffer for the entire island.
    // All connected machines pull from this single object.
    private final EnergyStorage energyBuffer;
    private boolean dirty = false;

    public ElectricalNetwork(int id) {
        this.id = id;
        // 1M FE buffer per network, max transfer 10k/t
        this.energyBuffer = new EnergyStorage(1000000, 10000, 10000) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int r = super.receiveEnergy(maxReceive, simulate);
                // Mark dirty only if energy actually changed
                if (r > 0 && !simulate) dirty = true;
                return r;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int r = super.extractEnergy(maxExtract, simulate);
                if (r > 0 && !simulate) dirty = true;
                return r;
            }
        };
    }

    public int getId() {
        return id;
    }

    public void addNode(BlockPos pos) {
        if (members.add(pos)) {
            dirty = true;
        }
    }

    public void removeNode(BlockPos pos) {
        if (members.remove(pos)) {
            dirty = true;
        }
    }

    public Set<BlockPos> getMembers() {
        return members;
    }

    public EnergyStorage getStorage() {
        return energyBuffer;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setClean() {
        this.dirty = false;
    }

    public void merge(ElectricalNetwork other) {
        // Absorb members
        this.members.addAll(other.members);
        // Absorb energy
        int energy = other.energyBuffer.getEnergyStored();
        // Force inject energy even beyond limit temporarily if needed, or cap it?
        // Let's cap it to max capacity to be safe
        int space = this.energyBuffer.getMaxEnergyStored() - this.energyBuffer.getEnergyStored();
        int toAdd = Math.min(space, energy);
        this.energyBuffer.receiveEnergy(toAdd, false);
        
        this.dirty = true;
    }

    // Serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putInt("Energy", energyBuffer.getEnergyStored());
        
        ListTag memberList = new ListTag();
        for (BlockPos pos : members) {
            memberList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("Members", memberList);
        return tag;
    }

    public static ElectricalNetwork load(CompoundTag tag) {
        ElectricalNetwork net = new ElectricalNetwork(tag.getInt("Id"));
        // Direct set energy (bypass restrictions for loading)
        int energy = tag.getInt("Energy");
        net.energyBuffer.receiveEnergy(energy, false); 
        
        ListTag memberList = tag.getList("Members", 10);
        for (int i = 0; i < memberList.size(); i++) {
            NbtUtils.readBlockPos(memberList.getCompound(i), "Rel").ifPresent(net.members::add); // Handle legacy Rel? NbtUtils uses standard format
            // NbtUtils.readBlockPos expects simple compound
            net.members.add(NbtUtils.readBlockPos(memberList.getCompound(i), "P").orElse(BlockPos.ZERO));
        }
        return net;
    }
    
    // Custom loader for NbtUtils weirdness if needed, but standard is fine
    public void loadMembers(ListTag list) {
        for (int i=0; i<list.size(); i++) {
            NbtUtils.readBlockPos(list.getCompound(i), "P").ifPresent(members::add);
        }
    }
}
