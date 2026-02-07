package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.menu.AutoAssemblerMenu;
import com.example.factorycore.menu.BatteryMenu;
import com.example.factorycore.menu.ElectricFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

public class CoreMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            FactoryCore.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU = MENUS.register("electric_furnace",
            () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AutoAssemblerMenu>> AUTO_ASSEMBLER_MENU = MENUS.register("auto_assembler",
            () -> IMenuTypeExtension.create(AutoAssemblerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryMenu>> BATTERY_MENU = MENUS.register("battery",
            () -> IMenuTypeExtension.create(BatteryMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}