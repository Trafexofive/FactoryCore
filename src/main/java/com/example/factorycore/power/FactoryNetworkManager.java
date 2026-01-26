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

    public FactoryNetworkManager() {}

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

    // Modern NeoForge Factory for SavedData usually simpler, let's stick to standard pattern
    // Actually, SavedData.Factory is the way. 
    // DataFixTypes might not be easily accessible or needed for new mod. 
    // I'll use the simpler computeIfAbsent signature if possible or generic Factory.
    
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
        if (!nodeToNetworkId.containsKey(pos)) return null;
        return networks.get(nodeToNetworkId.get(pos));
    }

    /**
     * Called when a floor block is placed.
     * Checks neighbors.
     * 0 Neighbors -> New Network.
     * 1 Neighbor -> Join Network.
     * >1 Neighbors -> Merge Networks if different.
     */
    public void addNode(BlockPos pos) {
        if (nodeToNetworkId.containsKey(pos)) return; // Already exists

        // Check 6 neighbors
        ElectricalNetwork foundNet = null;
        
        // Simple adjacency check
        BlockPos[] neighbors = {pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()};
        
        for (BlockPos n : neighbors) {
            ElectricalNetwork neighborNet = getNetworkAt(n);
            if (neighborNet != null) {
                if (foundNet == null) {
                    // First one found, join it
                    foundNet = neighborNet;
                    foundNet.addNode(pos);
                    nodeToNetworkId.put(pos, foundNet.getId());
                } else if (foundNet.getId() != neighborNet.getId()) {
                    // Merge needed
                    mergeNetworks(foundNet, neighborNet);
                }
            }
        }

        if (foundNet == null) {
            // Create new
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
            
            if (net.getMembers().isEmpty()) {
                networks.remove(net.getId());
            }
            
            // TODO: Split detection logic would go here.
            // For now, we assume the graph stays connected or we accept "magic wireless" behavior within an ID.
            setDirty();
        }
    }

    private void mergeNetworks(ElectricalNetwork master, ElectricalNetwork victim) {
        if (master == victim) return;
        
        // Move all members from victim to master
        for (BlockPos pos : victim.getMembers()) {
            nodeToNetworkId.put(pos, master.getId()); // Update lookup
        }
        
        master.merge(victim);
        networks.remove(victim.getId());
        setDirty();
    }
}
