package com.example.factorycore.block.entity;

import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * Advanced base class for FactoryCore multiblocks.
 * Handles structure validation, automatic I/O port discovery, and provides transfer helpers.
 */
public abstract class AbstractFactoryMultiblockBlockEntity extends BlockEntity {
    protected boolean isFormed = false;
    protected int checkTimer = 0;
    protected Direction cachedFacing = Direction.NORTH;

    // --- Storage ---
    protected final ItemStackHandler inventory = createItemHandler();
    protected final FluidTank fluidTank = createFluidTank();

    // --- Port Tracking ---
    protected final Map<Direction, BlockPos> itemPorts = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockPos> fluidPorts = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockPos> energyPorts = new EnumMap<>(Direction.class);

    // --- Capability Caches (Server Side Only) ---
    protected final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> itemCaches = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockCapabilityCache<IFluidHandler, Direction>> fluidCaches = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);

    public AbstractFactoryMultiblockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        updateFacingCache(state);
    }

    // --- Configuration Getters ---
    protected int getInventorySize() { return 2; }
    protected int getFluidCapacity() { return 16000; }

    protected ItemStackHandler createItemHandler() {
        return new ItemStackHandler(getInventorySize()) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };
    }

    protected FluidTank createFluidTank() {
        return new FluidTank(getFluidCapacity()) {
            @Override
            protected void onContentsChanged() { setChanged(); }
        };
    }

    // --- Abstract API ---
    public abstract MultiblockPattern getPattern();
    protected abstract void serverTick();

    // --- Ticking Logic ---
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractFactoryMultiblockBlockEntity be) {
        if (level.isClientSide) return;

        if (be.checkTimer++ >= 20) {
            be.checkTimer = 0;
            be.recheckStructure();
        }

        if (be.isFormed) {
            be.serverTick();
        }
    }

    public void recheckStructure() {
        if (level == null) return;
        updateFacingCache(getBlockState());
        
        boolean nowFormed = getPattern().matches(level, worldPosition, cachedFacing);
        if (nowFormed != this.isFormed) {
            this.isFormed = nowFormed;
            onFormationChanged(this.isFormed);
        }
    }

    protected void onFormationChanged(boolean formed) {
        setChanged();
        if (formed) {
            discoverIOPorts();
            initCapabilityCaches();
            if (level != null) {
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        } else {
            clearPorts();
            clearCapabilityCaches();
        }
    }

    private void updateFacingCache(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            this.cachedFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
    }

    // --- I/O Port Discovery ---

    /**
     * Scans the structure or neighbors to find valid I/O points.
     * Default implementation assumes any side of the controller touching a container is a port.
     * Override this to define specific port blocks in your multiblock pattern.
     */
    protected void discoverIOPorts() {
        clearPorts();
        if (level == null) return;

        // Default: Check all 6 sides of the controller itself
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite()) != null) {
                itemPorts.put(dir, neighborPos);
            }
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite()) != null) {
                fluidPorts.put(dir, neighborPos);
            }
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite()) != null) {
                energyPorts.put(dir, neighborPos);
            }
        }
    }

    private void clearPorts() {
        itemPorts.clear();
        fluidPorts.clear();
        energyPorts.clear();
    }

    private void initCapabilityCaches() {
        if (level == null || level.isClientSide) return;
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;

        itemPorts.forEach((dir, pos) -> itemCaches.put(dir, BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, pos, dir.getOpposite())));
        fluidPorts.forEach((dir, pos) -> fluidCaches.put(dir, BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, serverLevel, pos, dir.getOpposite())));
        energyPorts.forEach((dir, pos) -> energyCaches.put(dir, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, serverLevel, pos, dir.getOpposite())));
    }

    private void clearCapabilityCaches() {
        itemCaches.clear();
        fluidCaches.clear();
        energyCaches.clear();
    }

    // --- Transfer Helpers ---

    protected void pushItemToPort(Direction dir, int slot, int amount) {
        BlockCapabilityCache<IItemHandler, Direction> cache = itemCaches.get(dir);
        if (cache == null) return;
        IItemHandler target = cache.getCapability();
        if (target != null) {
            net.minecraft.world.item.ItemStack toPush = inventory.extractItem(slot, amount, true);
            if (!toPush.isEmpty()) {
                net.minecraft.world.item.ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, toPush, false);
                inventory.extractItem(slot, toPush.getCount() - remainder.getCount(), false);
            }
        }
    }

    protected void pullItemFromPort(Direction dir, int slot, int amount) {
        BlockCapabilityCache<IItemHandler, Direction> cache = itemCaches.get(dir);
        if (cache == null) return;
        IItemHandler target = cache.getCapability();
        if (target != null) {
            for (int i = 0; i < target.getSlots(); i++) {
                net.minecraft.world.item.ItemStack canExtract = target.extractItem(i, amount, true);
                if (!canExtract.isEmpty()) {
                    net.minecraft.world.item.ItemStack remainder = inventory.insertItem(slot, canExtract, false);
                    target.extractItem(i, canExtract.getCount() - remainder.getCount(), false);
                    if (remainder.isEmpty()) break;
                }
            }
        }
    }

    protected IEnergyStorage getAnyEnergyPort() {
        for (BlockCapabilityCache<IEnergyStorage, Direction> cache : energyCaches.values()) {
            IEnergyStorage storage = cache.getCapability();
            if (storage != null) return storage;
        }
        // Fallback to floor
        return getFloorEnergy();
    }

    protected IEnergyStorage getFloorEnergy() {
        if (level == null) return null;
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.below(), Direction.UP);
    }

    // --- Persistence ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", isFormed);
        tag.putInt("Facing", cachedFacing.get3DDataValue());
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Fluids", fluidTank.writeToNBT(registries, new CompoundTag()));
        
        // Save Ports
        tag.put("ItemPorts", savePortMap(itemPorts));
        tag.put("FluidPorts", savePortMap(fluidPorts));
        tag.put("EnergyPorts", savePortMap(energyPorts));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isFormed = tag.getBoolean("Formed");
        this.cachedFacing = Direction.from3DDataValue(tag.getInt("Facing"));
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Fluids")) this.fluidTank.readFromNBT(registries, tag.getCompound("Fluids"));
        
        loadPortMap(tag.getList("ItemPorts", Tag.TAG_COMPOUND), itemPorts);
        loadPortMap(tag.getList("FluidPorts", Tag.TAG_COMPOUND), fluidPorts);
        loadPortMap(tag.getList("EnergyPorts", Tag.TAG_COMPOUND), energyPorts);
    }

    private ListTag savePortMap(Map<Direction, BlockPos> map) {
        ListTag list = new ListTag();
        map.forEach((dir, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("D", dir.get3DDataValue());
            entry.putLong("P", pos.asLong());
            list.add(entry);
        });
        return list;
    }

    private void loadPortMap(ListTag list, Map<Direction, BlockPos> map) {
        map.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            map.put(Direction.from3DDataValue(entry.getInt("D")), BlockPos.of(entry.getLong("P")));
        }
    }

    // --- Getters ---
    public ItemStackHandler getInventory() { return inventory; }
    public FluidTank getFluidTank() { return fluidTank; }
    public boolean isFormed() { return isFormed; }
    public Direction getFacing() { return cachedFacing; }
}
