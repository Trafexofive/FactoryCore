package com.example.factorycore.event;

import com.example.factorycore.FactoryCore;
import com.example.factorycore.block.ElectricalPoleBlock;
import com.example.factorycore.block.entity.ElectricalPoleBlockEntity;
import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.registry.CoreBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = FactoryCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CommonModEventSubscriber {

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

        // Capability for the POLE BE
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.ELECTRICAL_POLE.get(),
            (be, side) -> be.getEnergyStorage()
        );

        // Universal Pole Access: Register capability for the BLOCK so all 3 parts work.
        event.registerBlock(
            Capabilities.EnergyStorage.BLOCK,
            (level, pos, state, be, side) -> {
                net.minecraft.core.BlockPos base = ElectricalPoleBlock.getPoleBase(level, pos, state);
                if (base != null && level.getBlockEntity(base) instanceof ElectricalPoleBlockEntity pole) {
                    return pole.getEnergyStorage();
                }
                return null;
            },
            CoreBlocks.ELECTRICAL_POLE.get()
        );

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            CoreBlockEntities.PIPE.get(),
            (be, side) -> be.getInventory()
        );
    }
}