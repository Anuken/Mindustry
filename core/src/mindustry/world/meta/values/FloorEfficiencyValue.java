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
    private final boolean startZero, hasPercent, round;

    public FloorEfficiencyValue(Floor floor, float multiplier, boolean startZero, boolean hasPercent, boolean round){
        this.floor = floor;
        this.multiplier = multiplier;
        this.startZero = startZero;
        this.hasPercent = hasPercent;
        this.round = round;
    }

    public FloorEfficiencyValue(Floor floor, float multiplier, boolean startZero){
        this(floor, multiplier, startZero, true, true);
    }

    @Override
    public void display(Table table){
        table.stack(new Image(floor.icon(Cicon.medium)).setScaling(Scaling.fit), new Table(t -> {
            t.top().right().add((multiplier < 0 ? "[scarlet]" : startZero ? "[accent]" : "[accent]+") + (round ? (int)(multiplier * 100f) : (multiplier * 100f)) + (hasPercent ? "%" : "")).style(Styles.outlineLabel);
        }));
    }
}
