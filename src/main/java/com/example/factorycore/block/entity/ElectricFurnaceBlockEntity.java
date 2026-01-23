package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlockEntity extends BlockEntity {
    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }
}
