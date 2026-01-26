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

    public boolean matches(Level level, BlockPos origin) {
        for (Map.Entry<BlockPos, Predicate<BlockState>> entry : pattern.entrySet()) {
            BlockPos target = origin.offset(entry.getKey());
            if (!entry.getValue().test(level.getBlockState(target))) {
                return false;
            }
        }
        return true;
    }
    
    public Map<BlockPos, Predicate<BlockState>> getPattern() {
        return pattern;
    }
}
