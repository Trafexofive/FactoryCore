package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Optional;

import com.example.factorycore.menu.ElectricFurnaceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

public class ElectricFurnaceBlockEntity extends AbstractFactoryMultiblockBlockEntity implements MenuProvider {
    private static final int ENERGY_PER_TICK = 100;
    private int progress = 0;
    private int maxProgress = 100;
    
    // Sync Energy to Client Menu
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            IEnergyStorage e = getFloorEnergy();
            return switch (index) {
                case 0 -> e != null ? e.getEnergyStored() : 0;
                case 1 -> progress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) progress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Electric Furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ElectricFurnaceMenu(containerId, inventory, this, data);
    }

    @Override
    public MultiblockPattern getPattern() {
        MultiblockPattern p = new MultiblockPattern(3, 3, 3);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = 0; z <= 2; z++) {
                    // Hollow check (boundary of -1..1, -1..1, 0..2)
                    boolean isEdge = (x == -1 || x == 1 || y == -1 || y == 1 || z == 0 || z == 2);
                    if (isEdge) {
                        if (x == 0 && y == 0 && z == 0) {
                            // Controller position (0,0,0)
                            p.add(x, y, z, CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get());
                        } else {
                            p.add(x, y, z, CoreBlocks.MACHINE_CASING.get());
                        }
                    } else {
                        // Enforce hollow center
                        p.add(x, y, z, net.minecraft.world.level.block.Blocks.AIR);
                    }
                }
            }
        }
        return p;
    }

    /**
     * Specific Machine Logic:
     * 1. Pull power from Floor (Block below).
     * 2. If power sufficient -> Process item.
     */
    @Override
    protected void serverTick() {
        IEnergyStorage energy = getFloorEnergy();
        if (energy == null || energy.getEnergyStored() < ENERGY_PER_TICK) {
            progress = 0; // Reset progress if power lost (penalty)
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            progress = 0;
            return;
        }

        // Logic: Smelt input to output
        // For now, let's just use vanilla Smelting recipes or simple mock
        // Since we are technical, we'll use actual RecipeManager later.
        // For prototype: If Iron Ingot -> Steel (mock) or just Vanilla Smelt.
        
        Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, 
            new net.minecraft.world.item.crafting.SingleRecipeInput(input), level).map(h -> h.value());

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().getResultItem(level.registryAccess());
            if (canOutput(result)) {
                energy.extractEnergy(ENERGY_PER_TICK, false);
                progress++;
                if (progress >= maxProgress) {
                    inventory.extractItem(0, 1, false);
                    inventory.insertItem(1, result.copy(), false);
                    progress = 0;
                }
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack currentOutput = inventory.getStackInSlot(1);
        if (currentOutput.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOutput, stack)) return false;
        return currentOutput.getCount() + stack.getCount() <= stack.getMaxStackSize();
    }
}