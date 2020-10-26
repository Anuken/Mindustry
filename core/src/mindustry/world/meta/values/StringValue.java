package mindustry.world.meta.values;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.world.meta.*;

public class StringValue implements StatValue{
    private final String value;

    public StringValue(String value, Object... args){
        this.value = Strings.format(value, args);
    }

    @Override
    public void display(Table table){
        table.add(value);
    }
}
