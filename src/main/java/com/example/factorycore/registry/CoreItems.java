package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FactoryCore.MODID);

    public static final DeferredItem<Item> MACHINE_CASING = ITEMS.register("machine_casing",
            () -> new BlockItem(CoreBlocks.MACHINE_CASING.get(), new Item.Properties()));

    public static final DeferredItem<Item> ELECTRIC_FURNACE_CONTROLLER = ITEMS.register("electric_furnace_controller",
            () -> new BlockItem(CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredItem<Item> AUTO_ASSEMBLER_CONTROLLER = ITEMS.register("auto_assembler_controller",
            () -> new BlockItem(CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredItem<Item> ELECTRICAL_FLOOR = ITEMS.register("electrical_floor",
            () -> new BlockItem(CoreBlocks.ELECTRICAL_FLOOR.get(), new Item.Properties()));

    public static final DeferredItem<Item> CREATIVE_ENERGY_SOURCE = ITEMS.register("creative_energy_source",
            () -> new BlockItem(CoreBlocks.CREATIVE_ENERGY_SOURCE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}