package mindustry.world.meta.values;

import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValue;

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
    }

    @Override
    public void display(Table table){
        int precision = Math.abs((int)value - value) <= 0.001f ? 0 : Math.abs((int)(value * 10) - value * 10) <= 0.001f ? 1 : 2;

        table.add(Strings.fixed(value, precision));
        table.add((unit.space ? " " : "") + unit.localized());
    }
}
