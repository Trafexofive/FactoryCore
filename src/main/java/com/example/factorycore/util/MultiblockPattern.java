package com.example.factorycore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class MultiblockPattern {
    private final Map<BlockPos, Predicate<BlockState>> pattern = new HashMap<>();
    private int sizeX, sizeY, sizeZ;

    public MultiblockPattern(int x, int y, int z) {
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
    }

    public void add(int x, int y, int z, Block block) {
        pattern.put(new BlockPos(x, y, z), state -> state.is(block));
    }

    public void add(int x, int y, int z, Predicate<BlockState> predicate) {
        pattern.put(new BlockPos(x, y, z), predicate);
    }

    public boolean matches(Level level, BlockPos origin, net.minecraft.core.Direction facing) {
        for (Map.Entry<BlockPos, Predicate<BlockState>> entry : pattern.entrySet()) {
            BlockPos rel = entry.getKey();

            // Rotate relative position based on facing
            BlockPos rotatedRel = rotate(rel, facing);
            BlockPos target = origin.offset(rotatedRel);

            BlockState targetState = level.getBlockState(target);
            if (!entry.getValue().test(targetState)) {
                // Debug logging
                System.out.println("Structure check failed at relative " + rel + " (rotated " + rotatedRel + ")");
                System.out.println("Expected match, found: " + targetState);
                return false;
            }
        }
        return true;
    }

    private BlockPos rotate(BlockPos pos, net.minecraft.core.Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos; // NORTH is base
        };
    }

    public Map<BlockPos, Predicate<BlockState>> getPattern() {
        return pattern;
    }
}
