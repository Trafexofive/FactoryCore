package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.block.entity.AutoAssemblerBlockEntity;
import com.example.factorycore.block.entity.ElectricFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, FactoryCore.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = BLOCK_ENTITIES.register("electric_furnace",
            () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoAssemblerBlockEntity>> AUTO_ASSEMBLER = BLOCK_ENTITIES.register("auto_assembler",
            () -> BlockEntityType.Builder.of(AutoAssemblerBlockEntity::new, CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
