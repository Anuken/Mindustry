package io.anuke.mindustry.world.blocks.storage;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.data;
import static io.anuke.mindustry.Vars.world;

public class LaunchPad extends StorageBlock{
    protected final int timerLaunch = timers++;
    /** Time inbetween launches. */
    protected float launchTime;

    public LaunchPad(String name){
        super(name);
        update = true;
        hasItems = true;
        solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.launchTime, launchTime / 60f, StatUnit.seconds);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return item.type == ItemType.material && tile.entity.items.total() < itemCapacity;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        float progress = Mathf.clamp(Mathf.clamp((tile.entity.items.total() / (float)itemCapacity)) * ((tile.entity.timer.getTime(timerLaunch) / (launchTime / tile.entity.timeScale))));
        float scale = size / 3f;

        Lines.stroke(2f);
        Draw.color(Pal.accentBack);
        Lines.poly(tile.drawx(), tile.drawy(), 4, scale * 10f * (1f - progress), 45 + 360f * progress);

        Draw.color(Pal.accent);

        if(tile.entity.cons.valid()){
            for(int i = 0; i < 3; i++){
                float f = (Time.time() / 200f + i * 0.5f) % 1f;

                Lines.stroke(((2f * (2f - Math.abs(0.5f - f) * 2f)) - 2f + 0.2f));
                Lines.poly(tile.drawx(), tile.drawy(), 4, (1f - f) * 10f * scale);
            }
        }

        Draw.reset();
    }

    @Override
    public void update(Tile tile){
        TileEntity entity = tile.entity;

        if(world.isZone() && entity.cons.valid() && world.isZone() && entity.items.total() >= itemCapacity && entity.timer.get(timerLaunch, launchTime / entity.timeScale)){
            for(Item item : Vars.content.items()){
                Events.fire(Trigger.itemLaunch);
                Effects.effect(Fx.padlaunch, tile);
                int used = Math.min(entity.items.get(item), itemCapacity);
                data.addItem(item, used);
                entity.items.remove(item, used);
            }
        }
    }
}
