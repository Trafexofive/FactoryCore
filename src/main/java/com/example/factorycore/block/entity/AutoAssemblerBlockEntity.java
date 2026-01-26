package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class AutoAssemblerBlockEntity extends AbstractFactoryMultiblockBlockEntity {
    private static final int ENERGY_PER_TICK = 500;
    private int progress = 0;
    private int maxProgress = 200;

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
    }

    @Override
    public MultiblockPattern getPattern() {
        MultiblockPattern p = new MultiblockPattern(3, 3, 3);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    if (x == 0 || x == 2 || y == 0 || y == 2 || z == 0 || z == 2) {
                        if (x == 1 && y == 1 && z == 0) {
                            p.add(x, y, z, CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get());
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

        // Test Logic: Duplicate Item
        ItemStack result = input.copy();
        result.setCount(1);

        if (canOutput(result)) {
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;
            if (progress >= maxProgress) {
                inventory.insertItem(1, result, false);
                progress = 0;
            }
            setChanged();
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