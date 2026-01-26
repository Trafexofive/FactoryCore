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

public class ElectricFurnaceBlockEntity extends AbstractFactoryMultiblockBlockEntity {
    private static final int ENERGY_PER_TICK = 100;
    private int progress = 0;
    private int maxProgress = 100;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    @Override
    public MultiblockPattern getPattern() {
        MultiblockPattern p = new MultiblockPattern(3, 3, 3);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    // Hollow check
                    if (x == 0 || x == 2 || y == 0 || y == 2 || z == 0 || z == 2) {
                        if (x == 1 && y == 1 && z == 0) {
                            // Controller position relative to pattern
                            p.add(x, y, z, CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get());
                        } else {
                            p.add(x, y, z, CoreBlocks.MACHINE_CASING.get());
                        }
                    }
                }
            }
        }
        return p;
    }

    @Override
    public BlockPos getPatternOffset() {
        // Controller is at (1, 1, 0) relative to bottom-back-left (0,0,0)
        return new BlockPos(1, 1, 0);
    }

    @Override
    protected void serverTick() {
        IEnergyStorage energy = getFloorEnergy();
        if (energy == null || energy.getEnergyStored() < ENERGY_PER_TICK) {
            progress = 0;
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