package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.block.ElectricalPoleBlock;
import com.example.factorycore.util.FactoryLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ElectricalPoleBlockEntity extends BlockEntity {
    private final Set<BlockPos> connections = ConcurrentHashMap.newKeySet();
    private BlockPos connectedFloor = null;

    public ElectricalPoleBlockEntity(BlockPos pos, BlockState blockState) {
        super(CoreBlockEntities.ELECTRICAL_POLE.get(), pos, blockState);
    }
    
    public Set<BlockPos> getConnections() { return connections; }
    public BlockPos getConnectedFloor() { return connectedFloor; }

    public IEnergyStorage getEnergyStorage() {
        if (level == null || level.isClientSide) return null;
        ElectricalNetwork net = FactoryNetworkManager.get(level).getNetworkAt(worldPosition);
        return net != null ? net.getStorage() : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            FactoryNetworkManager.get(level).addNode((ServerLevel)level, worldPosition);
        }
    }

    public void addVisualConnection(BlockPos other) {
        if (!other.equals(worldPosition)) {
            connections.add(other.immutable());
        }
    }

    public void refresh() {
        if (level == null || level.isClientSide) return;
        FactoryNetworkManager.get(level).refreshNode((ServerLevel)level, worldPosition);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        // NO POLLING: Connectivity is handled by NetworkManager.tick()
    }

    public void disconnectAll() {
        if (level != null && !level.isClientSide) {
            FactoryNetworkManager.get(level).removeNode((ServerLevel)level, worldPosition);
        }
    }
    
    public void removeConnection(BlockPos other) { 
        connections.remove(other);
    }

    @Override 
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        long[] arr = new long[connections.size()];
        int i = 0;
        for (BlockPos p : connections) arr[i++] = p.asLong();
        tag.putLongArray("VisualConnections", arr);
    }

    @Override 
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        connections.clear();
        if (tag.contains("VisualConnections")) {
            for (long val : tag.getLongArray("VisualConnections")) {
                connections.add(BlockPos.of(val).immutable());
            }
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

    

        @Override

        public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider lookupProvider) {

            if (pkt.getTag() != null) loadAdditional(pkt.getTag(), lookupProvider);

        }

    }