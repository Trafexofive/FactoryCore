package com.example.factorycore.block;

import com.example.factorycore.block.entity.PipeBlockEntity;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class PipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    public static final BooleanProperty EXTRACT_NORTH = BooleanProperty.create("extract_north");
    public static final BooleanProperty EXTRACT_EAST = BooleanProperty.create("extract_east");
    public static final BooleanProperty EXTRACT_SOUTH = BooleanProperty.create("extract_south");
    public static final BooleanProperty EXTRACT_WEST = BooleanProperty.create("extract_west");
    public static final BooleanProperty EXTRACT_UP = BooleanProperty.create("extract_up");
    public static final BooleanProperty EXTRACT_DOWN = BooleanProperty.create("extract_down");

    private static final Map<Direction, BooleanProperty> EXTRACT_MAP = new EnumMap<>(Direction.class);
    static {
        EXTRACT_MAP.put(Direction.NORTH, EXTRACT_NORTH);
        EXTRACT_MAP.put(Direction.EAST, EXTRACT_EAST);
        EXTRACT_MAP.put(Direction.SOUTH, EXTRACT_SOUTH);
        EXTRACT_MAP.put(Direction.WEST, EXTRACT_WEST);
        EXTRACT_MAP.put(Direction.UP, EXTRACT_UP);
        EXTRACT_MAP.put(Direction.DOWN, EXTRACT_DOWN);
    }

    private static final Map<Direction, BooleanProperty> PROPERTY_MAP = new EnumMap<>(Direction.class);
    static {
        PROPERTY_MAP.put(Direction.NORTH, NORTH);
        PROPERTY_MAP.put(Direction.EAST, EAST);
        PROPERTY_MAP.put(Direction.SOUTH, SOUTH);
        PROPERTY_MAP.put(Direction.WEST, WEST);
        PROPERTY_MAP.put(Direction.UP, UP);
        PROPERTY_MAP.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    private final Map<BlockState, VoxelShape> shapeCache = new java.util.HashMap<>();

    public static final com.mojang.serialization.MapCodec<PipeBlock> CODEC = simpleCodec(p -> new PipeBlock());

    public PipeBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
                .setValue(EXTRACT_NORTH, false).setValue(EXTRACT_EAST, false).setValue(EXTRACT_SOUTH, false)
                .setValue(EXTRACT_WEST, false).setValue(EXTRACT_UP, false).setValue(EXTRACT_DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED,
                    EXTRACT_NORTH, EXTRACT_EAST, EXTRACT_SOUTH, EXTRACT_WEST, EXTRACT_UP, EXTRACT_DOWN);
    }
    
    public static BooleanProperty getExtractProperty(Direction dir) {
        return EXTRACT_MAP.get(dir);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(state, s -> {
            VoxelShape shape = CORE_SHAPE;
            if (s.getValue(NORTH)) shape = Shapes.or(shape, Block.box(5, 5, 0, 11, 11, 5));
            if (s.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(5, 5, 11, 11, 11, 16));
            if (s.getValue(EAST)) shape = Shapes.or(shape, Block.box(11, 5, 5, 16, 11, 11));
            if (s.getValue(WEST)) shape = Shapes.or(shape, Block.box(0, 5, 5, 5, 11, 11));
            if (s.getValue(UP)) shape = Shapes.or(shape, Block.box(5, 11, 5, 11, 16, 11));
            if (s.getValue(DOWN)) shape = Shapes.or(shape, Block.box(5, 0, 5, 11, 5, 11));
            return shape;
        });
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        
        BlockState state = this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_MAP.get(dir), canConnectTo(level, pos.relative(dir), dir.getOpposite()));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_MAP.get(direction), canConnectTo(level, neighborPos, direction.getOpposite()));
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PipeBlockEntity) return true;
        if (be == null) return false;
        
        if (level instanceof Level world) {
            return world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side) != null;
        }
        return false;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        // Tag check for Wrench
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> wrenchTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
        
        if (stack.is(wrenchTag)) {
            if (!level.isClientSide) {
                Direction side = hitResult.getDirection();
                BooleanProperty prop = getExtractProperty(side);
                boolean current = state.getValue(prop);
                level.setBlock(pos, state.setValue(prop, !current), 3);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Extraction " + (current ? "Disabled" : "Enabled") + " on side: " + side.getName())
                        .withStyle(current ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN), true);
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, CoreBlockEntities.PIPE.get(), PipeBlockEntity::tick);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
