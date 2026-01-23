package com.example.factorycore.registry;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.block.AutoAssemblerControllerBlock;
import com.example.factorycore.block.ElectricFurnaceControllerBlock;
import com.example.factorycore.block.MultiblockMemberBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CoreBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FactoryCore.MODID);

    // Casings
    public static final DeferredBlock<Block> MACHINE_CASING = BLOCKS.register("machine_casing",
            () -> new MultiblockMemberBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).sound(SoundType.METAL)));

    // Controllers
    public static final DeferredBlock<Block> ELECTRIC_FURNACE_CONTROLLER = BLOCKS.register("electric_furnace_controller",
            () -> new ElectricFurnaceControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).sound(SoundType.METAL)));

    public static final DeferredBlock<Block> AUTO_ASSEMBLER_CONTROLLER = BLOCKS.register("auto_assembler_controller",
            () -> new AutoAssemblerControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
