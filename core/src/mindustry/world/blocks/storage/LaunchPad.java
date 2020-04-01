package mindustry.world.blocks.storage;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LaunchPad extends StorageBlock{
    public final int timerLaunch = timers++;
    public final int timerSilo = timers++;
    /** Time inbetween launches. */
    public float launchTime;

    private float velocityInaccuracy = 0f;

    private Array<Item> missiles = new Array<>();
    private Array<BulletType> bullets = new Array<>();
    private Array<Tile> nearest = new Array<>();
    private Array<TargetTrait> hydra = new Array<>();

    public LaunchPad(String name){
        super(name);
        update = true;
        hasItems = true;
        solid = true;
    }

    @Override
    public void init(){
        super.init();

        ((ItemTurret)Blocks.ripple).ammo.each((item, bullet) -> bullets.add(bullet));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.launchTime, launchTime / 60f, StatUnit.seconds);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return (item.type == ItemType.material) && tile.entity.items.total() < itemCapacity;
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

            missiles.clear();
            entity.items.forEach((item, amount) -> {
                for(int i = 0; i < Mathf.floor(amount / 10); ++i) missiles.add(item);
            });
            missiles.shuffle();
            if(missiles.size == 0) return;

            nearest.clear();
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile near = world.ltile(x, y);
                    if(!nearest.contains(near) && near.block != Blocks.air && near.entity != null && near.getTeam().isEnemy(tile.getTeam())){
                        nearest.add(near);
                    }
                }
            }
            nearest.sort(t -> -tile.dst(t));

            hydra.clear();
            while(hydra.size < Mathf.clamp(missiles.size, 0, itemCapacity)){
                
                if(nearest.isEmpty()) break;
                TargetTrait target = nearest.pop();

                if(target == null) break;
                if(Mathf.chance(0.5f)) hydra.add(target);
            }

            hydra.shuffle();
            if(hydra.size == 0) return;

            for(TargetTrait target : hydra){
                bullets.shuffle();
                entity.items.remove(missiles.first(), 10);

                Vec2 predict = Predict.intercept(tile, target, bullets.first().speed);

                float dst = entity.dst(predict.x, predict.y);
                float maxTraveled = bullets.first().lifetime * bullets.first().speed;

                for(int i = 0; i < bullets.first().ammoMultiplier; ++i) Call.createBullet(bullets.first(), tile.getTeam(), tile.drawx(), tile.drawy(), tile.angleTo(target) + Mathf.range(bullets.first().inaccuracy + bullets.first().inaccuracy), 1f + Mathf.range(velocityInaccuracy), (dst / maxTraveled));
            }

            netServer.titanic.add(tile);
        }
    }
}
