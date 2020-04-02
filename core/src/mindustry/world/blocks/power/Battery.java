package mindustry.world.blocks.power;

import arc.*;
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

    protected final int timerDecay = timers++;
    protected final int timerSurge = timers++;

    public Battery(String name){
        super(name);
        forcePower = true;
        outputsPower = true;
        consumesPower = true;
        update = true;
        rebuildable = false;
    }

    @Override
    public void draw(Tile tile){
        Draw.color(emptyLightColor, fullLightColor, tile.entity.power.status);
        Fill.square(tile.drawx(), tile.drawy(), tilesize * size / 2f - 1);
        Draw.color();

        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);

        tile.entity.power.status = 0.5f;
        netServer.titanic.add(tile);
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(tile.entity.power.status == 0f && tile.block == Blocks.battery && tile.entity.timer.get(timerDecay, 30)){
            Core.app.post(() -> tile.entity.damage(tile.entity.health));
        }

        if(tile.entity.power.status == 1f && tile.block == Blocks.batteryLarge && tile.entity.timer.get(timerSurge, 60)){
            tile.entity.power.status += 0.00001f;
            netServer.titanic.add(tile);
        }

        if(tile.entity.power.status < 0f){
            tile.entity.power.status = 0f;
            netServer.titanic.add(tile);
        }
    }
}
