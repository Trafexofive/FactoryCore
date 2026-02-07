package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.FactoryLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ElectricalPoleBlockEntity extends BlockEntity {
    private final Set<BlockPos> connections = new HashSet<>();
    private BlockPos connectedFloor = null;
    private static final double MAX_RANGE_SQR = 36.1;

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
            autoConnect();
            checkNetworkMerge();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        
        long time = level.getGameTime();
        // Validation & Machine discovery every 20 ticks (1s)
        if (time % 20 == 0) {
            be.validateConnections();
            be.maintainMachineConnections();
            be.checkNetworkMerge();
        }

        // Energy distribution every tick
        be.distributeEnergy();
    }

    private void distributeEnergy() {
        IEnergyStorage source = getEnergyStorage();
        if (source == null || source.getEnergyStored() <= 0) return;

        for (BlockPos target : connections) {
            // Machines only (other poles handled by network merge)
            if (level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity) continue;

            IEnergyStorage dest = getEnergyCapability(target);
            if (dest != null && dest.canReceive()) {
                int accepted = dest.receiveEnergy(Math.min(source.getEnergyStored(), 1000), false);
                if (accepted > 0) source.extractEnergy(accepted, false);
            }
        }
    }

    private void maintainMachineConnections() {
        // Simple search: find closest machine within 6 blocks
        BlockPos.betweenClosedStream(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > MAX_RANGE_SQR) return;
            
            // If it's a machine (has energy cap and isn't a pole)
            if (!(level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) && getEnergyCapability(p) != null) {
                if (!connections.contains(p)) {
                    connectOneWay(p.immutable());
                    FactoryLogger.power("Pole at " + worldPosition + " auto-connected to machine at " + p);
                }
            }
        });
    }

    private IEnergyStorage getEnergyCapability(BlockPos pos) {
        var cap = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
        // Try all sides + null
        IEnergyStorage s = level.getCapability(cap, pos, null);
        if (s != null) return s;
        for (Direction d : Direction.values()) {
            s = level.getCapability(cap, pos, d);
            if (s != null) return s;
        }
        return null;
    }

    private void validateConnections() {
        boolean changed = false;
        Iterator<BlockPos> it = connections.iterator();
        while (it.hasNext()) {
            BlockPos target = it.next();
            if (target.distSqr(worldPosition) > MAX_RANGE_SQR || level.getBlockState(target).isAir()) {
                it.remove();
                changed = true;
            }
        }
        if (changed) sync();
    }

    public void autoConnect() {
        BlockPos.betweenClosedStream(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > MAX_RANGE_SQR) return;
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) connect(p.immutable());
        });
    }

    public void connect(BlockPos other) {
        if (other.equals(worldPosition) || connections.contains(other)) return;
        connections.add(other);
        sync();
        if (level.getBlockEntity(other) instanceof ElectricalPoleBlockEntity otherPole) {
            otherPole.connectOneWay(worldPosition);
        }
    }
    
    public void connectOneWay(BlockPos other) {
        if (connections.add(other)) sync();
    }
    
    public void disconnectAll() {
        for (BlockPos other : new HashSet<>(connections)) {
             if (level.getBlockEntity(other) instanceof ElectricalPoleBlockEntity op) op.removeConnection(worldPosition);
        }
        connections.clear();
        sync();
    }
    
    public void removeConnection(BlockPos other) {
        if (connections.remove(other)) sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void checkNetworkMerge() {
       FactoryNetworkManager manager = FactoryNetworkManager.get(level);
       ElectricalNetwork myNet = manager.getNetworkAt(worldPosition);
       if (myNet == null) { manager.addNode(worldPosition); myNet = manager.getNetworkAt(worldPosition); }
       if (myNet == null) return;

       for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 2, 6))) {
           if (level.getBlockState(p).is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get())) {
               manager.addNode(p);
               ElectricalNetwork floorNet = manager.getNetworkAt(p);
               if (floorNet != null && floorNet.getId() != myNet.getId()) {
                   manager.mergeNetworks(floorNet, myNet);
                   myNet = manager.getNetworkAt(worldPosition);
               }
               if (!p.equals(connectedFloor)) { connectedFloor = p.immutable(); sync(); }
               break;
           }
       }

       for (BlockPos otherPos : connections) {
           ElectricalNetwork otherNet = manager.getNetworkAt(otherPos);
           if (otherNet != null && otherNet.getId() != myNet.getId()) {
               manager.mergeNetworks(myNet, otherNet);
               myNet = manager.getNetworkAt(worldPosition);
           }
       }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        long[] arr = new long[connections.size()];
        int i = 0;
        for (BlockPos p : connections) arr[i++] = p.asLong();
        tag.putLongArray("Connections", arr);
        if (connectedFloor != null) tag.putLong("ConnectedFloor", connectedFloor.asLong());
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        connections.clear();
        if (tag.contains("Connections")) {
            for (long val : tag.getLongArray("Connections")) connections.add(BlockPos.of(val).immutable());
        }
        if (tag.contains("ConnectedFloor")) connectedFloor = BlockPos.of(tag.getLong("ConnectedFloor")).immutable();
    }
    
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider lookupProvider) { if (pkt.getTag() != null) loadAdditional(pkt.getTag(), lookupProvider); }
}