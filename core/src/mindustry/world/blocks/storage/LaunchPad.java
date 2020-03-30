package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LaunchPad extends StorageBlock{
    public final int timerLaunch = timers++;
    public final int timerSilo = timers++;
    /** Time inbetween launches. */
    public float launchTime;

    private float velocityInaccuracy = 0f;

    private Array<TargetTrait> hydra = new Array<>();

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
        return (item.type == ItemType.material || (item == Items.pyratite && source.block == Blocks.incinerator)) && tile.entity.items.total() < itemCapacity;
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

        if(world.isZone() && entity.cons.valid() && entity.items.total() >= itemCapacity && entity.timer.get(timerLaunch, launchTime / entity.timeScale)){
            for(Item item : Vars.content.items()){
                Events.fire(Trigger.itemLaunch);
                Effects.effect(Fx.padlaunch, tile);
                int used = Math.min(entity.items.get(item), itemCapacity);
                data.addItem(item, used);
                entity.items.remove(item, used);
                Events.fire(new LaunchItemEvent(item, used));
            }
        }

        if(entity.timer.get(timerSilo, 60 * 2.5f) && entity.cons.valid()){

            hydra.clear();
            for(int i = 0; i < entity.items.get(Items.pyratite); ++i){
                hydra.add(Units.findEnemyTile(tile.getTeam(), tile.drawx(), tile.drawy(), Integer.MAX_VALUE, t -> !hydra.contains(t.entity), false));
            }

            hydra = hydra.select(tt -> tt != null);

            hydra.shuffle();

            if(hydra.size == 0) return;

            entity.items.remove(Items.pyratite, hydra.size);
            netServer.titanic.add(tile);

            float delay = 0;
            for(TargetTrait target : hydra){
                Vec2 predict = Predict.intercept(tile, target, Bullets.artilleryIncendiary.speed);

                float dst = entity.dst(predict.x, predict.y);
                float maxTraveled = Bullets.artilleryIncendiary.lifetime * Bullets.artilleryIncendiary.speed;

                for(int i = 0; i < (4 * 2); ++i) Timer.schedule(() -> Call.createBullet(Bullets.artilleryIncendiary, tile.getTeam(), tile.drawx(), tile.drawy(), tile.angleTo(target) + Mathf.range(Bullets.artilleryIncendiary.inaccuracy + Bullets.artilleryIncendiary.inaccuracy), 1f + Mathf.range(velocityInaccuracy), (dst / maxTraveled)), delay += (1f/60f));
            }
        }
    }
}
