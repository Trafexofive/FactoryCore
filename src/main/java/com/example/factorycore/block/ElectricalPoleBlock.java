package com.example.factorycore.block;

import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.example.factorycore.power.FactoryNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
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
        PolePart(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<PolePart> PART = EnumProperty.create("part", PolePart.class);
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);
    public static final com.mojang.serialization.MapCodec<ElectricalPoleBlock> CODEC = simpleCodec(ElectricalPoleBlock::new);

    public ElectricalPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, PolePart.BOTTOM));
    }

    public ElectricalPoleBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).noOcclusion());
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return (state.getValue(PART) == PolePart.BOTTOM) ? new ElectricalPoleBlockEntity(pos, state) : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /**
     * Helper to find the base (BOTTOM) position of a pole structure.
     */
    public static BlockPos getPoleBase(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ElectricalPoleBlock)) return null;
        PolePart part = state.getValue(PART);
        if (part == PolePart.BOTTOM) return pos;
        if (part == PolePart.MIDDLE) return pos.below();
        if (part == PolePart.TOP) return pos.below(2);
        return null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && state.getValue(PART) == PolePart.BOTTOM && !state.is(oldState.getBlock())) {
            FactoryNetworkManager.get(level).addNode(pos);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        PolePart part = state.getValue(PART);
        if (direction == Direction.DOWN && part != PolePart.BOTTOM && neighborState.isAir()) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == Direction.UP && part != PolePart.TOP && neighborState.isAir()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        if (state.getValue(PART) == PolePart.BOTTOM) {
            BlockPos middle = pos.above();
            BlockPos top = pos.above(2);
            if (level.getBlockState(middle).isAir() || level.getBlockState(middle).canBeReplaced())
                level.setBlock(middle, state.setValue(PART, PolePart.MIDDLE), 3);
            if (level.getBlockState(top).isAir() || level.getBlockState(top).canBeReplaced())
                level.setBlock(top, state.setValue(PART, PolePart.TOP), 3);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && state.getValue(PART) == PolePart.BOTTOM) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricalPoleBlockEntity pole) {
                pole.refresh();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && state.getValue(PART) == PolePart.BOTTOM) {
            FactoryNetworkManager manager = FactoryNetworkManager.get(level);
            if (manager != null) manager.removeNode(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricalPoleBlockEntity pole) pole.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != PolePart.BOTTOM) return null;
        return (lvl, p, s, be) -> { if (be instanceof ElectricalPoleBlockEntity pole) ElectricalPoleBlockEntity.tick(lvl, p, s, pole); };
    }
}
