package com.example.factorycore.block;

import com.example.factorycore.block.entity.AbstractFactoryMultiblockBlockEntity;
import com.example.factorycore.block.entity.ElectricFurnaceBlockEntity;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ElectricFurnaceControllerBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<ElectricFurnaceControllerBlock> CODEC = simpleCodec(ElectricFurnaceControllerBlock::new);

    public ElectricFurnaceControllerBlock(Properties properties) {
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
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CoreBlockEntities.ELECTRIC_FURNACE.get(), AbstractFactoryMultiblockBlockEntity::tick);
    }
}