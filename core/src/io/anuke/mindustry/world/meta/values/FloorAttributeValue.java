package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.meta.StatValue;

public class FloorAttributeValue implements StatValue{
    private final Floor floor;

    public FloorAttributeValue(Floor floor){
        this.floor = floor;
    }

    @Override
    public void display(Table table){ // todo: delegate below to the block equivalent of ItemDisplay
        table.add(new Image(floor.icon(Cicon.medium))).padRight(3); // todo: nicely blend the edges somehow
        table.add(floor.localizedName());
    }
}
