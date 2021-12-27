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

import static mindustry.Vars.*;

public class SuppressionFieldAbility extends Ability{
    protected static Rand rand = new Rand();
    protected static Seq<Building> builds = new Seq<>();

    public float reload = 60f * 1.5f;
    public float range = 200f;

    public float orbRadius = 4.5f, orbMidScl = 0.62f, orbSinScl = 8f, orbSinMag = 1f;
    public Color color1 = Pal.sap.cpy().mul(1.6f), color2 = Pal.sap;
    public float layer = Layer.effect;

    public int particles = 15;
    public float particleSize = 4f;
    public float particleLen = 7f;
    public float rotateScl = 3f;
    public float particleLife = 110f;
    public Interp particleInterp = f -> Interp.circleOut.apply(Interp.slope.apply(f));
    public Color particleColor = Pal.sap.cpy().a(0.8f);

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

                    any = true;

                    //add prev check so ability spam doesn't lead to particle spam (essentially, recently suppressed blocks don't get new particles)
                    if(!headless && prev - Time.time <= reload/2f){
                        builds.add(build);
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

        Draw.color(color2);
        Fill.circle(unit.x, unit.y, rad);

        Draw.color(color1);
        Fill.circle(unit.x, unit.y, rad * orbMidScl);

        float base = (Time.time / particleLife);
        rand.setSeed(unit.id);
        Draw.color(particleColor);
        for(int i = 0; i < particles; i++){
            float fin = (rand.random(1f) + base) % 1f, fout = 1f - fin;
            float angle = rand.random(360f) + (Time.time / rotateScl + unit.rotation) % 360f;
            float len = particleLen * particleInterp.apply(fout);
            Fill.circle(
            unit.x + Angles.trnsx(angle, len),
            unit.y + Angles.trnsy(angle, len),
            particleSize * Mathf.slope(fin)
            );
        }

        //TODO improve
        if(heat > 0.001f){
            Draw.color(Pal.sapBullet);
            Lines.stroke(1.2f * heat * Mathf.absin(10f, 1f));
            Lines.circle(unit.x, unit.y, range);
        }

        Draw.reset();
    }
}
