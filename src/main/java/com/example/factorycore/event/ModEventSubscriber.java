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
            Capabilities.ItemHandler.BLOCK,
            CoreBlockEntities.PIPE.get(),
            (be, side) -> be.getInventory()
        );
    }
}
