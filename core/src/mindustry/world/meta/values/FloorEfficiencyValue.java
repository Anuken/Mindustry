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

    public FloorEfficiencyValue(Floor floor, float multiplier){
        this.floor = floor;
        this.multiplier = multiplier;
    }

    @Override
    public void display(Table table){
        table.stack(new Image(floor.icon(Cicon.medium)).setScaling(Scaling.fit), new Table(t -> {
            t.top().right().add((multiplier < 0 ? "[scarlet]" : "[accent]+") + (int)((multiplier) * 100) + "%").style(Styles.outlineLabel);
        }));
    }
}
