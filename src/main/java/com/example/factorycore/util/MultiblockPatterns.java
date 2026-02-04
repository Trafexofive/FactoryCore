package com.example.factorycore.util;

import com.example.factorycore.registry.CoreBlocks;
import net.minecraft.world.level.block.Blocks;

public class MultiblockPatterns {
    public static final MultiblockPattern ELECTRIC_FURNACE = createElectricFurnace();
    public static final MultiblockPattern AUTO_ASSEMBLER = createAutoAssembler();

    private static MultiblockPattern createElectricFurnace() {
        MultiblockPattern p = new MultiblockPattern(3, 1, 3);
        // Standard 3x3 footprint on floor
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Controller
                p.add(x, 0, z, CoreBlocks.ELECTRICAL_FLOOR.get());
            }
        }
        return p;
    }

    private static MultiblockPattern createAutoAssembler() {
        MultiblockPattern p = new MultiblockPattern(3, 1, 3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                p.add(x, 0, z, CoreBlocks.ELECTRICAL_FLOOR.get());
            }
        }
        return p;
    }
}
