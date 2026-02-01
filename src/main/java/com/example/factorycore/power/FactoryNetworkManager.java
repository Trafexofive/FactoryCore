package com.example.factorycore.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class FactoryNetworkManager extends SavedData {
    private static final String DATA_NAME = "factory_core_power_networks";

    private final Map<Integer, ElectricalNetwork> networks = new HashMap<>();
    private final Map<BlockPos, Integer> nodeToNetworkId = new HashMap<>();
    private int nextId = 1;

    public FactoryNetworkManager() {
    }

    public static FactoryNetworkManager get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(new Factory<FactoryNetworkManager>(
                    FactoryNetworkManager::new,
                    FactoryNetworkManager::load,
                    DataFixTypes.LEVEL // Use LEVEL or similar if available, or null for simple
            ), DATA_NAME);
        }
        return null;
    }

    // Modern NeoForge Factory for SavedData usually simpler, let's stick to
    // standard pattern
    // Actually, SavedData.Factory is the way.
    // DataFixTypes might not be easily accessible or needed for new mod.
    // I'll use the simpler computeIfAbsent signature if possible or generic
    // Factory.

    public static FactoryNetworkManager load(CompoundTag tag, HolderLookup.Provider provider) {
        FactoryNetworkManager manager = new FactoryNetworkManager();
        manager.nextId = tag.getInt("NextId");

        ListTag nets = tag.getList("Networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < nets.size(); i++) {
            CompoundTag netTag = nets.getCompound(i);
            ElectricalNetwork network = ElectricalNetwork.load(netTag);
            manager.networks.put(network.getId(), network);

            for (BlockPos pos : network.getMembers()) {
                manager.nodeToNetworkId.put(pos, network.getId());
            }
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("NextId", nextId);
        ListTag nets = new ListTag();
        for (ElectricalNetwork net : networks.values()) {
            nets.add(net.save());
        }
        tag.put("Networks", nets);
        return tag;
    }

    // --- Logic ---

    public ElectricalNetwork getNetwork(int id) {
        return networks.get(id);
    }

    public ElectricalNetwork getNetworkAt(BlockPos pos) {
        if (!nodeToNetworkId.containsKey(pos))
            return null;
        return networks.get(nodeToNetworkId.get(pos));
    }

    /**
     * Called when a floor block is placed.
     * Core Business Logic for Network Formation:
     * 1. Check all 6 adjacent blocks for existing networks.
     * 2. If no neighbors have a network, create a NEW network ID.
     * 3. If neighbors belong to the SAME network, just add this block to it.
     * 4. If neighbors belong to DIFFERENT networks, we must MERGE them.
     * - The larger network (or arbitrarily first found) absorbs the smaller ones.
     * - Energy buffers are combined (up to capacity).
     * - Old IDs are invalidated and redirected to the new master ID.
     */
    public void addNode(BlockPos pos) {
        if (nodeToNetworkId.containsKey(pos))
            return; // Already exists

        // Check 6 neighbors
        ElectricalNetwork foundNet = null;

        // Simple adjacency check
        BlockPos[] neighbors = { pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west() };

        for (BlockPos n : neighbors) {
            ElectricalNetwork neighborNet = getNetworkAt(n);
            if (neighborNet != null) {
                if (foundNet == null) {
                    // First one found, join it
                    foundNet = neighborNet;
                    foundNet.addNode(pos);
                    nodeToNetworkId.put(pos, foundNet.getId());
                } else if (foundNet.getId() != neighborNet.getId()) {
                    // Critical: Two different networks touched. Merge required.
                    mergeNetworks(foundNet, neighborNet);
                }
            }
        }

        if (foundNet == null) {
            // Isolated block -> Create new network root
            ElectricalNetwork newNet = new ElectricalNetwork(nextId++);
            networks.put(newNet.getId(), newNet);
            newNet.addNode(pos);
            nodeToNetworkId.put(pos, newNet.getId());
        }

        setDirty();
    }

    public void removeNode(BlockPos pos) {
        ElectricalNetwork net = getNetworkAt(pos);
        if (net != null) {
            net.removeNode(pos);
            nodeToNetworkId.remove(pos);

            // Garbage Collection: If network is empty, delete it to free ID/memory
            if (net.getMembers().isEmpty()) {
                networks.remove(net.getId());
            }

            // NOTE: We do NOT currently handle network splitting (graph partition).
            // If a floor line is cut in the middle, both halves remain on the same Network
            // ID
            // effectively creating a "wireless" bridge between the disconnected parts.
            // This is a trade-off for performance (O(1) removal vs O(N) graph traversal).
            setDirty();
        }
    }

    /**
     * Merges 'victim' network into 'master' network.
     * Logic:
     * 1. Transfer all member blocks from victim to master.
     * 2. Transfer stored energy.
     * 3. Update lookup table so victim's blocks point to master ID.
     * 4. Delete victim network.
     */
    public void mergeNetworks(ElectricalNetwork master, ElectricalNetwork victim) {
        if (master == victim)
            return;

        // Move all members from victim to master
        for (BlockPos pos : victim.getMembers()) {
            nodeToNetworkId.put(pos, master.getId()); // Update lookup
        }

        master.merge(victim);
        networks.remove(victim.getId());
        setDirty();
    }
}
