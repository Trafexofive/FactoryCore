package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FactoryCore.MODID);

        public static final DeferredBlock<Block> MACHINE_CASING = BLOCKS.register("machine_casing",
                        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                                        .sound(SoundType.METAL)));

        public static final DeferredBlock<Block> ELECTRIC_FURNACE_CONTROLLER = BLOCKS.register(
                        "electric_furnace_controller",
                        () -> new com.example.factorycore.block.ElectricFurnaceControllerBlock(BlockBehaviour.Properties
                                        .of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

        public static final DeferredBlock<Block> AUTO_ASSEMBLER_CONTROLLER = BLOCKS.register(
                        "auto_assembler_controller",
                        () -> new com.example.factorycore.block.AutoAssemblerControllerBlock(BlockBehaviour.Properties
                                        .of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

        public static final DeferredBlock<Block> ELECTRICAL_FLOOR = BLOCKS.register("electrical_floor",
                        () -> new com.example.factorycore.block.ElectricalFloorBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));

        public static final DeferredBlock<Block> CREATIVE_ENERGY_SOURCE = BLOCKS.register("creative_energy_source",
                        () -> new com.example.factorycore.block.CreativeEnergySourceBlock(
                                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                                        .strength(-1.0f, 3600000.0f).sound(SoundType.METAL)));

        public static final DeferredBlock<Block> ELECTRICAL_POLE = BLOCKS.register("electrical_pole",
                        () -> new com.example.factorycore.block.ElectricalPoleBlock());

        public static final DeferredBlock<Block> SOLAR_PANEL = BLOCKS.register("solar_panel",
                        () -> new com.example.factorycore.block.SolarPanelBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> BATTERY = BLOCKS.register("battery",
                        () -> new com.example.factorycore.block.BatteryBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }
}