package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.world.gen.EngineerPlaneChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, FactoryCore.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<EngineerPlaneChunkGenerator>> ENGINEER_PLANE = CHUNK_GENERATORS.register("engineer_plane", () -> EngineerPlaneChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }
}
