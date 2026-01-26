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
        String tag = "factorycore_received_v1";
        
        if (!player.getPersistentData().contains(tag)) {
            player.getPersistentData().putBoolean(tag, true);
            
            // 1. Give Materials
            player.getInventory().add(new ItemStack(CoreItems.MACHINE_CASING.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.ELECTRICAL_FLOOR.get(), 64));
            player.getInventory().add(new ItemStack(CoreItems.CREATIVE_ENERGY_SOURCE.get(), 1));
            player.getInventory().add(new ItemStack(CoreItems.ELECTRIC_FURNACE_CONTROLLER.get(), 1));
            player.getInventory().add(new ItemStack(CoreItems.AUTO_ASSEMBLER_CONTROLLER.get(), 1));
            
            // 2. Give Blueprints
            player.getInventory().add(createBlueprint("Electric Furnace", createFurnacePattern()));
            player.getInventory().add(createBlueprint("Auto Assembler", createAssemblerPattern()));
            
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("FactoryCore Blueprints Received").withStyle(net.minecraft.ChatFormatting.GREEN), false);
        }
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
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    // Hollow check: Must be on boundary
                    boolean isEdge = (x==0 || x==2 || y==0 || y==2 || z==0 || z==2);
                    if (!isEdge) continue;

                    BlockState state = CoreBlocks.MACHINE_CASING.get().defaultBlockState();
                    
                    // Controller at front center (1, 1, 0)
                    if (x == 1 && y == 1 && z == 0) {
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
        
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    boolean isEdge = (x==0 || x==2 || y==0 || y==2 || z==0 || z==2);
                    if (!isEdge) continue;

                    BlockState state = CoreBlocks.MACHINE_CASING.get().defaultBlockState();
                    if (x == 1 && y == 1 && z == 0) {
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
