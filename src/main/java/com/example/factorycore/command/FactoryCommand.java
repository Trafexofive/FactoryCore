package com.example.factorycore.command;

import com.example.factorycore.registry.CoreDimensions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public class FactoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("factory")
            .then(Commands.literal("visit")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel targetLevel = player.server.getLevel(CoreDimensions.ENGINEER_PLANE);
                    
                    if (targetLevel == null) {
                        context.getSource().sendFailure(Component.literal("Engineer Plane dimension not found!"));
                        return 0;
                    }

                    if (player.level().dimension() == CoreDimensions.ENGINEER_PLANE) {
                        context.getSource().sendSuccess(() -> Component.literal("You are already in the Engineer Plane."), false);
                        return 1;
                    }

                    // Save return point
                    CompoundTag tag = player.getPersistentData();
                    CompoundTag returnTag = new CompoundTag();
                    returnTag.putString("Dim", player.level().dimension().location().toString());
                    returnTag.putDouble("X", player.getX());
                    returnTag.putDouble("Y", player.getY());
                    returnTag.putDouble("Z", player.getZ());
                    returnTag.putFloat("YRot", player.getYRot());
                    returnTag.putFloat("XRot", player.getXRot());
                    returnTag.putInt("GameMode", player.gameMode.getGameModeForPlayer().getId());
                    tag.put("FactoryCore_Return", returnTag);

                    // Force Creative
                    player.setGameMode(GameType.CREATIVE);
                    
                    // Teleport safely to 0, 4, 0 (Floor is at 0, 4 is safe air)
                    player.teleportTo(targetLevel, 0.5, 4.0, 0.5, player.getYRot(), player.getXRot());
                    
                    context.getSource().sendSuccess(() -> Component.literal("Welcome to the Engineer Plane."), false);
                    return 1;
                })
            )
            .then(Commands.literal("leave")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (player.level().dimension() != CoreDimensions.ENGINEER_PLANE) {
                        context.getSource().sendFailure(Component.literal("You are not in the Engineer Plane."));
                        return 0;
                    }

                    CompoundTag tag = player.getPersistentData();
                    if (tag.contains("FactoryCore_Return")) {
                        CompoundTag returnTag = tag.getCompound("FactoryCore_Return");
                        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(returnTag.getString("Dim")));
                        ServerLevel targetLevel = player.server.getLevel(dimKey);
                        
                        if (targetLevel != null) {
                            int gmId = returnTag.getInt("GameMode");
                            player.setGameMode(GameType.byId(gmId));
                            
                            player.teleportTo(targetLevel, returnTag.getDouble("X"), returnTag.getDouble("Y"), returnTag.getDouble("Z"), returnTag.getFloat("YRot"), returnTag.getFloat("XRot"));
                            context.getSource().sendSuccess(() -> Component.literal("Returned to previous location."), false);
                            return 1;
                        }
                    }

                    // Fallback to Overworld Spawn
                    ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                    if (overworld == null) return 0;

                    BlockPos spawn = overworld.getSharedSpawnPos();
                    player.setGameMode(GameType.SURVIVAL);
                    player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                    
                    context.getSource().sendSuccess(() -> Component.literal("Returned to Overworld Spawn (No saved location found)."), false);
                    return 1;
                })
            )
        );
    }
}
