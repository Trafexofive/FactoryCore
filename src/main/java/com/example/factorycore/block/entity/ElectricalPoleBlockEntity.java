package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ElectricalPoleBlockEntity extends BlockEntity {
    private final Set<BlockPos> connections = new HashSet<>();
    private boolean initialized = false;
    private static final int MAX_RANGE_SQR = 18 * 18;

    public ElectricalPoleBlockEntity(BlockPos pos, BlockState blockState) {
        super(CoreBlockEntities.ELECTRICAL_POLE.get(), pos, blockState);
    }
    
    public Set<BlockPos> getConnections() {
        return connections;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        
        if (!be.initialized) {
            be.autoConnect();
            be.initialized = true;
        }
        
        if (level.getGameTime() % 20 == 0) {
            be.validateConnections();
            be.checkNetworkMerge();
        }
    }
    
    private void validateConnections() {
        boolean changed = false;
        Iterator<BlockPos> it = connections.iterator();
        while (it.hasNext()) {
            BlockPos target = it.next();
            if (target.distSqr(this.worldPosition) > MAX_RANGE_SQR || target.equals(this.worldPosition)) {
                it.remove();
                changed = true;
                continue;
            }
            if (level.isLoaded(target) && !(level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void autoConnect() {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        BlockPos.betweenClosedStream(worldPosition.offset(-15, -5, -15), worldPosition.offset(15, 10, 15)).forEach(p -> {
            if (p.equals(worldPosition)) return;
            if (p.distSqr(worldPosition) > 225) return;
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity other) {
                if (other.connections.size() < 5) {
                    candidates.add(p.immutable());
                }
            }
        });

        // Sort by distance (closest first) to ensure clean wiring (Factorio-style)
        candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));

        for (BlockPos p : candidates) {
            if (connections.size() >= 5) break;
            // Re-check target connection count (in case it filled up during this loop)
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof ElectricalPoleBlockEntity other && other.connections.size() < 5) {
                 this.connect(p);
            }
        }
    }

    public void connect(BlockPos other) {
        if (other.equals(this.worldPosition) || connections.contains(other)) return;
        connections.add(other);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        BlockEntity otherBe = level.getBlockEntity(other);
        if (otherBe instanceof ElectricalPoleBlockEntity otherPole) {
            otherPole.connectOneWay(this.worldPosition);
        }
    }
    
    public void connectOneWay(BlockPos other) {
        if (connections.add(other)) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void disconnectAll() {
        Set<BlockPos> copy = new HashSet<>(connections);
        for (BlockPos other : copy) {
             BlockEntity otherBe = level.getBlockEntity(other);
             if (otherBe instanceof ElectricalPoleBlockEntity otherPole) otherPole.removeConnection(this.worldPosition);
        }
        connections.clear();
        setChanged();
    }
    
    public void removeConnection(BlockPos other) {
        if (connections.remove(other)) {
            setChanged();
             level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void checkNetworkMerge() {
       ElectricalNetwork net1 = getNetworkBelow(worldPosition);
       if (net1 == null) return;
       for (BlockPos otherPos : connections) {
           ElectricalNetwork net2 = getNetworkBelow(otherPos);
           if (net2 != null && net2.getId() != net1.getId()) {
               FactoryNetworkManager.get(level).mergeNetworks(net1, net2);
               return; 
           }
       }
    }
    
    private ElectricalNetwork getNetworkBelow(BlockPos pos) {
        return FactoryNetworkManager.get(level).getNetworkAt(pos.below());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        long[] arr = new long[connections.size()];
        int i = 0;
        for (BlockPos p : connections) arr[i++] = p.asLong();
        tag.putLongArray("Connections", arr);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        connections.clear();
        if (tag.contains("Connections")) {
            long[] arr = tag.getLongArray("Connections");
            for (long val : arr) connections.add(BlockPos.of(val));
        } else if (tag.contains("Connections", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) NbtUtils.readBlockPos(list.getCompound(i), "pos").ifPresent(connections::add);
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
