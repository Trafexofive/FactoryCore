package com.example.factorycore.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FactoryUI {

    public static <T> IDataProvider<T> supplier(Supplier<T> supplier) {
        return new IDataProvider<T>() {
            @Override
            public ISubscription registerListener(Consumer<T> listener) {
                return new ISubscription() {
                    @Override
                    public void unsubscribe() {}
                };
            }

            @Override
            public T getValue() {
                return supplier.get();
            }
        };
    }

    public static void apply(LayoutStyle layout, float left, float top, float width, float height) {
        layout.left(left);
        layout.top(top);
        layout.width(width);
        layout.height(height);
    }
    
    public static void margin(LayoutStyle layout, float top, float bottom, float left, float right) {
        layout.marginTop(top);
        layout.marginBottom(bottom);
        layout.marginLeft(left);
        layout.marginRight(right);
    }
    
    public static void pos(LayoutStyle layout, float left, float top) {
        layout.left(left);
        layout.top(top);
    }
    
    public static void bottom(LayoutStyle layout, float bottom, float left) {
        layout.bottom(bottom);
        layout.left(left);
    }
}