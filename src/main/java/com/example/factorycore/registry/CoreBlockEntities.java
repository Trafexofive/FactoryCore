package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.block.entity.AutoAssemblerBlockEntity;
import com.example.factorycore.block.entity.ElectricFurnaceBlockEntity;
import com.example.factorycore.block.entity.ElectricalFloorBlockEntity;
import com.example.factorycore.block.entity.CreativeEnergySourceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, FactoryCore.MODID);

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = BLOCK_ENTITIES
                        .register("electric_furnace",
                                        () -> BlockEntityType.Builder
                                                        .of(ElectricFurnaceBlockEntity::new,
                                                                        CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get())
                                                        .build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoAssemblerBlockEntity>> AUTO_ASSEMBLER = BLOCK_ENTITIES
                        .register("auto_assembler",
                                        () -> BlockEntityType.Builder
                                                        .of(AutoAssemblerBlockEntity::new,
                                                                        CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get())
                                                        .build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricalFloorBlockEntity>> ELECTRICAL_FLOOR = BLOCK_ENTITIES
                        .register("electrical_floor",
                                        () -> BlockEntityType.Builder.of(ElectricalFloorBlockEntity::new,
                                                        CoreBlocks.ELECTRICAL_FLOOR.get()).build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEnergySourceBlockEntity>> CREATIVE_ENERGY_SOURCE = BLOCK_ENTITIES
                        .register("creative_energy_source",
                                        () -> BlockEntityType.Builder
                                                        .of(CreativeEnergySourceBlockEntity::new,
                                                                        CoreBlocks.CREATIVE_ENERGY_SOURCE.get())
                                                        .build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.factorycore.block.entity.ElectricalPoleBlockEntity>> ELECTRICAL_POLE = BLOCK_ENTITIES
                        .register("electrical_pole",
                                        () -> BlockEntityType.Builder.of(
                                                        com.example.factorycore.block.entity.ElectricalPoleBlockEntity::new,
                                                        CoreBlocks.ELECTRICAL_POLE.get()).build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.factorycore.block.entity.SolarPanelBlockEntity>> SOLAR_PANEL = BLOCK_ENTITIES
                        .register("solar_panel",
                                        () -> BlockEntityType.Builder.of(
                                                        com.example.factorycore.block.entity.SolarPanelBlockEntity::new,
                                                        CoreBlocks.SOLAR_PANEL.get()).build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.factorycore.block.entity.PipeBlockEntity>> PIPE = BLOCK_ENTITIES
                        .register("pipe",
                                        () -> BlockEntityType.Builder.of(
                                                        com.example.factorycore.block.entity.PipeBlockEntity::new,
                                                        CoreBlocks.PIPE.get()).build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.example.factorycore.block.entity.BatteryBlockEntity>> BATTERY = BLOCK_ENTITIES
                        .register("battery",
                                        () -> BlockEntityType.Builder.of(
                                                        com.example.factorycore.block.entity.BatteryBlockEntity::new,
                                                        CoreBlocks.BATTERY.get()).build(null));

        public static void register(IEventBus eventBus) {
                BLOCK_ENTITIES.register(eventBus);
        }
}
