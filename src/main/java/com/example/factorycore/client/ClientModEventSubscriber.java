package com.example.factorycore.client;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.registry.CoreMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventSubscriber {
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {

        }

        @SubscribeEvent
        public static void registerRenderers(
                        net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
                event.registerBlockEntityRenderer(
                                com.example.factorycore.registry.CoreBlockEntities.ELECTRICAL_POLE.get(),
                                com.example.factorycore.client.renderer.ElectricalPoleRenderer::new);
                event.registerBlockEntityRenderer(
                                com.example.factorycore.registry.CoreBlockEntities.ELECTRIC_FURNACE.get(),
                                com.example.factorycore.client.renderer.FactoryMultiblockRenderer::new);
                event.registerBlockEntityRenderer(
                                com.example.factorycore.registry.CoreBlockEntities.AUTO_ASSEMBLER.get(),
                                com.example.factorycore.client.renderer.FactoryMultiblockRenderer::new);
        }
}
