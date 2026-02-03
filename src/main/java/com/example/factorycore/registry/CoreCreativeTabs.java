package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, FactoryCore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACTORYCORE_TAB = CREATIVE_MODE_TABS.register("factorycore_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("FactoryCore"))
                    .icon(() -> new ItemStack(CoreItems.ELECTRIC_FURNACE_CONTROLLER.get()))
                    .displayItems((params, output) -> {
                        output.accept(CoreItems.MACHINE_CASING.get());
                        output.accept(CoreItems.ELECTRICAL_FLOOR.get());
                        output.accept(CoreItems.ELECTRICAL_POLE.get());
                        output.accept(CoreItems.CREATIVE_ENERGY_SOURCE.get());
                        output.accept(CoreItems.ELECTRIC_FURNACE_CONTROLLER.get());
                        output.accept(CoreItems.AUTO_ASSEMBLER_CONTROLLER.get());
                        output.accept(CoreItems.BATTERY.get());
                        output.accept(CoreItems.SOLAR_PANEL.get());
                        output.accept(CoreItems.PIPE.get());
                        output.accept(CoreItems.WRENCH.get());
                        output.accept(CoreItems.MULTIMETER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
