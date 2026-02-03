package com.example.factorycore.block;

import com.example.factorycore.block.entity.ElectricalFloorBlockEntity;
import com.example.factorycore.power.FactoryNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ElectricalFloorBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<ElectricalFloorBlock> CODEC = simpleCodec(ElectricalFloorBlock::new);

    public ElectricalFloorBlock(Properties properties) {
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
        return new ElectricalFloorBlockEntity(pos, state);
    }

    /**
     * Business Logic: Registration
     * When placed, this block MUST notify the global NetworkManager to join or create an energy island.
     * This happens Server-Side only.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            FactoryNetworkManager manager = FactoryNetworkManager.get(level);
            if (manager != null) {
                manager.addNode(pos);
                manager.setDirty();
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    /**
     * Business Logic: Deregistration
     * When broken, this block tells the manager to remove it from the graph.
     * This might trigger network deletion if it was the last node.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            FactoryNetworkManager.get(level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
