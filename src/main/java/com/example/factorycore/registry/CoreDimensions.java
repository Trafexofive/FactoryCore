package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class CoreDimensions {
    public static final ResourceKey<DimensionType> ENGINEER_PLANE_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(FactoryCore.MODID, "engineer_plane")
    );

    public static final ResourceKey<Level> ENGINEER_PLANE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(FactoryCore.MODID, "engineer_plane")
    );

    public static void register() {
        // ResourceKeys don't need explicit DeferredRegister, they are just keys for data-driven content.
        // But having the class ensures we have the reference.
    }
}
