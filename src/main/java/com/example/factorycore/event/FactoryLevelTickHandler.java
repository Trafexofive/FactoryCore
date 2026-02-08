package com.example.factorycore.event;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.power.FactoryNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FactoryLevelTickHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            FactoryNetworkManager manager = FactoryNetworkManager.get(serverLevel);
            if (manager != null) {
                manager.tick(serverLevel);
            }
        }
    }
}
