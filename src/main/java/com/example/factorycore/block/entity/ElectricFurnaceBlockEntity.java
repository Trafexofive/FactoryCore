package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlockEntity extends AbstractFactoryMultiblockBlockEntity implements IContainerUIHolder {
    private int energy = 0;
    private int maxEnergy = 10000;
    private int progress = 0;
    private int maxProgress = 100;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    @Override
    public ModularUI createUI(Player player) {
        return ModularUI.of(UI.empty(), player);
    }

    @Override
    public boolean isStillValid(Player player) {
        return true;
    }

    @Override
    public MultiblockPattern getPattern() {
        return com.example.factorycore.util.MultiblockPatterns.ELECTRIC_FURNACE;
    }

    @Override
    protected void serverTick() {
        if (isFormed()) {
            // Logic
        }
    }
}