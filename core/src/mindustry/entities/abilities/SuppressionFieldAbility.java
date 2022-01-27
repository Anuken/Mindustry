package mindustry.entities.abilities;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.MendProjector.*;
import mindustry.world.blocks.defense.RegenProjector.*;

import static mindustry.Vars.*;

public class SuppressionFieldAbility extends Ability{
    protected static Rand rand = new Rand();
    protected static Seq<Building> builds = new Seq<>();

    public float reload = 60f * 1.5f;
    public float range = 200f;

    public float orbRadius = 4.1f, orbMidScl = 0.33f, orbSinScl = 8f, orbSinMag = 1f;
    public Color color = Pal.sap.cpy().mul(1.6f);
    public float layer = Layer.effect;

    public float x = 0f, y = 0f;
    public int particles = 15;
    public float particleSize = 4f;
    public float particleLen = 7f;
    public float rotateScl = 3f;
    public float particleLife = 110f;
    public Interp particleInterp = f -> Interp.circleOut.apply(Interp.slope.apply(f));
    public Color particleColor = Pal.sap.cpy();

    public float applyParticleChance = 13f;

    protected boolean any;
    protected float timer;
    protected float heat = 0f;

    @Override
    public void update(Unit unit){
        if((timer += Time.delta) >= reload){
            any = false;
            builds.clear();
            Vars.indexer.eachBlock(null, unit.x,unit.y, range, build -> true, build -> {
                if(build.team != unit.team){
                    float prev = build.healSuppressionTime;
                    build.applyHealSuppression(reload + 1f);

                    //TODO maybe should be block field instead of instanceof check
                    if(build.wasRecentlyHealed(60f * 12f) || (build instanceof MendBuild || build instanceof RegenProjectorBuild)){
                        any = true;

                        //add prev check so ability spam doesn't lead to particle spam (essentially, recently suppressed blocks don't get new particles)
                        if(!headless && prev - Time.time <= reload/2f){
                            builds.add(build);
                        }
                    }
                }
            });

            //to prevent particle spam, the amount of particles is to remain constant (scales with number of buildings)
            float scaledChance = applyParticleChance / builds.size;
            for(var build : builds){
                if(Mathf.chance(scaledChance)){
                    Time.run(Mathf.random(reload), () -> {
                        Fx.regenSuppressSeek.at(build.x + Mathf.range(build.block.size * tilesize / 2f), build.y + Mathf.range(build.block.size * tilesize / 2f), 0f, unit);
                    });
                }
            }

            timer = 0f;
        }

        heat = Mathf.lerpDelta(heat,  any ? 1f : 0f, 0.09f);

    }

    @Override
    public void draw(Unit unit){
        Draw.z(layer);

        float rad = orbRadius + Mathf.absin(orbSinScl, orbSinMag);
        Tmp.v1.set(x, y).rotate(unit.rotation - 90f);
        float rx = unit.x + Tmp.v1.x, ry = unit.y + Tmp.v1.y;

        float base = (Time.time / particleLife);
        rand.setSeed(unit.id);
        Draw.color(particleColor);
        for(int i = 0; i < particles; i++){
            float fin = (rand.random(1f) + base) % 1f, fout = 1f - fin;
            float angle = rand.random(360f) + (Time.time / rotateScl + unit.rotation) % 360f;
            float len = particleLen * particleInterp.apply(fout);
            Fill.circle(
            rx + Angles.trnsx(angle, len),
            ry + Angles.trnsy(angle, len),
            particleSize * Mathf.slope(fin)
            );
        }

        Lines.stroke(2f);

        Draw.color(color);
        Lines.circle(rx, ry, rad);

        Draw.color(color);
        Fill.circle(rx, ry, rad * orbMidScl);

        //TODO improve
        if(heat > 0.001f){
            Draw.color(Pal.sapBullet);
            Lines.stroke(1.2f * heat * Mathf.absin(10f, 1f));
            Lines.circle(rx, ry, range);
        }

        Draw.reset();
    }
}
