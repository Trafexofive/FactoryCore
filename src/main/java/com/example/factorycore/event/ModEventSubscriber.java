package com.example.factorycore.event;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.command.FactoryCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        
        net.minecraft.core.BlockPos pos = event.getPos();
        net.minecraft.world.level.block.entity.BlockEntity be = event.getLevel().getBlockEntity(pos);
        
        if (be instanceof com.example.factorycore.block.entity.AutoAssemblerBlockEntity assembler) {
            System.out.println("Forcing AutoAssembler GUI from Event");
            event.getEntity().openMenu(assembler, buf -> {
                buf.writeBlockPos(pos);
                com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.encode(buf, event.getLevel().getBlockState(pos));
            });
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        } else if (be instanceof com.example.factorycore.block.entity.ElectricFurnaceBlockEntity furnace) {
            System.out.println("Forcing ElectricFurnace GUI from Event");
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

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        com.example.factorycore.block.entity.ElectricalFloorBlockEntity.registerCapabilities(event);
        com.example.factorycore.block.entity.CreativeEnergySourceBlockEntity.registerCapabilities(event);
        
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            CoreBlockEntities.ELECTRIC_FURNACE.get(),
            (be, side) -> be.getItemHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            CoreBlockEntities.AUTO_ASSEMBLER.get(),
            (be, side) -> be.getItemHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            CoreBlockEntities.ELECTRIC_FURNACE.get(),
            (be, side) -> be.getFluidHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            CoreBlockEntities.AUTO_ASSEMBLER.get(),
            (be, side) -> be.getFluidHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.BATTERY.get(),
            (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.SOLAR_PANEL.get(),
            (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.ELECTRICAL_POLE.get(),
            (be, side) -> be.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            CoreBlockEntities.PIPE.get(),
            (be, side) -> be.getInventory()
        );
    }
}
