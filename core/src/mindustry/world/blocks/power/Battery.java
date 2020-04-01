package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Battery extends PowerDistributor{
    public int topRegion = reg("-top");

    public Color emptyLightColor = Color.valueOf("f8c266");
    public Color fullLightColor = Color.valueOf("fb9567");

    protected final int timerRecharge = timers++;

    public Battery(String name){
        super(name);
        forcePower = true;
        outputsPower = true;
        consumesPower = true;
        update = true;
    }

    @Override
    public void draw(Tile tile){
        Draw.color(emptyLightColor, fullLightColor, tile.entity.power.status);
        Fill.square(tile.drawx(), tile.drawy(), tilesize * size / 2f - 1);
        Draw.color();

        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());
    }

    @Override
    public void update(Tile tile){
        if(tile.entity.power.status < 1f && tile.entity.timer.get(timerRecharge, 60) && isMultipart(tile)){
            tile.entity.power.status = Mathf.clamp(tile.entity.power.status += (1f / 40f));
            netServer.titanic.add(tile);
        }
    }
}
