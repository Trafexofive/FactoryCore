package com.example.factorycore.block.entity;

import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
 * Enhanced base class for FactoryCore multiblocks.
 * Handles structure validation, capability caching for I/O ports, and transfer helpers.
 */
public abstract class AbstractFactoryMultiblockBlockEntity extends BlockEntity {
    protected boolean isFormed = false;
    protected int checkTimer = 0;

    // --- Standard Storage ---
    protected final ItemStackHandler inventory = createItemHandler();
    protected final FluidTank fluidTank = createFluidTank();

    // --- Capability Caches ---
    protected final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> itemCaches = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockCapabilityCache<IFluidHandler, Direction>> fluidCaches = new EnumMap<>(Direction.class);
    protected final Map<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);

    public AbstractFactoryMultiblockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- Abstract Methods ---
    public abstract MultiblockPattern getPattern();
    protected abstract void serverTick();

    // --- Configuration Overrides ---
    protected ItemStackHandler createItemHandler() {
        return new ItemStackHandler(2) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }
        };
    }

    protected FluidTank createFluidTank() {
        return new FluidTank(16000) {
            @Override
            protected void onContentsChanged() { setChanged(); }
        };
    }

    // --- Core Logic ---

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
        
        BlockState state = getBlockState();
        Direction facing = Direction.NORTH;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }

        boolean nowFormed = getPattern().matches(level, worldPosition, facing);
        
        if (nowFormed != this.isFormed) {
            this.isFormed = nowFormed;
            onFormationChanged(this.isFormed);
        }
    }

    protected void onFormationChanged(boolean formed) {
        setChanged();
        if (formed) {
            initCapabilityCaches();
            if (level != null) {
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        } else {
            clearCapabilityCaches();
        }
    }

    private void initCapabilityCaches() {
        if (level == null || level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(dir);
            itemCaches.put(dir, BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, (net.minecraft.server.level.ServerLevel) level, targetPos, dir.getOpposite()));
            fluidCaches.put(dir, BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, (net.minecraft.server.level.ServerLevel) level, targetPos, dir.getOpposite()));
            energyCaches.put(dir, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, (net.minecraft.server.level.ServerLevel) level, targetPos, dir.getOpposite()));
        }
    }

    private void clearCapabilityCaches() {
        itemCaches.clear();
        fluidCaches.clear();
        energyCaches.clear();
    }

    // --- Transfer Helpers ---

    protected void pushItems(int slot, int amount, Direction toSide) {
        BlockCapabilityCache<IItemHandler, Direction> cache = itemCaches.get(toSide);
        if (cache == null) return;
        
        IItemHandler target = cache.getCapability();
        if (target != null) {
            net.minecraft.world.item.ItemStack stack = inventory.extractItem(slot, amount, true);
            if (!stack.isEmpty()) {
                net.minecraft.world.item.ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack, false);
                int accepted = stack.getCount() - remainder.getCount();
                inventory.extractItem(slot, accepted, false);
            }
        }
    }

    protected void pullItems(int slot, int amount, Direction fromSide) {
        BlockCapabilityCache<IItemHandler, Direction> cache = itemCaches.get(fromSide);
        if (cache == null) return;

        IItemHandler target = cache.getCapability();
        if (target != null) {
            for (int i = 0; i < target.getSlots(); i++) {
                net.minecraft.world.item.ItemStack canExtract = target.extractItem(i, amount, true);
                if (!canExtract.isEmpty()) {
                    net.minecraft.world.item.ItemStack remainder = inventory.insertItem(slot, canExtract, false);
                    int accepted = canExtract.getCount() - remainder.getCount();
                    target.extractItem(i, accepted, false);
                    if (accepted > 0) break;
                }
            }
        }
    }

    protected int extractEnergy(int amount, boolean simulate) {
        IEnergyStorage floor = getFloorEnergy();
        if (floor != null) return floor.extractEnergy(amount, simulate);

        for (BlockCapabilityCache<IEnergyStorage, Direction> cache : energyCaches.values()) {
            IEnergyStorage storage = cache.getCapability();
            if (storage != null && storage.canExtract()) {
                return storage.extractEnergy(amount, simulate);
            }
        }
        return 0;
    }

    protected IEnergyStorage getFloorEnergy() {
        if (level == null) return null;
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.below(), Direction.UP);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", isFormed);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Fluids", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isFormed = tag.getBoolean("Formed");
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.fluidTank.readFromNBT(registries, tag.getCompound("Fluids"));
    }

    public ItemStackHandler getInventory() { return inventory; }
    public FluidTank getFluidTank() { return fluidTank; }
    public boolean isFormed() { return isFormed; }
}