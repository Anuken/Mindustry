package mindustry.world.blocks.defense;

import arc.math.*;
import arc.util.*;
import arc.struct.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.audio.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;
import mindustry.world.*;
import mindustry.entities.*;
import mindustry.annotations.Annotations.Load;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.randLenVectors;
import static mindustry.Vars.tilesize;

public class ShockwaveTower extends Block {
    public float range = 80f;
    public float interval = 30f;
    public float bulletDamage = 150;
    public float minTargets = 1; //don't know if anyone needs this, adding it anyway
    public float falloffCount = 20f;
    public float shake = 3f;
    public Sound shootSound = Sounds.bang;
    public Color waveColor = Pal.redSpark, heatColor = Color.red;
    public float cooldownMultiplier = 1f;

    public @Load("@-heat") TextureRegion heatRegion; //debating whether it should be "glow" instead

    public Effect waveEffect = new Effect(20, e -> {
        color(waveColor);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, range * e.finpow());
        color(Pal.gray);
        randLenVectors(e.id + 1, 8, 1f + 23f * e.finpow(), (x, y) ->
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f
        ));
    });

    public ShockwaveTower(String name) {
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / interval, StatUnit.perSecond);
        stats.add(Stat.falloffCount, falloffCount);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    public class ShockwaveTowerBuild extends Building {
        public float refresh = Mathf.random(interval);
        public float reload = 0f;
        float heat = 0f;
        public Seq<Bullet> targets = new Seq<>();
        public float d;

        @Override
        public void updateTile() {
            if (potentialEfficiency > 0 && (refresh += Time.delta) >= interval) {
                targets.clear();
                refresh = 0f;
                targets = Groups.bullet.intersect(x - range, y - range, range * 2, range * 2).filter(b -> b.team != team && b.type().hittable);

                if (efficiency > 0 && targets.size >= minTargets) {
                    waveEffect.at(this);
                    shootSound.at(this);
                    Effect.shake(shake, shake, this);
                    reload = interval;
                    d = Math.min(bulletDamage, bulletDamage * falloffCount / targets.size);

                    for (var target : targets) {
                        if (target.type() != null && target.type().hittable) { //destroys lasers regardless of the filter sometimes
                            if (target.damage() > d) {
                                target.damage(target.damage() - d);
                            } else {
                                target.remove();
                            }
                        }
                    }
                }
            }
            heat = Mathf.clamp(reload -= (cooldownMultiplier * Time.delta), 0, interval) / interval;
        }
        @Override
        public boolean shouldConsume() {
            return targets.size >= minTargets;
        }

        @Override
        public void draw() {
             super.draw();
             Drawf.additive(heatRegion, heatColor, heat, x, y, 0f, Layer.blockAdditive);
        }
    }
 }