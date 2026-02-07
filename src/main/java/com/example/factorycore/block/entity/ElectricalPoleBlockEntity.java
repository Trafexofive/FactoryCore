package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.FactoryLogger;
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
    private BlockPos connectedFloor = null;
    private static final double MAX_RANGE_SQR = 36.1; // 6 blocks inclusive

    public ElectricalPoleBlockEntity(BlockPos pos, BlockState blockState) {
        super(CoreBlockEntities.ELECTRICAL_POLE.get(), pos, blockState);
    }
    
    public Set<BlockPos> getConnections() {
        return connections;
    }

    public BlockPos getConnectedFloor() {
        return connectedFloor;
    }

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        if (level == null || level.isClientSide) return null;
        FactoryNetworkManager manager = FactoryNetworkManager.get(level);
        if (manager == null) return null;
        
        ElectricalNetwork net = manager.getNetworkAt(worldPosition);
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

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        
        long gameTime = level.getGameTime();
        long offset = Math.abs(pos.asLong() % 20);
        
        // Validation (Fast, 0.5s)
        if ((gameTime + offset) % 10 == 0) {
            be.validateConnections();
            be.checkNetworkMerge();
        }
        
        // Scanning (Medium, 1s) - Re-scan to find new machines
        if ((gameTime + offset) % 20 == 0) {
            be.maintainMachineConnections();
        }

        // Power Distribution (Every Tick)
        be.distributeEnergy();
    }

    private void distributeEnergy() {
        FactoryNetworkManager manager = FactoryNetworkManager.get(level);
        if (manager == null) return;
        ElectricalNetwork network = manager.getNetworkAt(worldPosition);
        if (network == null || network.getStorage().getEnergyStored() <= 0) return;

        net.neoforged.neoforge.energy.IEnergyStorage source = network.getStorage();
        int maxExtract = 10000;

        for (BlockPos target : connections) {
            if (level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity) continue;

            // Try to find ANY accepting side
            net.neoforged.neoforge.energy.IEnergyStorage dest = null;
            var cap = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;

            // 1. Try side facing pole
            net.minecraft.core.Direction dirToPole = getDirectionTo(target, worldPosition);
            dest = level.getCapability(cap, target, dirToPole);

            // 2. Try null side
            if (dest == null) dest = level.getCapability(cap, target, null);

            // 3. Brute force all sides
            if (dest == null) {
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                    dest = level.getCapability(cap, target, d);
                    if (dest != null) break;
                }
            }

            if (dest != null && dest.canReceive()) {
                int simulated = source.extractEnergy(maxExtract, true);
                int accepted = dest.receiveEnergy(simulated, false);
                if (accepted > 0) {
                    source.extractEnergy(accepted, false);
                }
            }
        }
    }
    
    private net.minecraft.core.Direction getDirectionTo(BlockPos from, BlockPos to) {
        return net.minecraft.core.Direction.getNearest(
            to.getX() - from.getX(),
            to.getY() - from.getY(),
            to.getZ() - from.getZ()
        );
    }

    private void validateConnections() {
        boolean changed = false;
        Iterator<BlockPos> it = connections.iterator();
        while (it.hasNext()) {
            BlockPos target = it.next();
            
            // 1. Range Check
            if (target.distSqr(this.worldPosition) > MAX_RANGE_SQR || target.equals(this.worldPosition)) {
                it.remove();
                changed = true;
                continue;
            }
            
            // 2. Existence Check (Sticky Logic: Only remove if Air)
            if (level.isLoaded(target)) {
                BlockState s = level.getBlockState(target);
                if (s.isAir()) {
                    it.remove();
                    changed = true;
                    continue;
                }
                
                // Special case: If it WAS a pole and is now NOT a pole (but valid block), 
                // we treat it as a machine. If it WAS a machine and is now a Pole, we handle that too.
                // But generally, if it's not air, we keep it connected and let distributeEnergy fail gracefully if no cap.
                // This prevents "flickering" if capability is transiently missing.
            }
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void maintainMachineConnections() {
        // Scan 6 blocks radius
        BlockPos.betweenClosedStream(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6)).forEach(p -> {
            if (p.equals(worldPosition)) return;
            if (p.distSqr(worldPosition) > MAX_RANGE_SQR) return;

            // Only try to connect if it has energy capability AND is not a pole
            if (!(level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) && hasEnergyCapability(p)) {
                handleMachineConnection(p.immutable());
            }
        });
    }

    private void handleMachineConnection(BlockPos machinePos) {
        // Closest Pole Logic
        BlockPos closestPole = null;
        double minDst = Double.MAX_VALUE;

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
                if (!connections.contains(machinePos)) {
                    connectOneWay(machinePos);
                }
            } else {
                if (connections.contains(machinePos)) {
                    removeConnection(machinePos);
                }
            }
        }
    }
    
    public void autoConnect() {
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        BlockPos.betweenClosedStream(worldPosition.offset(-6, -6, -6), worldPosition.offset(6, 6, 6)).forEach(p -> {
            if (p.equals(worldPosition)) return;
            if (p.distSqr(worldPosition) > MAX_RANGE_SQR) return;

            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity other) {
                if (other.connections.size() < 10) candidates.add(p.immutable());
            } else if (hasEnergyCapability(p)) {
                candidates.add(p.immutable());
            }
        });
        candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));
        for (BlockPos p : candidates) {
            if (connections.size() >= 10) break;
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) {
                 this.connect(p);
            } else {
                handleMachineConnection(p);
            }
        }
    }

    private boolean hasEnergyCapability(BlockPos pos) {
        var cap = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
        if (level.getCapability(cap, pos, null) != null) return true;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (level.getCapability(cap, pos, d) != null) return true;
        }
        return false;
    }

    public void connect(BlockPos other) {
        BlockPos target = other.immutable();
        if (target.equals(this.worldPosition) || connections.contains(target)) return;
        connections.add(target);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        BlockEntity otherBe = level.getBlockEntity(target);
        if (otherBe instanceof ElectricalPoleBlockEntity otherPole) {
            otherPole.connectOneWay(this.worldPosition);
        }
    }
    
    public void connectOneWay(BlockPos other) {
        if (connections.add(other.immutable())) {
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
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    
    public void removeConnection(BlockPos other) {
        if (connections.remove(other)) {
            setChanged();
             level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void checkNetworkMerge() {
       FactoryNetworkManager manager = FactoryNetworkManager.get(level);
       if (manager == null) return;

       ElectricalNetwork myNet = manager.getNetworkAt(worldPosition);
       if (myNet == null) {
           manager.addNode(worldPosition);
           myNet = manager.getNetworkAt(worldPosition);
           if (myNet == null) return;
       }

       BlockPos closestFloor = null;
       double minDist = Double.MAX_VALUE;
       for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 2, 6))) {
           if (level.getBlockState(p).is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get())) {
               double d = p.distSqr(worldPosition);
               if (d < minDist && d <= MAX_RANGE_SQR) {
                   minDist = d;
                   closestFloor = p.immutable();
               }
           }
       }

       if (closestFloor != null) {
           manager.addNode(closestFloor);
           ElectricalNetwork floorNet = manager.getNetworkAt(closestFloor);
           if (floorNet != null && floorNet.getId() != myNet.getId()) {
               manager.mergeNetworks(floorNet, myNet); 
               myNet = manager.getNetworkAt(worldPosition);
               if (myNet == null) return;
           }
           
           if (!closestFloor.equals(connectedFloor)) {
               connectedFloor = closestFloor;
               setChanged();
               level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
           }
       } else if (connectedFloor != null) {
           connectedFloor = null;
           setChanged();
           level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
       }

       for (BlockPos otherPos : connections) {
           ElectricalNetwork otherNet = manager.getNetworkAt(otherPos);
           if (otherNet != null && otherNet.getId() != myNet.getId()) {
               manager.mergeNetworks(myNet, otherNet);
               myNet = manager.getNetworkAt(worldPosition);
               if (myNet == null) return;
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
            long[] arr = tag.getLongArray("Connections");
            for (long val : arr) connections.add(BlockPos.of(val).immutable());
        } else if (tag.contains("Connections", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) NbtUtils.readBlockPos(list.getCompound(i), "pos").map(BlockPos::immutable).ifPresent(connections::add);
        }
        if (tag.contains("ConnectedFloor")) connectedFloor = BlockPos.of(tag.getLong("ConnectedFloor")).immutable();
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
