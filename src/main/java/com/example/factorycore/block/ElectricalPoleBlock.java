package com.example.factorycore.block;

import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ElectricalPoleBlock extends BaseEntityBlock {
    public enum PolePart implements StringRepresentable {
        BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

        private final String name;

        PolePart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<PolePart> PART = EnumProperty.create("part", PolePart.class);

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public static final com.mojang.serialization.MapCodec<ElectricalPoleBlock> CODEC = simpleCodec(
            ElectricalPoleBlock::new);

    public ElectricalPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, PolePart.BOTTOM));
    }

    public ElectricalPoleBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)
                .noOcclusion());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricalPoleBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 2 &&
                level.getBlockState(pos.above()).canBeReplaced(context) &&
                level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return this.defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        level.setBlock(pos.above(), this.defaultBlockState().setValue(PART, PolePart.MIDDLE), 3);
        level.setBlock(pos.above(2), this.defaultBlockState().setValue(PART, PolePart.TOP), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        PolePart part = state.getValue(PART);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (part == PolePart.BOTTOM && direction == Direction.UP) {
                if (!neighborState.is(this) || neighborState.getValue(PART) != PolePart.MIDDLE)
                    return Blocks.AIR.defaultBlockState();
            }
            if (part == PolePart.MIDDLE) {
                if (direction == Direction.UP
                        && (!neighborState.is(this) || neighborState.getValue(PART) != PolePart.TOP))
                    return Blocks.AIR.defaultBlockState();
                if (direction == Direction.DOWN
                        && (!neighborState.is(this) || neighborState.getValue(PART) != PolePart.BOTTOM))
                    return Blocks.AIR.defaultBlockState();
            }
            if (part == PolePart.TOP && direction == Direction.DOWN) {
                if (!neighborState.is(this) || neighborState.getValue(PART) != PolePart.MIDDLE)
                    return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Only disconnect if BOTTOM
            if (state.getValue(PART) == PolePart.BOTTOM) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ElectricalPoleBlockEntity pole) {
                    pole.disconnectAll();
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide)
            return null;
        if (state.getValue(PART) != PolePart.BOTTOM)
            return null; // Only Bottom Ticks
        return createTickerHelper(type, CoreBlockEntities.ELECTRICAL_POLE.get(), ElectricalPoleBlockEntity::tick);
    }
}
