package com.example.factorycore.block;

import com.example.factorycore.block.entity.AutoAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AutoAssemblerControllerBlock extends MultiblockControllerBlock {
    public AutoAssemblerControllerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoAssemblerBlockEntity(pos, state);
    }
}
