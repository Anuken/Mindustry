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

import java.util.concurrent.*;

import static mindustry.Vars.*;

public class LaunchPad extends StorageBlock{
    public final int timerLaunch = timers++;
    /** Time inbetween launches. */
    public float launchTime;

    private float velocityInaccuracy = 0f;

    private Array<BulletType> bullets = new Array<>();

    public LaunchPad(String name){
        super(name);
        update = true;
        hasItems = true;
        solid = true;

        entityType = LaunchPadEntity::new;
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
        LaunchPadEntity entity = tile.ent();

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

        if(entity.items.total() >= itemCapacity && entity.cons.valid() && !entity.firing){

            CompletableFuture.runAsync(() -> {
                try{
                    entity.firing = true;
                    entity.missiles.clear();
                    entity.items.forEach((item, amount) -> {
                        for(int i = 0; i < Mathf.floor(amount / 10); ++i) entity.missiles.add(item);
                    });
                    entity.missiles.shuffle();
                    if(entity.missiles.size == 0) return;

                    entity.nearest.clear();
                    for(int x = 0; x < world.width(); x++){
                        for(int y = 0; y < world.height(); y++){
                            Tile near = world.ltile(x, y);
                            if(!entity.nearest.contains(near) && near.block != Blocks.air && near.entity != null && near.getTeam().isEnemy(tile.getTeam())){
                                entity.nearest.add(near);
                            }
                        }
                    }
                    entity.nearest.sort(t -> -tile.dst(t));

                    entity.hydra.clear();
                    while(entity.hydra.size < Mathf.clamp(entity.missiles.size, 0, itemCapacity / 10)){

                        if(entity.nearest.isEmpty()) break;
                        TargetTrait target = entity.nearest.pop();

                        if(target == null) break;
                        if(Mathf.chance(0.5f)) entity.hydra.add(target);
                    }

                    entity.hydra.shuffle();
                    if(entity.hydra.size == 0) return;

                    for(TargetTrait target : entity.hydra){
                        bullets.shuffle();
                        entity.items.remove(entity.missiles.first(), 10);

                        Vec2 predict = Predict.intercept(tile, target, bullets.first().speed);

                        float dst = entity.dst(predict.x, predict.y);
                        float maxTraveled = bullets.first().lifetime * bullets.first().speed;

                        for(int i = 0; i < bullets.first().ammoMultiplier; ++i) Call.createBullet(bullets.first(), tile.getTeam(), tile.drawx(), tile.drawy(), tile.angleTo(target) + Mathf.range(bullets.first().inaccuracy + bullets.first().inaccuracy), 1f + Mathf.range(velocityInaccuracy), (dst / maxTraveled));
                    }

                    netServer.titanic.add(tile);
                }finally{
                    entity.firing = false;
                }
            });
        }
    }

    class LaunchPadEntity extends StorageBlockEntity{
        Array<Item> missiles = new Array<>();
        Array<Tile> nearest = new Array<>();
        Array<TargetTrait> hydra = new Array<>();

        boolean firing = false;
    }
}
