package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

public class CoreMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            FactoryCore.MODID);

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
