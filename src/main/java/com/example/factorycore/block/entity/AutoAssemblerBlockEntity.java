package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.example.factorycore.menu.AutoAssemblerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

public class AutoAssemblerBlockEntity extends AbstractFactoryMultiblockBlockEntity implements MenuProvider {
    private static final int ENERGY_PER_TICK = 500;
    private int progress = 0;
    private int maxProgress = 200;

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
        @Override public void set(int index, int value) { if (index == 1) progress = value; }
        @Override public int getCount() { return 2; }
    };

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
    }

    @Override
    protected int getInventorySize() {
        return 11; // 9 slots for 3x3 crafting grid + 1 output slot + 1 extra for flexibility
    }

    @Override
    public Component getDisplayName() { return Component.literal("Auto Assembler"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AutoAssemblerMenu(containerId, inventory, this, data);
    }

    @Override
    public MultiblockPattern getPattern() {
        MultiblockPattern p = new MultiblockPattern(3, 3, 3);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    boolean isEdge = (x == -1 || x == 1 || y == 0 || y == 2 || z == 0 || z == 2);
                    if (isEdge) {
                        if (x == 0 && y == 0 && z == 0) {
                            p.add(x, y, z, CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get());
                        } else {
                            p.add(x, y, z, CoreBlocks.MACHINE_CASING.get());
                        }
                    } else {
                        p.add(x, y, z, net.minecraft.world.level.block.Blocks.AIR);
                    }
                }
            }
        }
        return p;
    }

    @Override
    protected void serverTick() {
        IEnergyStorage energy = getFloorEnergy();
        if (energy == null || energy.getEnergyStored() < ENERGY_PER_TICK) {
            progress = 0;
            return;
        }

        // Check if we have a valid recipe in the 3x3 grid
        ItemStack result = getCraftingResult();
        if (result.isEmpty()) {
            progress = 0;
            return;
        }

        if (canOutput(result)) {
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;
            if (progress >= maxProgress) {
                // Consume the ingredients from the 3x3 grid
                consumeIngredients();
                // Output the result
                inventory.insertItem(10, result, false); // Output slot is 10
                progress = 0;
            }
            setChanged();
        } else {
            progress = 0;
        }
    }

    /**
     * Gets the crafting result for the current 3x3 grid
     */
    private ItemStack getCraftingResult() {
        // Create a SimpleContainer to represent the crafting grid
        net.minecraft.world.SimpleContainer craftingGrid = new net.minecraft.world.SimpleContainer(9);
        for (int i = 0; i < 9; i++) {
            craftingGrid.setItem(i, inventory.getStackInSlot(i).copy());
        }

        // Create the crafting input from the container
        net.minecraft.world.item.crafting.CraftingInput craftingInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, craftingGrid.getItems());

        // Get the recipe manager and find a matching recipe
        if (level != null) {
            java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, craftingInput, level);

            if (recipe.isPresent()) {
                return recipe.get().value().assemble(craftingInput, level.registryAccess());
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Consumes the ingredients from the 3x3 grid after crafting
     */
    private void consumeIngredients() {
        // Create a SimpleContainer to represent the crafting grid
        net.minecraft.world.SimpleContainer craftingGrid = new net.minecraft.world.SimpleContainer(9);
        for (int i = 0; i < 9; i++) {
            craftingGrid.setItem(i, inventory.getStackInSlot(i).copy());
        }

        // Create the crafting input from the container
        net.minecraft.world.item.crafting.CraftingInput craftingInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, craftingGrid.getItems());

        // Get the recipe manager and find a matching recipe
        if (level != null) {
            java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, craftingInput, level);

            if (recipe.isPresent()) {
                // Get the recipe's ingredients and match them to items in the grid
                java.util.List<net.minecraft.world.item.crafting.Ingredient> recipeIngredients = recipe.get().value().getIngredients();

                // For each ingredient in the recipe, find and consume a matching item from the grid
                for (net.minecraft.world.item.crafting.Ingredient ingredient : recipeIngredients) {
                    if (!ingredient.isEmpty()) {
                        // Find a matching item in the grid
                        for (int slot = 0; slot < 9; slot++) {
                            ItemStack stackInSlot = inventory.getStackInSlot(slot);
                            if (!stackInSlot.isEmpty() && ingredient.test(stackInSlot)) {
                                // Consume from the actual inventory
                                inventory.extractItem(slot, 1, false);
                                break; // Move to next ingredient
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack currentOutput = inventory.getStackInSlot(1);
        if (currentOutput.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOutput, stack)) return false;
        return currentOutput.getCount() + stack.getCount() <= stack.getMaxStackSize();
    }
}