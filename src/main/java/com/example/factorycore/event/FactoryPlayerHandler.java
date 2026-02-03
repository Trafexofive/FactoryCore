package com.example.factorycore.event;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.registry.CoreBlocks;
import com.example.factorycore.registry.CoreItems;
import com.example.ghostlib.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FactoryPlayerHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String tag = "factorycore_received_v11"; // Increment tag
        
        if (!player.getPersistentData().contains(tag)) {
            player.getPersistentData().putBoolean(tag, true);
            
            // Give 64 of everything (FactoryCore only)
            player.getInventory().add(new ItemStack(CoreItems.MACHINE_CASING.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.ELECTRICAL_FLOOR.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.ELECTRICAL_POLE.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.CREATIVE_ENERGY_SOURCE.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.ELECTRIC_FURNACE_CONTROLLER.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.AUTO_ASSEMBLER_CONTROLLER.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.BATTERY.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.SOLAR_PANEL.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.PIPE.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.WRENCH.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.MULTIMETER.get(), 64));
            
            // Starter Blueprints
            player.getInventory().add(createBlueprint("Electric Furnace", createFurnacePattern()));
            player.getInventory().add(createBlueprint("Auto Assembler", createAssemblerPattern()));
            player.getInventory().add(createBlueprint("Battery Array", createBatteryPattern()));
            
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Core Industrial Foundation Initialized (V11)").withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
    }

    private static CompoundTag createBatteryPattern() {
        CompoundTag tag = new CompoundTag();
        ListTag patternList = new ListTag();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = CoreBlocks.MACHINE_CASING.get().defaultBlockState();
                    if (x == 0 && y == 0 && z == 0) state = CoreBlocks.BATTERY.get().defaultBlockState();
                    
                    // Hollow center
                    if (x == 0 && y == 1 && z == 0) {
                        CompoundTag bTag = new CompoundTag();
                        bTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                        bTag.put("State", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
                        patternList.add(bTag);
                        continue;
                    }

                    CompoundTag bTag = new CompoundTag();
                    bTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                    bTag.put("State", NbtUtils.writeBlockState(state));
                    patternList.add(bTag);
                }
            }
        }
        tag.put("Pattern", patternList);
        tag.putInt("SizeX", 3); tag.putInt("SizeY", 3); tag.putInt("SizeZ", 3);
        return tag;
    }

    private static CompoundTag createDronePortPattern() {
        CompoundTag tag = new CompoundTag();
        ListTag patternList = new ListTag();
        
        // 3x3x3 Structure
        // Controller is TOP-CENTER (0, 0, 0 in pattern relative to top)
        // We will define it such that (0,0,0) is bottom center for easier placement,
        // but the Controller block is actually at (0, 2, 0)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = CoreItems.MACHINE_CASING.get().getDefaultInstance().getItem() instanceof net.minecraft.world.item.BlockItem bi ? bi.getBlock().defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState();
                    
                    if (x == 0 && y == 2 && z == 0) {
                        state = com.example.ghostlib.registry.ModBlocks.DRONE_PORT.get().defaultBlockState();
                    }

                    CompoundTag blockTag = new CompoundTag();
                    // Offset pattern so 0,0,0 is bottom center
                    blockTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                    blockTag.put("State", NbtUtils.writeBlockState(state));
                    patternList.add(blockTag);
                }
            }
        }
        tag.put("Pattern", patternList);
        tag.putInt("SizeX", 3);
        tag.putInt("SizeY", 3);
        tag.putInt("SizeZ", 3);
        return tag;
    }

    private static ItemStack createBlueprint(String name, CompoundTag patternTag) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(patternTag));
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name).withStyle(net.minecraft.ChatFormatting.YELLOW));
        return stack;
    }

    private static CompoundTag createFurnacePattern() {
        CompoundTag tag = new CompoundTag();
        ListTag patternList = new ListTag();
        
        // 3x3x3 Hollow Cube
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) { // Fix: Start at 0
                for (int z = 0; z <= 2; z++) {
                    // Hollow check: Must be on boundary
                    boolean isEdge = (x == -1 || x == 1 || y == 0 || y == 2 || z == 0 || z == 2);
                    if (!isEdge) {
                        // Enforce center is AIR
                        CompoundTag blockTag = new CompoundTag();
                        blockTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                        blockTag.put("State", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
                        patternList.add(blockTag);
                        continue;
                    }

                    BlockState state = CoreBlocks.MACHINE_CASING.get().defaultBlockState();
                    
                    // Controller at front center (0, 0, 0)
                    if (x == 0 && y == 0 && z == 0) {
                        state = CoreBlocks.ELECTRIC_FURNACE_CONTROLLER.get().defaultBlockState();
                    }

                    CompoundTag blockTag = new CompoundTag();
                    blockTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                    blockTag.put("State", NbtUtils.writeBlockState(state));
                    patternList.add(blockTag);
                }
            }
        }
        tag.put("Pattern", patternList);
        tag.putInt("SizeX", 3);
        tag.putInt("SizeY", 3);
        tag.putInt("SizeZ", 3);
        return tag;
    }

    private static CompoundTag createAssemblerPattern() {
        CompoundTag tag = new CompoundTag();
        ListTag patternList = new ListTag();
        
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) { // Fix: Start at 0
                for (int z = 0; z <= 2; z++) {
                    boolean isEdge = (x == -1 || x == 1 || y == 0 || y == 2 || z == 0 || z == 2);
                    if (!isEdge) {
                        CompoundTag blockTag = new CompoundTag();
                        blockTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                        blockTag.put("State", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
                        patternList.add(blockTag);
                        continue;
                    }

                    BlockState state = CoreBlocks.MACHINE_CASING.get().defaultBlockState();
                    if (x == 0 && y == 0 && z == 0) {
                        state = CoreBlocks.AUTO_ASSEMBLER_CONTROLLER.get().defaultBlockState();
                    }

                    CompoundTag blockTag = new CompoundTag();
                    blockTag.put("Rel", NbtUtils.writeBlockPos(new BlockPos(x, y, z)));
                    blockTag.put("State", NbtUtils.writeBlockState(state));
                    patternList.add(blockTag);
                }
            }
        }
        tag.put("Pattern", patternList);
        tag.putInt("SizeX", 3);
        tag.putInt("SizeY", 3);
        tag.putInt("SizeZ", 3);
        return tag;
    }
}