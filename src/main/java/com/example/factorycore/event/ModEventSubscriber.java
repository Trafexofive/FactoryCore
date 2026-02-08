package com.example.factorycore.event;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.command.FactoryCommand;
import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEventSubscriber {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        triggerNearbyPoleRefresh(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        triggerNearbyPoleRefresh(event.getLevel(), event.getPos());
    }

    private static void triggerNearbyPoleRefresh(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        net.minecraft.core.BlockPos.MutableBlockPos mpos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    mpos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(mpos);
                    if (be instanceof ElectricalPoleBlockEntity pole) {
                        pole.refresh();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        
        net.minecraft.core.BlockPos pos = event.getPos();
        net.minecraft.world.level.block.entity.BlockEntity be = event.getLevel().getBlockEntity(pos);
        
        if (be instanceof com.example.factorycore.block.entity.AutoAssemblerBlockEntity assembler) {
            event.getEntity().openMenu(assembler, buf -> {
                buf.writeBlockPos(pos);
                com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.encode(buf, event.getLevel().getBlockState(pos));
            });
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        } else if (be instanceof com.example.factorycore.block.entity.ElectricFurnaceBlockEntity furnace) {
            event.getEntity().openMenu(furnace, buf -> {
                buf.writeBlockPos(pos);
                com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.encode(buf, event.getLevel().getBlockState(pos));
            });
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FactoryCommand.register(event.getDispatcher());
    }
}
