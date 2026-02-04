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

public class AutoAssemblerBlockEntity extends AbstractFactoryMultiblockBlockEntity implements com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUI {

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
    }

    @Override
    public ModularUI createUI(com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder holder) {
        UI ui = UI.empty();
        ui.getRootElement().addChild(new Label().setValue(net.minecraft.network.chat.Component.literal("Auto Assembler")).layout(l -> FactoryUI.margin(l, 5f, 0f, 5f, 0f)));
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                float x = 30f + j * 18f;
                float y = 17f + i * 18f;
                ui.getRootElement().addChild(new ItemSlot().bind(inventory, index).layout(l -> FactoryUI.pos(l, x, y)));
            }
        }

        ui.getRootElement().addChild(new ItemSlot().bind(inventory, 9).layout(l -> FactoryUI.pos(l, 124f, 35f)));
        
        ui.getRootElement().addChild(new ProgressBar().bindDataSource(FactoryUI.supplier(() -> 1.0f))
                .layout(l -> FactoryUI.apply(l, 10f, 17f, 10f, 54f)));
        
        ui.getRootElement().addChild(new InventorySlots().layout(l -> FactoryUI.bottom(l, 5f, 8f)));
        return ModularUI.of(ui, holder.player);
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.AUTO_ASSEMBLER;
    }

    @Override
    protected void serverTick() {
    }
}