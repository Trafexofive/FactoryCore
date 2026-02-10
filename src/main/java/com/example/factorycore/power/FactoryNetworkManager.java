package com.example.factorycore.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.*;
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
        Integer id = nodeToNetworkId.get(pos);
        if (id == null) return null;
        ElectricalNetwork net = networks.get(id);
        if (net == null) {
            // Cleanup stale lookup
            nodeToNetworkId.remove(pos);
            return null;
        }
        return net;
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
    public void tick(ServerLevel level) {
        for (ElectricalNetwork net : networks.values()) {
            net.tick(level);
        }
    }

    public void addNode(ServerLevel level, BlockPos pos) {
        if (nodeToNetworkId.containsKey(pos)) return;

        Set<Integer> adjacentNetworks = new HashSet<>();
        
        // 1. Direct adjacency (Networks)
        for (Direction dir : Direction.values()) {
            ElectricalNetwork neighborNet = getNetworkAt(pos.relative(dir));
            if (neighborNet != null) adjacentNetworks.add(neighborNet.getId());
        }

        // 2. Long-Range Pole Connection (Networks)
        if (level.getBlockState(pos).getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock) {
            for (BlockPos otherPole : findPolesInRange(level, pos, 6)) {
                ElectricalNetwork otherNet = getNetworkAt(otherPole);
                if (otherNet != null) adjacentNetworks.add(otherNet.getId());
            }
        }

        ElectricalNetwork master = null;
        if (adjacentNetworks.isEmpty()) {
            master = new ElectricalNetwork(nextId++);
            networks.put(master.getId(), master);
        } else {
            Iterator<Integer> it = adjacentNetworks.iterator();
            master = getNetwork(it.next());
            while (it.hasNext()) {
                mergeNetworks(level, master, getNetwork(it.next()));
            }
        }

        master.addNode(level, pos);
        nodeToNetworkId.put(pos, master.getId());
        
        // 3. Visual Sync (Unified Scan)
        syncVisuals(level, pos, findAllVisualLinks(level, pos));
        
        setDirty();
    }

    private List<BlockPos> findAllVisualLinks(ServerLevel level, BlockPos pos) {
        List<BlockPos> visualLinks = new ArrayList<>();
        
        // 1. Direct neighbors (Floor & Machines)
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (dir == Direction.DOWN && level.getBlockState(neighbor).is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get())) {
                visualLinks.add(neighbor);
            }
            var cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite());
            if (cap != null && !(level.getBlockState(neighbor).getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock)) {
                visualLinks.add(neighbor);
            }
        }

        // 2. Long-Range Poles
        if (level.getBlockState(pos).getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock) {
            visualLinks.addAll(findPolesInRange(level, pos, 6));
        }
        
        return visualLinks;
    }

    private List<BlockPos> findPolesInRange(ServerLevel level, BlockPos pos, int range) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-range, -2, -range), pos.offset(range, 2, range))) {
            if (p.equals(pos)) continue;
            if (level.getBlockState(p).getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock) {
                found.add(p.immutable());
            }
        }
        return found;
    }

    private void syncVisuals(ServerLevel level, BlockPos pos, List<BlockPos> links) {
        var be = level.getBlockEntity(pos);
        if (be instanceof com.example.factorycore.block.entity.ElectricalPoleBlockEntity pole) {
            pole.getConnections().clear();
            for (BlockPos target : links) {
                pole.addVisualConnection(target);
                var targetBe = level.getBlockEntity(target);
                if (targetBe instanceof com.example.factorycore.block.entity.ElectricalPoleBlockEntity targetPole) {
                    targetPole.addVisualConnection(pos);
                    targetBe.setChanged();
                    level.sendBlockUpdated(target, targetBe.getBlockState(), targetBe.getBlockState(), 3);
                }
            }
            pole.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        }
    }

    public void removeNode(ServerLevel level, BlockPos pos) {
        ElectricalNetwork net = getNetworkAt(pos);
        if (net != null) {
            // Clean up visual links from neighbors first
            var be = level.getBlockEntity(pos);
            if (be instanceof com.example.factorycore.block.entity.ElectricalPoleBlockEntity pole) {
                for (BlockPos target : new HashSet<>(pole.getConnections())) {
                    var targetBe = level.getBlockEntity(target);
                    if (targetBe instanceof com.example.factorycore.block.entity.ElectricalPoleBlockEntity targetPole) {
                        targetPole.removeConnection(pos);
                        targetBe.setChanged();
                        level.sendBlockUpdated(target, targetBe.getBlockState(), targetBe.getBlockState(), 3);
                    }
                }
            }

            net.removeNode(pos);
            nodeToNetworkId.remove(pos);

            Set<BlockPos> members = new HashSet<>(net.getMembers());
            if (!members.isEmpty()) {
                for (BlockPos p : members) nodeToNetworkId.remove(p);
                networks.remove(net.getId());

                for (BlockPos p : members) {
                    if (!nodeToNetworkId.containsKey(p)) {
                        addNode(level, p);
                    }
                }
            }
            setDirty();
        }
    }

    public void mergeNetworks(ServerLevel level, ElectricalNetwork master, ElectricalNetwork victim) {
        if (master == victim || victim == null) return;
        for (BlockPos pos : victim.getMembers()) nodeToNetworkId.put(pos, master.getId());
        master.merge(level, victim);
        networks.remove(victim.getId());
        setDirty();
    }

    public void refreshNode(ServerLevel level, BlockPos pos) {
        ElectricalNetwork net = getNetworkAt(pos);
        if (net != null) {
            net.refreshNode(level, pos);
            syncVisuals(level, pos, findAllVisualLinks(level, pos));
        }
    }
}
