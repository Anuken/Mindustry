package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.LiquidDisplay;
import io.anuke.mindustry.world.meta.ContentStatValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class LiquidValue implements ContentStatValue{
    private final Liquid liquid;

    public LiquidValue(Liquid liquid){
        this.liquid = liquid;
    }

    @Override
    public UnlockableContent[] getValueContent(){
        return new UnlockableContent[]{liquid};
    }

    @Override
    public void display(Table table){
        table.add(new LiquidDisplay(liquid));
    }
}
