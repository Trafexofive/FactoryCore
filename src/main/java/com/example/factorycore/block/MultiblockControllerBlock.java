package com.example.factorycore.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class MultiblockControllerBlock extends Block implements EntityBlock {
    public MultiblockControllerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // This will be overridden or handled by specific registry logic if we want generic, 
        // but for now we are using it as a base. 
        // Actually, we registered specific blocks using this class, so we need to know WHICH TE to create.
        // Or we make subclasses. Let's make subclasses to be cleaner.
        return null; 
    }
}
