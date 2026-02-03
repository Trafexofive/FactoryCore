package com.example.factorycore.block.entity;

import com.example.factorycore.registry.CoreBlockEntities;
import com.example.factorycore.util.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BatteryBlockEntity extends AbstractFactoryMultiblockBlockEntity {

    private final net.neoforged.neoforge.energy.EnergyStorage energy = new net.neoforged.neoforge.energy.EnergyStorage(5000000, 10000, 10000);



    private static final MultiblockPattern PATTERN = createPattern();



    private static MultiblockPattern createPattern() {

        MultiblockPattern p = new MultiblockPattern(3, 3, 3);

        for (int x = -1; x <= 1; x++) {

            for (int y = 0; y <= 2; y++) {

                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {

                        p.add(x, y, z, com.example.factorycore.registry.CoreBlocks.BATTERY.get());

                    } else if (x == 0 && y == 1 && z == 0) {

                        p.add(x, y, z, (state) -> state.isAir());

                    } else {

                        p.add(x, y, z, com.example.factorycore.registry.CoreBlocks.MACHINE_CASING.get());

                    }

                }

            }

        }

        return p;

    }



    public BatteryBlockEntity(BlockPos pos, BlockState state) {

        super(CoreBlockEntities.BATTERY.get(), pos, state);

    }



    @Override

    public MultiblockPattern getPattern() {

        return PATTERN;

    }



    @Override

    protected int getInventorySize() {

        return 0; // Purely energy storage

    }



            @Override



            protected void serverTick() {



                IEnergyStorage network = getFloorEnergy();



                if (network != null) {



                    // STANDALONE / SMALL BUFFER: 1000 FE/t



                    // LARGE BUFFER: 50000 FE/t (Multiblock)



                    int throughput = isFormed ? 50000 : 1000;



        



                    // Always try to balance: provide to empty network, pull from full network



                    int myEnergy = energy.getEnergyStored();



                    int netEnergy = network.getEnergyStored();



                    int netMax = network.getMaxEnergyStored();



        



                    // Provide power if we have it and network isn't full



                    if (myEnergy > 0 && netEnergy < netMax) {



                        int toPush = energy.extractEnergy(throughput, true);



                        int accepted = network.receiveEnergy(toPush, false);



                        energy.extractEnergy(accepted, false);



                    }



        



                    // Pull power if we have space and network isn't empty



                    if (myEnergy < energy.getMaxEnergyStored() && netEnergy > 0) {



                        int toPull = network.extractEnergy(throughput, true);



                        int received = energy.receiveEnergy(toPull, false);



                        network.extractEnergy(received, false);



                    }



                }



            }



        



    



    @Override

    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);

        tag.putInt("BatteryEnergy", energy.getEnergyStored());

    }



    @Override

    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);

        energy.receiveEnergy(tag.getInt("BatteryEnergy"), false);

    }

    

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() { return energy; }

}


