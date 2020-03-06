package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.*;

public class LaunchPad extends StorageBlock{
    public final int timerLaunch = timers++;
    /** Time inbetween launches. */
    public float launchTime;

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
    public boolean acceptItem(Tilec source, Item item){
        return item.type == ItemType.material && tile.items.total() < itemCapacity;
    }

    @Override
    public void draw(){
        super.draw();

        //TODO broken
        float progress = Mathf.clamp(Mathf.clamp((tile.items.total() / (float)itemCapacity)) * ((timer().getTime(timerLaunch) / (launchTime / tile.timeScale()))));
        float scale = size / 3f;

        Lines.stroke(2f);
        Draw.color(Pal.accentBack);
        Lines.poly(x, y, 4, scale * 10f * (1f - progress), 45 + 360f * progress);

        Draw.color(Pal.accent);

        if(tile.cons().valid()){
            for(int i = 0; i < 3; i++){
                float f = (Time.time() / 200f + i * 0.5f) % 1f;

                Lines.stroke(((2f * (2f - Math.abs(0.5f - f) * 2f)) - 2f + 0.2f));
                Lines.poly(x, y, 4, (1f - f) * 10f * scale);
            }
        }

        Draw.reset();
    }

    @Override
    public void updateTile(){
        Tilec entity = tile.entity;

        if(state.isCampaign() && consValid() && items.total() >= itemCapacity && timer(timerLaunch, launchTime / timeScale())){
            for(Item item : Vars.content.items()){
                Events.fire(Trigger.itemLaunch);
                Fx.padlaunch.at(tile);
                int used = Math.min(items.get(item), itemCapacity);
                data.addItem(item, used);
                items.remove(item, used);
                Events.fire(new LaunchItemEvent(item, used));
            }
        }
    }
}
