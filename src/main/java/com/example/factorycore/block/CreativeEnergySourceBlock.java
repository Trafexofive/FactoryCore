package com.example.factorycore.block;

import com.example.factorycore.block.entity.CreativeEnergySourceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeEnergySourceBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<CreativeEnergySourceBlock> CODEC = simpleCodec(CreativeEnergySourceBlock::new);

    public CreativeEnergySourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergySourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, com.example.factorycore.registry.CoreBlockEntities.CREATIVE_ENERGY_SOURCE.get(), CreativeEnergySourceBlockEntity::tick);
    }
}
