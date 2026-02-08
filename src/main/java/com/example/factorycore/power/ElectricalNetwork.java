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
    private final EnergyStorage energyBuffer;
    private boolean dirty = false;
    private BlockPos assignedPole = null;

    public ElectricalNetwork(int id) {
        this.id = id;
        // 1M FE buffer per network, max transfer 10k/t
        this.energyBuffer = new EnergyStorage(1000000, 10000, 10000) {
            @Override
            public synchronized int receiveEnergy(int maxReceive, boolean simulate) {
                int r = super.receiveEnergy(maxReceive, simulate);
                if (r > 0 && !simulate) dirty = true;
                return r;
            }

            @Override
            public synchronized int extractEnergy(int maxExtract, boolean simulate) {
                int r = super.extractEnergy(maxExtract, simulate);
                if (r > 0 && !simulate) dirty = true;
                return r;
            }
        };
    }

    public int getId() {
        return id;
    }

    public BlockPos getAssignedPole() {
        return assignedPole;
    }

    public void setAssignedPole(BlockPos assignedPole) {
        this.assignedPole = assignedPole;
        this.dirty = true;
    }

    public void tick(Level level) {
        if (energyBuffer.getEnergyStored() <= 0) return;

        for (BlockPos pos : members) {
            if (energyBuffer.getEnergyStored() <= 0) break;

            BlockPos machinePos = pos.above();
            
            // Query with DOWN face, then fallback to null side for generic machines
            net.neoforged.neoforge.energy.IEnergyStorage machineStorage = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 
                machinePos, 
                net.minecraft.core.Direction.DOWN
            );
            
            if (machineStorage == null) {
                machineStorage = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 
                    machinePos, 
                    null
                );
            }

            if (machineStorage != null && machineStorage.canReceive()) {
                int maxOutput = 1000; 
                int extracted = energyBuffer.extractEnergy(maxOutput, true); 
                if (extracted > 0) {
                    int accepted = machineStorage.receiveEnergy(extracted, false);
                    if (accepted > 0) {
                        energyBuffer.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    public void addNode(BlockPos pos) {
        if (members.add(pos)) {
            dirty = true;
        }
    }

    public void removeNode(BlockPos pos) {
        if (members.remove(pos)) {
            dirty = true;
            if (assignedPole != null && assignedPole.equals(pos)) {
                assignedPole = null;
            }
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
        if (other == this) return;
        this.members.addAll(other.members);
        int energy = other.energyBuffer.getEnergyStored();
        int space = this.energyBuffer.getMaxEnergyStored() - this.energyBuffer.getEnergyStored();
        int toAdd = Math.min(space, energy);
        this.energyBuffer.receiveEnergy(toAdd, false);
        
        // Preserve assigned pole if current one is null
        if (this.assignedPole == null) {
            this.assignedPole = other.assignedPole;
        }
        
        this.dirty = true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putInt("Energy", energyBuffer.getEnergyStored());
        if (assignedPole != null) {
            tag.putLong("AssignedPole", assignedPole.asLong());
        }
        
        ListTag memberList = new ListTag();
        for (BlockPos pos : members) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.put("P", NbtUtils.writeBlockPos(pos));
            memberList.add(memberTag);
        }
        tag.put("Members", memberList);
        return tag;
    }

    public static ElectricalNetwork load(CompoundTag tag) {
        ElectricalNetwork net = new ElectricalNetwork(tag.getInt("Id"));
        int energy = tag.getInt("Energy");
        net.energyBuffer.receiveEnergy(energy, false); 
        if (tag.contains("AssignedPole")) {
            net.assignedPole = BlockPos.of(tag.getLong("AssignedPole"));
        }
        
        ListTag memberList = tag.getList("Members", 10);
        for (int i = 0; i < memberList.size(); i++) {
            NbtUtils.readBlockPos(memberList.getCompound(i), "P").ifPresent(net.members::add);
        }
        return net;
    }
}