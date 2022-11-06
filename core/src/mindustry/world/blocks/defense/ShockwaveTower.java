package mindustry.world.blocks.defense;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ShockwaveTower extends Block{
    public int timerCheck = timers ++;

    public float range = 110f;
    public float reload = 60f * 1.5f;
    public float bulletDamage = 160;
    public float falloffCount = 20f;
    public float shake = 2f;
    //checking for bullets every frame is costly, so only do it at intervals even when ready.
    public float checkInterval = 8f;
    public Sound shootSound = Sounds.bang;
    public Color waveColor = Pal.accent, heatColor = Pal.turretHeat, shapeColor = Color.valueOf("f29c83");
    public float cooldownMultiplier = 1f;
    public Effect hitEffect = Fx.hitSquaresColor;
    public Effect waveEffect = Fx.pointShockwave;

    //TODO switch to drawers eventually or something
    public float shapeRotateSpeed = 1f, shapeRadius = 6f;
    public int shapeSides = 4;

    public @Load("@-heat") TextureRegion heatRegion;

    public ShockwaveTower(String name){
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.damage, bulletDamage, StatUnit.none);
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / reload, StatUnit.perSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, waveColor);
    }
    
    public class ShockwaveTowerBuild extends Building{
        public float reloadCounter = Mathf.random(reload);
        public float heat = 0f;
        public Seq<Bullet> targets = new Seq<>();

        @Override
        public void updateTile(){
            if(potentialEfficiency > 0 && (reloadCounter += Time.delta) >= reload && timer(timerCheck, checkInterval)){
                targets.clear();
                Groups.bullet.intersect(x - range, y - range, range * 2, range * 2, b -> {
                    if(b.team != team && b.type.hittable){
                        targets.add(b);
                    }
                });

                if(targets.size > 0){
                    heat = 1f;
                    reloadCounter = 0f;
                    waveEffect.at(x, y, range, waveColor);
                    shootSound.at(this);
                    Effect.shake(shake, shake, this);
                    float waveDamage = Math.min(bulletDamage, bulletDamage * falloffCount / targets.size);

                    for(var target : targets){
                        if(target.damage > waveDamage){
                            target.damage -= waveDamage;
                        }else{
                            target.remove();
                        }
                        hitEffect.at(target.x, target.y, waveColor);
                    }

                    if(team == state.rules.defaultTeam){
                        Events.fire(Trigger.shockwaveTowerUse);
                    }
                }
            }

            heat = Mathf.clamp(heat - Time.delta / reload * cooldownMultiplier);
        }

        @Override
        public float warmup(){
            return heat;
        }

        @Override
        public boolean shouldConsume(){
            return targets.size != 0;
        }

        @Override
        public void draw(){
            super.draw();
            Drawf.additive(heatRegion, heatColor, heat, x, y, 0f, Layer.blockAdditive);

            Draw.z(Layer.effect);
            Draw.color(shapeColor, waveColor, Mathf.pow(heat, 2f));
            Fill.poly(x, y, shapeSides, shapeRadius * potentialEfficiency, Time.time * shapeRotateSpeed);
            Draw.color();
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, waveColor);
        }
    }
}
