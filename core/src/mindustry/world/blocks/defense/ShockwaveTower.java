package mindustry.world.blocks.defense;

import arc.math.*;
import arc.util.*;
import arc.struct.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.audio.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;
import mindustry.world.*;
import mindustry.entities.*;
import mindustry.annotations.Annotations.Load;
import static mindustry.Vars.*;

public class ShockwaveTower extends Block {
    public float range = 80f;
    public float interval = 30f;
    public float bulletDamage = 150;
    public float falloffCount = 20f;
    public float shake = 3f;
    public Sound shootSound = Sounds.bang;
    public Color waveColor = Pal.redSpark, heatColor = Color.red;
    public float cooldownMultiplier = 1f;
    public Effect waveEffect = Fx.pointShockwave;

    public @Load("@-heat") TextureRegion heatRegion; //debating whether it should be "glow" instead

    public ShockwaveTower(String name){
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / interval, StatUnit.perSecond);
        stats.add(Stat.falloffCount, falloffCount);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, waveColor);
    }

    public class ShockwaveTowerBuild extends Building{
        public float refresh = Mathf.random(interval);
        public float reload = 0f;
        public float heat = 0f;
        public Seq<Bullet> targets = new Seq<>();
        public float waveDamage;

        @Override
        public void updateTile(){
            if(potentialEfficiency > 0 && (refresh += Time.delta) >= interval){
                targets.clear();
                refresh = 0f;
                targets = Groups.bullet.intersect(x - range, y - range, range * 2, range * 2).filter(b -> b.team != team && b.type().hittable);
                Log.info(targets.size +" "+ efficiency);

                if(efficiency > 0 && targets.size != 0){
                    waveEffect.at(this);
                    shootSound.at(this);
                    Effect.shake(shake, shake, this);
                    reload = interval;
                    waveDamage = Math.min(bulletDamage, bulletDamage * falloffCount / targets.size);
                    Log.info("e");

                    for(var target : targets) {
                        if(target.type() != null && target.type().hittable){ //destroys lasers regardless of the filter sometimes
                            if(target.damage() > waveDamage) {
                                target.damage(target.damage() - waveDamage);
                            }else{
                                target.remove();
                            }
                        }
                    }
                }
            }
            heat = Mathf.clamp(reload -= cooldownMultiplier * Time.delta, 0, interval) / interval;
        }
        @Override
        public boolean shouldConsume(){
            return targets.size != 0;
        }

        @Override
        public void draw(){
            super.draw();
            Drawf.additive(heatRegion, heatColor, heat, x, y, 0f, Layer.blockAdditive);
        }
    }
}
