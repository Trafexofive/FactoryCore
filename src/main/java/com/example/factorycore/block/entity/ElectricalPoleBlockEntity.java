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
    private BlockPos connectedFloor = null;
    private boolean initialized = false;
    private static final double MAX_RANGE_SQR = 36.0; // Increased to 6 blocks inclusive

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
            initialized = true;
        }
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ElectricalPoleBlockEntity be) {
        if (level.isClientSide) return;
        
        if (level.getGameTime() % 10 == 0) { // Faster sync (0.5s)
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
        // Search range reduced to 4 blocks
        BlockPos.betweenClosedStream(worldPosition.offset(-4, -2, -4), worldPosition.offset(4, 5, 4)).forEach(p -> {
            if (p.equals(worldPosition)) return;
            double d = p.distSqr(worldPosition);
            if (d > MAX_RANGE_SQR) return; // Inclusive check

            // Check for electrical poles first
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity other) {
                if (other.connections.size() < 5) {
                    candidates.add(p.immutable());
                }
            }
            // Check for machines with energy capabilities
            else if (hasEnergyCapability(p)) {
                candidates.add(p.immutable());
            }
        });

        // Sort by distance (closest first)
        candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(worldPosition)));

        for (BlockPos p : candidates) {
            if (connections.size() >= 5) break;
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof ElectricalPoleBlockEntity other && other.connections.size() < 5) {
                 this.connect(p);
            }
            // Connect to machines with energy capabilities
            else if (hasEnergyCapability(p)) {
                this.connect(p);
            }
        }
    }

    /**
     * Check if a block has an energy capability that we can connect to
     */
    private boolean hasEnergyCapability(BlockPos pos) {
        net.neoforged.neoforge.capabilities.BlockCapability<net.neoforged.neoforge.energy.IEnergyStorage, net.minecraft.core.Direction> capability =
            net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            net.neoforged.neoforge.energy.IEnergyStorage storage = level.getCapability(capability, pos, direction.getOpposite());
            if (storage != null) {
                return true;
            }
        }
        return false;
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
       FactoryNetworkManager manager = FactoryNetworkManager.get(level);
       if (manager == null) return;

       ElectricalNetwork myNet = manager.getNetworkAt(worldPosition);
       if (myNet == null) {
           // Should have been added onPlace, but retry just in case
           manager.addNode(worldPosition);
           myNet = manager.getNetworkAt(worldPosition);
           if (myNet == null) return;
       }

       // 1. Search for Floor Networks (Increased range to 4 for bridging islands)
       BlockPos closestFloor = null;
       double minDist = Double.MAX_VALUE;
       for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-4, -2, -4), worldPosition.offset(4, 2, 4))) {
           if (level.getBlockState(p).is(com.example.factorycore.registry.CoreBlocks.ELECTRICAL_FLOOR.get())) {
               double d = p.distSqr(worldPosition);
               if (d < minDist) {
                   minDist = d;
                   closestFloor = p.immutable();
               }
           }
       }

       if (closestFloor != null) {
           // Ensure floor is registered
           manager.addNode(closestFloor);
           ElectricalNetwork floorNet = manager.getNetworkAt(closestFloor);
           if (floorNet != null && floorNet.getId() != myNet.getId()) {
               com.example.factorycore.util.FactoryLogger.power("Pole at " + worldPosition + " found island floor at " + closestFloor + ". Merging net " + myNet.getId() + " -> " + floorNet.getId());
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

       // 2. Bridge to other connected poles
       for (BlockPos otherPos : connections) {
           ElectricalNetwork otherNet = manager.getNetworkAt(otherPos);
           if (otherNet != null && otherNet.getId() != myNet.getId()) {
               manager.mergeNetworks(myNet, otherNet);
               myNet = manager.getNetworkAt(worldPosition);
               if (myNet == null) return;
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
        if (connectedFloor != null) tag.putLong("ConnectedFloor", connectedFloor.asLong());
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
        if (tag.contains("ConnectedFloor")) connectedFloor = BlockPos.of(tag.getLong("ConnectedFloor"));
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
