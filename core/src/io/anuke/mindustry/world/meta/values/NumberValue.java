package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Strings;

/**
 * A stat that is a number with a unit attacked.
 * The number is rounded to 2 decimal places by default.
 */
public class NumberValue implements StatValue{
    private final StatUnit unit;
    private final float value;

    public NumberValue(float value, StatUnit unit){
        this.unit = unit;
        this.value = value;

        if(unit != StatUnit.none && unit.localized().contains("???")){
            throw new RuntimeException("No bundle definition found for unit: '" + unit + "'");
        }
    }

    @Override
    public void display(Table table){
        float diff = Math.abs((int) value - value);
        int precision = diff <= 0.01f ? 0 : diff <= 0.1f ? 1 : 2;

        table.add(Strings.toFixed(value, precision));
        table.add(" " + unit.localized());
    }
}
