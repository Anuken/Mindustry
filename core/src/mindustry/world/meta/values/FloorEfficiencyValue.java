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
    private final boolean startZero;

    public FloorEfficiencyValue(Floor floor, float multiplier, boolean startZero){
        this.floor = floor;
        this.multiplier = multiplier;
        this.startZero = startZero;
    }

    @Override
    public void display(Table table){
        table.stack(new Image(floor.icon(Cicon.medium)).setScaling(Scaling.fit), new Table(t -> {
            t.top().right().add((multiplier < 0 ? "[scarlet]" : startZero ? "[accent]" : "[accent]+") + (int)((multiplier) * 100) + "%").style(Styles.outlineLabel);
        }));
    }
}
