package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.world.meta.StatValue;

public class BooleanValue implements StatValue{
    private final boolean value;

    public BooleanValue(boolean value){
        this.value = value;
    }

    @Override
    public void display(Table table){
        table.add(!value ? "$no" : "$yes");
    }
}
