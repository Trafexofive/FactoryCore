package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
    private static final double MAX_RANGE_SQR = 36.1; // 6 blocks inclusive

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
            // Immediate initial setup
            autoConnect();
            maintainMachineConnections();
            checkNetworkMerge();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        
        long time = level.getGameTime();
        
        // Faster responsiveness: 5 ticks (0.25s) for critical syncs
        if (time % 5 == 0) {
            be.validateConnections();
            be.checkNetworkMerge();
        }

        // Machine discovery: 10 ticks (0.5s)
        if (time % 10 == 0) {
            be.maintainMachineConnections();
        }

        be.distributeEnergy();
    }

    private void distributeEnergy() {
        IEnergyStorage source = getEnergyStorage();
        if (source == null || source.getEnergyStored() <= 0) return;

        for (BlockPos target : connections) {
            if (level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity) continue;

            IEnergyStorage dest = getEnergyCapability(target);
            if (dest != null && dest.canReceive()) {
                int accepted = dest.receiveEnergy(Math.min(source.getEnergyStored(), 10000), false);
                if (accepted > 0) source.extractEnergy(accepted, false);
            }
        }
    }

    private void maintainMachineConnections() {
        BlockPos.betweenClosedStream(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > MAX_RANGE_SQR) return;
            if (!(level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) && getEnergyCapability(p) != null) {
                handleMachineConnection(p.immutable());
            }
        });
    }

    private void handleMachineConnection(BlockPos machinePos) {
        BlockPos closestPole = null;
        double minDst = Double.MAX_VALUE;

        // Find the absolute closest pole to this machine
        for (BlockPos p : BlockPos.betweenClosed(machinePos.offset(-6, -6, -6), machinePos.offset(6, 6, 6))) {
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) {
                double dst = p.distSqr(machinePos);
                if (dst <= MAX_RANGE_SQR) {
                    if (dst < minDst) {
                        minDst = dst;
                        closestPole = p.immutable();
                    } else if (Math.abs(dst - minDst) < 0.001 && p.equals(this.worldPosition)) {
                        closestPole = this.worldPosition;
                    }
                }
            }
        }

        if (closestPole != null) {
            if (closestPole.equals(this.worldPosition)) {
                if (!connections.contains(machinePos)) connectOneWay(machinePos);
            } else {
                if (connections.contains(machinePos)) removeConnection(machinePos);
            }
        }
    }

    private IEnergyStorage getEnergyCapability(BlockPos pos) {
        var cap = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
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

       BlockPos bestFloor = null;
       double minFloorDst = Double.MAX_VALUE;

       // Bridge network to nearby Nodes
       for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6))) {
           if (p.equals(worldPosition)) continue;
           
           BlockState s = level.getBlockState(p);
           boolean isFloor = s.is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get());
           boolean isPole = s.getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock;
           
           if (isFloor || isPole) {
               ElectricalNetwork otherNet = manager.getNetworkAt(p);
               if (otherNet != null && otherNet.getId() != myNet.getId()) {
                   manager.mergeNetworks(myNet, otherNet);
                   myNet = manager.getNetworkAt(worldPosition);
               }
               
               if (isFloor) {
                   double d = p.distSqr(worldPosition);
                   if (d < minFloorDst) {
                       minFloorDst = d;
                       bestFloor = p.immutable();
                   }
               }
           }
       }
       
       if (bestFloor != null && !bestFloor.equals(connectedFloor)) {
           connectedFloor = bestFloor;
           sync();
       } else if (bestFloor == null && connectedFloor != null) {
           connectedFloor = null;
           sync();
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