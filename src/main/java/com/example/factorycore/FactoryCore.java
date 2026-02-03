package com.example.factorycore;

import com.example.factorycore.registry.CoreBlocks;

import com.example.factorycore.registry.CoreBlockEntities;

import com.example.factorycore.registry.CoreItems;

import com.example.factorycore.registry.CoreChunkGenerators;

import com.example.factorycore.registry.CoreMenus;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.common.Mod;



@Mod(FactoryCore.MODID)

public class FactoryCore {

    public static final String MODID = "factorycore";



        public FactoryCore(IEventBus modEventBus) {
        com.example.factorycore.util.FactoryLogger.init();

        com.example.factorycore.registry.CoreCreativeTabs.register(modEventBus);



            CoreBlocks.register(modEventBus);



            CoreItems.register(modEventBus);



    

        CoreBlockEntities.register(modEventBus);

        CoreChunkGenerators.register(modEventBus);

        CoreMenus.register(modEventBus);

    }

}
