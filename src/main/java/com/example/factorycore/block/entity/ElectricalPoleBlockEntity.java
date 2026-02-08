package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.block.ElectricalPoleBlock;
import com.example.factorycore.util.FactoryLogger;
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
import java.util.concurrent.ConcurrentHashMap;

public class ElectricalPoleBlockEntity extends BlockEntity {
    private int scanRange = 6;
    private double maxRangeSqr = 36.1;
    private int transferLimit = 10000;

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
        if (level != null && !level.isClientSide) refresh();
    }

    public void refresh() {
        if (level == null || level.isClientSide) return;
        autoConnect();
        checkNetworkMerge();
        maintainMachineConnections();
        maintainFloorConnections();
        validateConnections();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        long time = level.getGameTime();
        long offset = Math.abs(pos.asLong() % 20);
        if ((time + offset) % 10 == 0) {
            be.validateConnections();
            be.checkNetworkMerge();
        }
        if ((time + offset) % 20 == 0) {
            be.maintainMachineConnections();
            be.maintainFloorConnections();
        }
        be.handleEnergyTransfer();
    }

    private void handleEnergyTransfer() {
        IEnergyStorage network = getEnergyStorage();
        if (network == null) return;

        for (BlockPos target : connections) {
            if (isPole(target) || isFloor(target)) continue;

            Direction dirToPole = Direction.getNearest(
                worldPosition.getX() - target.getX(),
                worldPosition.getY() - target.getY(),
                worldPosition.getZ() - target.getZ()
            );
            
            IEnergyStorage machine = getEnergyCapability(target, dirToPole);
            if (machine == null) continue;

            if (machine.canReceive() && network.getEnergyStored() > 0) {
                int toPush = network.extractEnergy(transferLimit, true);
                int accepted = machine.receiveEnergy(toPush, false);
                if (accepted > 0) network.extractEnergy(accepted, false);
            }

            if (machine.canExtract()) {
                int space = network.getMaxEnergyStored() - network.getEnergyStored();
                if (space > 0) {
                    int toPull = Math.min(space, transferLimit);
                    int extracted = machine.extractEnergy(toPull, false);
                    if (extracted > 0) network.receiveEnergy(extracted, false);
                }
            }
        }
    }

    private void maintainMachineConnections() {
        BlockPos.betweenClosedStream(worldPosition.offset(-scanRange, -scanRange, -scanRange), worldPosition.offset(scanRange, scanRange, scanRange)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > maxRangeSqr) return;
            if (!isPole(p) && !isFloor(p)) {
                // Do not connect directly to machines on floors
                if (isFloor(p.below())) {
                    if (connections.contains(p)) removeConnection(p);
                    return;
                }
                if (getEnergyCapability(p, null) != null) handleMachineConnection(p.immutable());
            }
        });
    }

    private void maintainFloorConnections() {
        FactoryNetworkManager manager = FactoryNetworkManager.get(level);
        if (manager == null) return;

        java.util.Map<Integer, java.util.List<BlockPos>> floorsByNetwork = new java.util.HashMap<>();
        BlockPos.betweenClosedStream(worldPosition.offset(-scanRange, -scanRange, -scanRange), worldPosition.offset(scanRange, scanRange, scanRange)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > maxRangeSqr) return;
            if (isFloor(p)) {
                ElectricalNetwork net = manager.getNetworkAt(p);
                if (net != null) floorsByNetwork.computeIfAbsent(net.getId(), k -> new java.util.ArrayList<>()).add(p.immutable());
            }
        });

        for (var entry : floorsByNetwork.entrySet()) {
            ElectricalNetwork net = manager.getNetwork(entry.getKey());
            java.util.List<BlockPos> networkFloors = entry.getValue();
            
            BlockPos closest = null;
            double minDst = Double.MAX_VALUE;
            for (BlockPos p : networkFloors) {
                double dst = p.distSqr(worldPosition);
                if (dst < minDst) { minDst = dst; closest = p; }
            }

            if (closest != null) {
                BlockPos currentAssigned = net.getAssignedPole();
                boolean isWinner = false;

                if (currentAssigned == null || currentAssigned.equals(worldPosition)) isWinner = true;
                else {
                    // Tie-break with other pole
                    if (level.getBlockEntity(currentAssigned) instanceof ElectricalPoleBlockEntity otherPole) {
                        double otherDst = closest.distSqr(currentAssigned);
                        if (minDst < otherDst) isWinner = true;
                        else if (Math.abs(minDst - otherDst) < 0.001 && worldPosition.asLong() < currentAssigned.asLong()) isWinner = true;
                    } else isWinner = true; // Other pole is gone
                }

                if (isWinner) {
                    net.setAssignedPole(worldPosition);
                    if (!connections.contains(closest)) connectOneWay(closest);
                    for (BlockPos p : networkFloors) if (!p.equals(closest) && connections.contains(p)) removeConnection(p);
                } else {
                    for (BlockPos p : networkFloors) if (connections.contains(p)) removeConnection(p);
                }
            }
        }
    }

    private void handleMachineConnection(BlockPos machinePos) {
        BlockPos closestPole = null;
        double minDst = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(machinePos.offset(-scanRange, -scanRange, -scanRange), machinePos.offset(scanRange, scanRange, scanRange))) {
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) {
                double dst = machinePos.distSqr(p);
                if (dst <= maxRangeSqr) {
                    if (dst < minDst) { minDst = dst; closestPole = p.immutable(); }
                    else if (Math.abs(dst - minDst) < 0.001 && p.equals(this.worldPosition)) closestPole = this.worldPosition;
                }
            }
        }
        if (closestPole != null) {
            if (closestPole.equals(this.worldPosition)) { if (!connections.contains(machinePos)) connectOneWay(machinePos); }
            else { if (connections.contains(machinePos)) removeConnection(machinePos); }
        }
    }

    private IEnergyStorage getEnergyCapability(BlockPos pos, Direction side) {
        var cap = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
        IEnergyStorage s = level.getCapability(cap, pos, side);
        if (s != null) return s;
        s = level.getCapability(cap, pos, null);
        if (s != null) return s;
        for (Direction d : Direction.values()) { if (d != side) { s = level.getCapability(cap, pos, d); if (s != null) return s; } }
        return null;
    }

    private boolean isPole(BlockPos pos) { return level.getBlockState(pos).getBlock() instanceof ElectricalPoleBlock; }
    private boolean isFloor(BlockPos pos) { return level.getBlockState(pos).is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get()); }

    private void validateConnections() {
        boolean changed = false;
        Iterator<BlockPos> it = connections.iterator();
        while (it.hasNext()) {
            BlockPos target = it.next();
            if (level.isLoaded(target)) {
                if (target.distSqr(worldPosition) > maxRangeSqr || level.getBlockState(target).isAir()) { it.remove(); changed = true; }
            }
        }
        if (changed) sync();
    }

    public void autoConnect() {
        BlockPos.betweenClosedStream(worldPosition.offset(-scanRange, -scanRange, -scanRange), worldPosition.offset(scanRange, scanRange, scanRange)).forEach(p -> {
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > maxRangeSqr) return;
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) connect(p.immutable());
        });
    }

    public void connect(BlockPos other) {
        if (other.equals(worldPosition) || connections.contains(other)) return;
        connections.add(other); sync();
        if (level.getBlockEntity(other) instanceof ElectricalPoleBlockEntity otherPole) otherPole.connectOneWay(worldPosition);
    }
    
    public void connectOneWay(BlockPos other) { if (connections.add(other)) sync(); }
    
    public void disconnectAll() {
        for (BlockPos other : new HashSet<>(connections)) { if (level.getBlockEntity(other) instanceof ElectricalPoleBlockEntity op) op.removeConnection(worldPosition); }
        connections.clear(); sync();
    }
    
    public void removeConnection(BlockPos other) { if (connections.remove(other)) sync(); }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    private void checkNetworkMerge() {
       FactoryNetworkManager manager = FactoryNetworkManager.get(level);
       ElectricalNetwork myNet = manager.getNetworkAt(worldPosition);
       if (myNet == null) { manager.addNode(worldPosition); myNet = manager.getNetworkAt(worldPosition); }
       if (myNet == null) return;
       BlockPos bestFloor = null;
       double minFloorDst = Double.MAX_VALUE;
       for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-scanRange, -scanRange, -scanRange), worldPosition.offset(scanRange, scanRange, scanRange))) {
           if (p.equals(worldPosition)) continue;
           BlockState s = level.getBlockState(p);
           if (s.is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get()) || s.getBlock() instanceof com.example.factorycore.block.ElectricalPoleBlock) {
               ElectricalNetwork otherNet = manager.getNetworkAt(p);
               if (otherNet != null && otherNet.getId() != myNet.getId()) { manager.mergeNetworks(myNet, otherNet); myNet = manager.getNetworkAt(worldPosition); }
               if (s.is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get())) {
                   double d = p.distSqr(worldPosition);
                   if (d < minFloorDst) { minFloorDst = d; bestFloor = p.immutable(); }
               }
           }
       }
       if (bestFloor != null && !bestFloor.equals(connectedFloor)) { connectedFloor = bestFloor; sync(); }
       else if (bestFloor == null && connectedFloor != null) { connectedFloor = null; sync(); }
    }

    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        long[] arr = new long[connections.size()]; int i = 0;
        for (BlockPos p : connections) arr[i++] = p.asLong();
        tag.putLongArray("Connections", arr);
        if (connectedFloor != null) tag.putLong("ConnectedFloor", connectedFloor.asLong());
        tag.putInt("ScanRange", scanRange);
        tag.putInt("TransferLimit", transferLimit);
    }

    @Override public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        connections.clear();
        if (tag.contains("Connections")) { for (long val : tag.getLongArray("Connections")) connections.add(BlockPos.of(val).immutable()); }
        if (tag.contains("ConnectedFloor")) connectedFloor = BlockPos.of(tag.getLong("ConnectedFloor")).immutable();
        if (tag.contains("ScanRange")) {
            scanRange = tag.getInt("ScanRange");
            maxRangeSqr = (scanRange * scanRange) + 0.1;
        }
        if (tag.contains("TransferLimit")) transferLimit = tag.getInt("TransferLimit");
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider lookupProvider) { if (pkt.getTag() != null) loadAdditional(pkt.getTag(), lookupProvider); }
}