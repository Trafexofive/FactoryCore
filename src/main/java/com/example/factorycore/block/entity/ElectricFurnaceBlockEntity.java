package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import com.example.factorycore.util.MultiblockPatterns;
import com.example.factorycore.ui.FactoryUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ElectricFurnaceBlockEntity extends AbstractFactoryMultiblockBlockEntity implements net.minecraft.world.MenuProvider {
    private int progress = 0;
    private int maxProgress = 100;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.example.factorycore.menu.ElectricFurnaceMenu((net.minecraft.world.inventory.MenuType)com.example.factorycore.registry.CoreMenus.ELECTRIC_FURNACE_MENU.get(), windowId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Electric Furnace MK1");
    }

    @Override
    public com.lowdragmc.lowdraglib2.gui.ui.ModularUI createUI(net.minecraft.world.entity.player.Player player) {
        System.out.println("ElectricFurnace: createUI called");
        com.lowdragmc.lowdraglib2.gui.ui.UI ui = com.lowdragmc.lowdraglib2.gui.ui.UI.empty();
        ui.getRootElement().layout(l -> FactoryUI.apply(l, 0f, 0f, 176f, 166f));
        ui.getRootElement().style(s -> s.background(com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites.RECT));
        
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.Label().setValue(getDisplayName()).layout(l -> FactoryUI.margin(l, 5f, 0f, 5f, 0f)));
        
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot().bind(inventory, 0).layout(l -> FactoryUI.pos(l, 56f, 17f)));
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot().bind(inventory, 1).layout(l -> FactoryUI.pos(l, 116f, 35f)));
        
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar().bindDataSource(FactoryUI.supplier(() -> (float) progress / maxProgress))
                .layout(l -> FactoryUI.apply(l, 79f, 34f, 24f, 17f)));
        
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar().bindDataSource(FactoryUI.supplier(() -> {
            net.neoforged.neoforge.energy.IEnergyStorage e = getFloorEnergy();
            return e != null ? (float) e.getEnergyStored() / e.getMaxEnergyStored() : 0f;
        })).layout(l -> FactoryUI.apply(l, 10f, 17f, 10f, 54f)));
        
        ui.getRootElement().addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots().layout(l -> FactoryUI.pos(l, 8f, 84f)));
        
        return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(ui, player);
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.ELECTRIC_FURNACE;
    }

    @Override
    protected void serverTick() {
        if (!isFormed() || level == null) return;

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            if (progress > 0) progress = 0;
            return;
        }

        net.neoforged.neoforge.energy.IEnergyStorage energy = getFloorEnergy();
        if (energy != null && energy.getEnergyStored() >= 20) {
            progress++;
            energy.extractEnergy(20, false);
            
            if (progress >= maxProgress) {
                ItemStack result = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
                if (inventory.insertItem(1, result, true).isEmpty()) {
                    inventory.insertItem(1, result, false);
                    inventory.extractItem(0, 1, false);
                    progress = 0;
                }
            }
            setChanged();
        }
    }
}
