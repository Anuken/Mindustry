package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Strings;

public class StringValue implements StatValue{
    private final String value;

    public StringValue(String value, Object... args){
        this.value = Strings.formatArgs(value, args);
    }

    @Override
    public void display(Table table){
        table.add(value);
    }
}
