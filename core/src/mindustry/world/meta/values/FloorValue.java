package mindustry.world.meta.values;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

public class FloorValue implements StatValue{
    private final Floor floor;

    public FloorValue(Floor floor){
        this.floor = floor;
    }

    @Override
    public void display(Table table){
        table.add(new Image(floor.icon(Cicon.small))).padRight(3);
        table.add(floor.localizedName).padRight(3);
    }
}
