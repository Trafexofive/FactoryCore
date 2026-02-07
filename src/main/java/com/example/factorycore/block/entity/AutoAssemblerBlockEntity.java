package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import com.example.factorycore.util.MultiblockPatterns;
import com.example.factorycore.ui.FactoryUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AutoAssemblerBlockEntity extends AbstractFactoryMultiblockBlockEntity implements net.minecraft.world.MenuProvider {
    private boolean isLocked = false;
    private boolean isActive = false;
    private int progress = 0;
    private int maxProgress = 20; // 1 second @ 20tps

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
    }

    @Override
    protected int getInventorySize() { return 11; } // 9 grid + 2 output

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsLocked", isLocked);
        tag.putBoolean("IsActive", isActive);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isLocked = tag.getBoolean("IsLocked");
        this.isActive = tag.getBoolean("IsActive");
        this.progress = tag.getInt("Progress");
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.example.factorycore.menu.AutoAssemblerMenu((net.minecraft.world.inventory.MenuType)com.example.factorycore.registry.CoreMenus.AUTO_ASSEMBLER_MENU.get(), windowId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Auto Assembler");
    }

    @Override
    public com.lowdragmc.lowdraglib2.gui.ui.ModularUI createUI(net.minecraft.world.entity.player.Player player) {
        try {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("factorycore", "gui/auto_assembler.xml");
            System.out.println("AutoAssembler: Loading XML from " + loc);
            
            org.w3c.dom.Document doc = com.lowdragmc.lowdraglib2.utils.XmlUtils.loadXml(loc);
            if (doc == null) {
                System.out.println("AutoAssembler: Document is NULL");
                return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(com.lowdragmc.lowdraglib2.gui.ui.UI.empty(), player);
            }

            com.lowdragmc.lowdraglib2.gui.ui.UI ui = com.lowdragmc.lowdraglib2.gui.ui.UI.of(doc);
            if (ui == null) {
                System.out.println("AutoAssembler: UI is NULL");
                return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(com.lowdragmc.lowdraglib2.gui.ui.UI.empty(), player);
            }

            System.out.println("AutoAssembler: UI Loaded. Root: " + ui.getRootElement().getClass().getSimpleName());

            // Bind Slots
            for (int i = 0; i <= 10; i++) {
                final int index = i;
                ui.select("slot_" + i, com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot.class)
                  .forEach(slot -> slot.bind(inventory, index));
            }

            // Bind Buttons
            ui.select("lock_btn", com.lowdragmc.lowdraglib2.gui.ui.elements.Button.class).forEach(btn -> {
                btn.setOnClick(click -> {
                    this.isLocked = !this.isLocked;
                    setChanged();
                });
            });

            ui.select("toggle_btn", com.lowdragmc.lowdraglib2.gui.ui.elements.Button.class).forEach(btn -> {
                btn.setOnClick(click -> {
                    this.isActive = !this.isActive;
                    btn.setText(isActive ? "Stop" : "Start");
                    setChanged();
                });
                btn.setText(isActive ? "Stop" : "Start");
            });

            // Bind Dynamic Labels
            ui.select("lock_status", com.lowdragmc.lowdraglib2.gui.ui.elements.Label.class).forEach(label -> {
                label.bindDataSource(FactoryUI.supplier(() -> Component.literal(isLocked ? "Locked" : "Unlocked")));
            });

            // Bind Energy
            ui.select("energy_bar", com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar.class).forEach(bar -> {
                bar.bindDataSource(FactoryUI.supplier(() -> {
                    net.neoforged.neoforge.energy.IEnergyStorage e = getFloorEnergy();
                    return e != null ? (float) e.getEnergyStored() / e.getMaxEnergyStored() : 0f;
                }));
            });

            return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(ui, player);
        } catch (Exception e) {
            System.err.println("AutoAssembler: CRITICAL ERROR IN createUI");
            e.printStackTrace();
            return com.lowdragmc.lowdraglib2.gui.ui.ModularUI.of(com.lowdragmc.lowdraglib2.gui.ui.UI.empty(), player);
        }
    }

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.AUTO_ASSEMBLER;
    }

    @Override
    protected void serverTick() {
        if (!isFormed() || level == null || !isActive) {
            if (progress > 0) progress = 0;
            return;
        }

        // 1. Create temporary crafting input for matching
        net.minecraft.world.inventory.CraftingContainer container = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) { return net.minecraft.world.item.ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        }, 3, 3);

        for (int i = 0; i < 9; i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }

        net.minecraft.world.item.crafting.CraftingInput input = container.asCraftInput();

        // 2. Find Recipe
        var recipeOptional = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        
        if (recipeOptional.isPresent()) {
            net.minecraft.world.item.ItemStack result = recipeOptional.get().value().assemble(input, level.registryAccess());
            
            // 3. Check Output Space (Slots 9 and 10)
            boolean canFit = false;
            int targetSlot = -1;
            for (int i = 9; i <= 10; i++) {
                if (inventory.insertItem(i, result, true).isEmpty()) {
                    canFit = true;
                    targetSlot = i;
                    break;
                }
            }

            if (canFit && targetSlot != -1) {
                // 4. Energy Check
                net.neoforged.neoforge.energy.IEnergyStorage energy = getFloorEnergy();
                if (energy != null && energy.getEnergyStored() >= 50) {
                    energy.extractEnergy(50, false);
                    progress++;
                    
                    if (progress >= maxProgress) {
                        // 5. Finalize Craft
                        inventory.insertItem(targetSlot, result, false);
                        
                        // Consume inputs (Unless locked, but Factorio 'lock' usually means slot filter, 
                        // here we'll assume lock means 'keep items in grid if possible' or similar. 
                        // Let's stick to standard consume for now to keep it simple).
                        for (int i = 0; i < 9; i++) {
                            inventory.extractItem(i, 1, false);
                        }
                        progress = 0;
                    }
                    setChanged();
                }
            } else {
                if (progress > 0) progress = 0;
            }
        } else {
            if (progress > 0) progress = 0;
        }
    }
}
