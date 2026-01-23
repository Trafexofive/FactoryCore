package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AutoAssemblerBlockEntity extends BlockEntity {
    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.AUTO_ASSEMBLER.get(), pos, state);
    }
}
