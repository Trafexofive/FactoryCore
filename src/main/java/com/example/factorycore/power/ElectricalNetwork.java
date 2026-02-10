package com.example.factorycore.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElectricalNetwork {
    private final int id;
    private final Set<BlockPos> members = new HashSet<>();
    
    // The shared energy buffer for the entire island.
    private final EnergyStorage energyBuffer;
    private boolean dirty = false;
    private BlockPos assignedPole = null;

    // --- Caches for Zero-Allocation Ticking ---
    // Maps coordinate -> Side -> Capability Cache
    private final Map<BlockPos, Map<Direction, net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>>> externalCaches = new java.util.concurrent.ConcurrentHashMap<>();

    public ElectricalNetwork(int id) {
        this.id = id;
        // 1M FE buffer per network, max transfer 50k/t (Upgraded for industrial scale)
        this.energyBuffer = new EnergyStorage(1000000, 50000, 50000) {
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

    public int getId() { return id; }
    public Set<BlockPos> getMembers() { return members; }
    public EnergyStorage getStorage() { return energyBuffer; }

    public void tick(ServerLevel level) {
        if (members.isEmpty()) return;

        // Linear Pass over cached external FE blocks (Universal Compatibility)
        for (var entry : externalCaches.entrySet()) {
            BlockPos nodePos = entry.getKey();
            for (var sideEntry : entry.getValue().entrySet()) {
                IEnergyStorage external = sideEntry.getValue().getCapability();
                if (external == null) continue;

                // 1. PULL from external producers (e.g. Mekanism Cables, Solar Panels)
                if (external.canExtract()) {
                    int space = energyBuffer.getMaxEnergyStored() - energyBuffer.getEnergyStored();
                    if (space > 0) {
                        int pulled = external.extractEnergy(Math.min(space, 10000), false);
                        energyBuffer.receiveEnergy(pulled, false);
                    }
                }

                // 2. PUSH to external consumers (e.g. Machines, Batteries)
                if (external.canReceive()) {
                    int available = energyBuffer.getEnergyStored();
                    if (available > 0) {
                        int toPush = energyBuffer.extractEnergy(Math.min(available, 10000), true);
                        int accepted = external.receiveEnergy(toPush, false);
                        energyBuffer.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    /**
     * Refreshes external connections for a specific node in the network.
     * Called when a neighbor changes or a node is added.
     */
    public void refreshNode(ServerLevel level, BlockPos pos) {
        Map<Direction, net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>> nodeMap = 
            externalCaches.computeIfAbsent(pos, k -> new java.util.EnumMap<>(Direction.class));

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            // Ignore other nodes in the same network to prevent self-looping
            if (members.contains(neighborPos)) {
                nodeMap.remove(dir);
                continue;
            }

            // Create or update cache for external FE block
            nodeMap.put(dir, net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                level, neighborPos, dir.getOpposite()
            ));
        }
    }

    public void addNode(ServerLevel level, BlockPos pos) {
        if (members.add(pos)) {
            refreshNode(level, pos);
            dirty = true;
        }
    }

    public void removeNode(BlockPos pos) {
        if (members.remove(pos)) {
            externalCaches.remove(pos);
            dirty = true;
            if (assignedPole != null && assignedPole.equals(pos)) {
                assignedPole = null;
            }
        }
    }

    public void merge(ServerLevel level, ElectricalNetwork other) {
        if (other == this) return;
        this.members.addAll(other.members);
        this.externalCaches.putAll(other.externalCaches);
        
        int energy = other.energyBuffer.getEnergyStored();
        int space = this.energyBuffer.getMaxEnergyStored() - this.energyBuffer.getEnergyStored();
        int toAdd = Math.min(space, energy);
        this.energyBuffer.receiveEnergy(toAdd, false);
        
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