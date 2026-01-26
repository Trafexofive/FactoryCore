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
    public Component getDisplayName() { return Component.literal("Auto Assembler"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AutoAssemblerMenu(containerId, inventory, this, data);
    }

    @Override
    public MultiblockPattern getPattern() {
        MultiblockPattern p = new MultiblockPattern(3, 3, 3);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = 0; z <= 2; z++) {
                    boolean isEdge = (x == -1 || x == 1 || y == -1 || y == 1 || z == 0 || z == 2);
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