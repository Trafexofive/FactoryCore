package com.example.factorycore.client;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.client.gui.ElectricFurnaceScreen;
import com.example.factorycore.registry.CoreMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventSubscriber {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CoreMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(CoreMenus.AUTO_ASSEMBLER.get(), com.example.factorycore.client.gui.AutoAssemblerScreen::new);
    }
}
