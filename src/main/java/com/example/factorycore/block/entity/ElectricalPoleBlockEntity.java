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
        
        // Staggered updates based on position to prevent lag spikes
        long gameTime = level.getGameTime();
        long offset = pos.asLong();
        
        if ((gameTime + offset) % 10 == 0) { // Fast sync (0.5s)
            be.validateConnections();
            be.checkNetworkMerge();
        }
        
        if ((gameTime + offset) % 20 == 0) { // Slow sync (1s)
            be.maintainMachineConnections();
        }

        be.distributeEnergy();
    }

    private void distributeEnergy() {
        FactoryNetworkManager manager = FactoryNetworkManager.get(level);
        if (manager == null) return;
        ElectricalNetwork network = manager.getNetworkAt(worldPosition);
        if (network == null || network.getStorage().getEnergyStored() <= 0) return;

        net.neoforged.neoforge.energy.IEnergyStorage source = network.getStorage();
        int maxExtract = 10000; // Limit extraction rate per tick per connection

        for (BlockPos target : connections) {
            // Skip other poles
            if (level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity) continue;

            // Determine best side to insert energy (Side facing the pole)
            net.minecraft.core.Direction directionToPole = getDirectionTo(target, worldPosition);
            
            // Try specific side first
            net.neoforged.neoforge.energy.IEnergyStorage dest = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 
                target, 
                directionToPole
            );
            
            // Fallback: Try null side (internal/omnidirectional)
            if (dest == null) {
                dest = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 
                    target, 
                    null
                );
            }
            
            // Fallback: Try all other sides
            if (dest == null) {
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    if (dir == directionToPole) continue;
                    dest = level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, 
                        target, 
                        dir
                    );
                    if (dest != null) break;
                }
            }

            if (dest != null && dest.canReceive()) {
                int simulatedExtract = source.extractEnergy(maxExtract, true);
                int accepted = dest.receiveEnergy(simulatedExtract, false);
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
            if (target.distSqr(this.worldPosition) > MAX_RANGE_SQR || target.equals(this.worldPosition)) {
                it.remove();
                changed = true;
                continue;
            }
            if (level.isLoaded(target)) {
                boolean isPole = level.getBlockEntity(target) instanceof ElectricalPoleBlockEntity;
                boolean isMachine = !isPole && hasEnergyCapability(target);
                
                // Remove if it's neither a pole nor a machine we can power
                if (!isPole && !isMachine) {
                    it.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void maintainMachineConnections() {
        // 1. Scan for machines in range
        BlockPos.betweenClosedStream(worldPosition.offset(-4, -4, -4), worldPosition.offset(4, 4, 4)).forEach(p -> {
            if (p.equals(worldPosition)) return;
            // Check distance
            if (p.distSqr(worldPosition) > MAX_RANGE_SQR) return;

            // Check if it's a machine (has energy, not a pole)
            if (!(level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) && hasEnergyCapability(p)) {
                // It's a machine. Check if we should be the one connecting.
                BlockPos target = p.immutable();
                handleMachineConnection(target);
            }
        });
    }

    private void handleMachineConnection(BlockPos machinePos) {
        // Find ALL poles connected to this machine (or close enough to connect)
        // This is expensive to scan globally, so we scan local area of the machine.
        
        BlockPos closestPole = null;
        double minDst = Double.MAX_VALUE;

        // Scan around the MACHINE to find nearby poles
        for (BlockPos p : BlockPos.betweenClosed(machinePos.offset(-4, -4, -4), machinePos.offset(4, 4, 4))) {
            if (level.getBlockEntity(p) instanceof ElectricalPoleBlockEntity) {
                double dst = p.distSqr(machinePos);
                if (dst <= MAX_RANGE_SQR) {
                    if (dst < minDst) {
                        minDst = dst;
                        closestPole = p;
                    } else if (dst == minDst && p.equals(this.worldPosition)) {
                        // Tie-breaker: prefer self if equal distance (stability)
                        closestPole = this.worldPosition;
                    }
                }
            }
        }

        if (closestPole != null) {
            if (closestPole.equals(this.worldPosition)) {
                // I am the closest! Connect if not already.
                if (!connections.contains(machinePos)) {
                    // Check if another pole is currently holding it (and steal it)
                    // We rely on the *other* pole running this same logic and disconnecting itself.
                    // But for immediate visual feedback, we can force disconnect neighbors? 
                    // No, let's just connect. The other pole will disconnect next tick.
                    connectOneWay(machinePos);
                }
            } else {
                // I am NOT the closest. Disconnect if connected.
                if (connections.contains(machinePos)) {
                    removeConnection(machinePos);
                }
            }
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