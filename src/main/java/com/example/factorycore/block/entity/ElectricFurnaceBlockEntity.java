package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import com.example.factorycore.util.MultiblockPatterns;
import com.example.factorycore.ui.FactoryUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlockEntity extends AbstractFactoryMultiblockBlockEntity implements com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUI {
    private int energy = 0;
    private int maxEnergy = 10000;
    private int progress = 0;
    private int maxProgress = 100;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    @Override
    public ModularUI createUI(com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder holder) {
        UI ui = UI.empty();
        ui.getRootElement().addChild(new Label().setValue(net.minecraft.network.chat.Component.literal("Electric Furnace MK1")).layout(l -> FactoryUI.margin(l, 5f, 0f, 5f, 0f)));
        
        ui.getRootElement().addChild(new ItemSlot().bind(inventory, 0).layout(l -> FactoryUI.pos(l, 56f, 17f)));
        ui.getRootElement().addChild(new ItemSlot().bind(inventory, 1).layout(l -> FactoryUI.pos(l, 116f, 35f)));
        
        ui.getRootElement().addChild(new ProgressBar().bindDataSource(FactoryUI.supplier(() -> (float) progress / maxProgress))
                .layout(l -> FactoryUI.apply(l, 79f, 34f, 24f, 17f)));
        
        ui.getRootElement().addChild(new ProgressBar().bindDataSource(FactoryUI.supplier(() -> 1.0f))
                .layout(l -> FactoryUI.apply(l, 10f, 17f, 10f, 54f)));
        
        ui.getRootElement().addChild(new InventorySlots().layout(l -> FactoryUI.bottom(l, 5f, 8f)));
        return ModularUI.of(ui, holder.player);
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.ELECTRIC_FURNACE;
    }

    @Override
    protected void serverTick() {
    }
}