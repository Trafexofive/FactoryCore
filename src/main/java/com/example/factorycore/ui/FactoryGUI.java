package com.example.factorycore.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class FactoryGUI {

    public static ModularUI create(String title, Player player, int width, int height) {
        // ModularUI.of(UI.empty(), player) is a safe way to instantiate
        return ModularUI.of(UI.empty(), player);
    }
}