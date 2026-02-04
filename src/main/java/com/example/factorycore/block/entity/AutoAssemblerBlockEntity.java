package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class AutoAssemblerBlockEntity extends AbstractFactoryMultiblockBlockEntity implements IContainerUIHolder {

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
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
        return com.example.factorycore.util.MultiblockPatterns.AUTO_ASSEMBLER;
    }

    @Override
    protected void serverTick() {
    }
}