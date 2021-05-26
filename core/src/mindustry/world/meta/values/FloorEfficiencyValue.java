package mindustry.world.meta.values;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

public class FloorEfficiencyValue implements StatValue{
    private final Floor floor;
    private final float multiplier;
    private final boolean startZero, isFuel;

    public FloorEfficiencyValue(Floor floor, float multiplier, boolean startZero, boolean isFuel){
        this.floor = floor;
        this.multiplier = multiplier;
        this.startZero = startZero;
        this.isFuel = isFuel;
    }

    public FloorEfficiencyValue(Floor floor, float multiplier, boolean startZero){
        this(floor, multiplier, startZero, false);
    }

    @Override
    public void display(Table table){
        table.stack(new Image(floor.icon(Cicon.medium)).setScaling(Scaling.fit), new Table(t -> {
            t.top().right().add((multiplier < 0 ? (isFuel ? "[accent]" : "[scarlet]") : startZero ? "[accent]" : (isFuel ? "[scarlet]+" : "[accent]+")) + (isFuel ? multiplier * 100f : (int)(multiplier * 100f)) + (isFuel ? "" : "%")).style(Styles.outlineLabel);
        }));
    }
}
