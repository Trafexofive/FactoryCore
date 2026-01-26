package com.example.factorycore.block.entity;

import com.example.factorycore.power.ElectricalNetwork;
import com.example.factorycore.power.FactoryNetworkManager;
import com.example.factorycore.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class ElectricalFloorBlockEntity extends BlockEntity {

    public ElectricalFloorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CoreBlockEntities.ELECTRICAL_FLOOR.get(), pos, blockState);
    }

    @Nullable
    public IEnergyStorage getEnergyStorage() {
        if (level == null || level.isClientSide) return null;
        FactoryNetworkManager manager = FactoryNetworkManager.get(level);
        if (manager == null) return null;
        
        ElectricalNetwork net = manager.getNetworkAt(worldPosition);
        return net != null ? net.getStorage() : null;
    }
    
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            CoreBlockEntities.ELECTRICAL_FLOOR.get(),
            (be, side) -> be.getEnergyStorage()
        );
    }
}
