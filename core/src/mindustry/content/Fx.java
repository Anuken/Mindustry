package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitAssembler.*;

import static arc.graphics.g2d.Draw.rect;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class Fx{
    public static final Rand rand = new Rand();
    public static final Vec2 v = new Vec2();

    public static final Effect

    none = new Effect(0, 0f, e -> {}),
    
    blockCrash = new Effect(100f, e -> {
        if(!(e.data instanceof Block block)) return;

        alpha(e.fin() + 0.5f);
        float offset = Mathf.lerp(0f, 200f, e.fout());
        color(0f, 0f, 0f, 0.44f);
        rect(block.fullIcon, e.x - offset * 4f, e.y, (float)block.size * 8f, (float)block.size * 8f);
        color(Color.white);
        rect(block.fullIcon, e.x + offset, e.y + offset * 5f, (float)block.size * 8f, (float)block.size * 8f);
    }),

    trailFade = new Effect(400f, e -> {
        if(!(e.data instanceof Trail trail)) return;
        //lifetime is how many frames it takes to fade out the trail
        e.lifetime = trail.length * 1.4f;

        if(!state.isPaused()){
            trail.shorten();
        }
        trail.drawCap(e.color, e.rotation);
        trail.draw(e.color, e.rotation);
    }),

    unitSpawn = new Effect(30f, e -> {
        if(!(e.data instanceof UnitType unit)) return;

        float scl = 1f + e.fout() * 2f;

        TextureRegion region = unit.fullIcon;

        alpha(e.fout());
        mixcol(Color.white, e.fin());

        rect(region, e.x, e.y, 180f);

        reset();

        alpha(e.fin());

        rect(region, e.x, e.y, region.width * Draw.scl * scl, region.height * Draw.scl * scl, e.rotation - 90);
    }),

    unitCapKill = new Effect(80f, e -> {
        color(Color.scarlet);
        alpha(e.fout(Interp.pow4Out));

        float size = 10f + e.fout(Interp.pow10In) * 25f;
        Draw.rect(Icon.warning.getRegion(), e.x, e.y, size, size);
    }),

    unitEnvKill = new Effect(80f, e -> {
        color(Color.scarlet);
        alpha(e.fout(Interp.pow4Out));

        float size = 10f + e.fout(Interp.pow10In) * 25f;
        Draw.rect(Icon.cancel.getRegion(), e.x, e.y, size, size);
    }),

    unitControl = new Effect(30f, e -> {
        if(!(e.data instanceof Unit select)) return;

        boolean block = select instanceof BlockUnitc;

        mixcol(Pal.accent, 1f);
        alpha(e.fout());
        rect(block ? ((BlockUnitc)select).tile().block.fullIcon : select.type.fullIcon, select.x, select.y, block ? 0f : select.rotation - 90f);
        alpha(1f);
        Lines.stroke(e.fslope());
        Lines.square(select.x, select.y, e.fout() * select.hitSize * 2f, 45);
        Lines.stroke(e.fslope() * 2f);
        Lines.square(select.x, select.y, e.fout() * select.hitSize * 3f, 45f);
        reset();
    }),

    unitDespawn = new Effect(100f, e -> {
        if(!(e.data instanceof Unit select) || select.type == null) return;

        float scl = e.fout(Interp.pow2Out);
        float p = Draw.scl;
        Draw.scl *= scl;

        mixcol(Pal.accent, 1f);
        rect(select.type.fullIcon, select.x, select.y, select.rotation - 90f);
        reset();

        Draw.scl = p;
    }),

    unitSpirit = new Effect(17f, e -> {
        if(!(e.data instanceof Position to)) return;

        color(Pal.accent);

        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow2In);
        float x = Tmp.v1.x, y = Tmp.v1.y;
        float size = 2.5f * e.fin();

        Fill.square(x, y, 1.5f * size, 45f);

        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow5In);
        x = Tmp.v1.x;
        y = Tmp.v1.y;

        Fill.square(x, y, size, 45f);
    }),

    itemTransfer = new Effect(12f, e -> {
        if(!(e.data instanceof Position to)) return;
        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow3)
        .add(Tmp.v2.sub(e.x, e.y).nor().rotate90(1).scl(Mathf.randomSeedRange(e.id, 1f) * e.fslope() * 10f));
        float x = Tmp.v1.x, y = Tmp.v1.y;
        float size = 1f;

        color(Pal.accent);
        Fill.circle(x, y, e.fslope() * 3f * size);

        color(e.color);
        Fill.circle(x, y, e.fslope() * 1.5f * size);
    }),

    pointBeam = new Effect(25f, 300f, e -> {
        if(!(e.data instanceof Position pos)) return;

        Draw.color(e.color, e.fout());
        Lines.stroke(1.5f);
        Lines.line(e.x, e.y, pos.getX(), pos.getY());
        Drawf.light(e.x, e.y, pos.getX(), pos.getY(), 20f, e.color, 0.6f * e.fout());
    }),

    pointHit = new Effect(8f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() + 0.2f);
        Lines.circle(e.x, e.y, e.fin() * 6f);
    }),

    lightning = new Effect(10f, 500f, e -> {
        if(!(e.data instanceof Seq)) return;
        Seq<Vec2> lines = e.data();

        stroke(3f * e.fout());
        color(e.color, Color.white, e.fin());

        for(int i = 0; i < lines.size - 1; i++){
            Vec2 cur = lines.get(i);
            Vec2 next = lines.get(i + 1);

            Lines.line(cur.x, cur.y, next.x, next.y, false);
        }

        for(Vec2 p : lines){
            Fill.circle(p.x, p.y, Lines.getStroke() / 2f);
        }
    }),

    coreBuildShockwave = new Effect(120, 500f, e -> {
        e.lifetime = e.rotation;

        color(Pal.command);
        stroke(e.fout(Interp.pow5Out) * 4f);
        Lines.circle(e.x, e.y, e.fin() * e.rotation * 2f);
    }),

    coreBuildBlock = new Effect(80f, e -> {
        if(!(e.data instanceof Block block)) return;

        mixcol(Pal.accent, 1f);
        alpha(e.fout());
        rect(block.fullIcon, e.x, e.y);
    }).layer(Layer.turret - 5f),

    moveCommand = new Effect(20, e -> {
        color(Pal.command);
        stroke(e.fout() * 5f);
        Lines.circle(e.x, e.y, 6f + e.fin() * 2f);
    }).layer(Layer.overlayUI),

    attackCommand = new Effect(20, e -> {
        color(Pal.remove);
        stroke(e.fout() * 5f);
        poly(e.x, e.y, 4, 7f + e.fin() * 2f);
    }).layer(Layer.overlayUI),

    commandSend = new Effect(28, e -> {
        color(Pal.command);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * e.rotation);
    }),

    upgradeCore = new Effect(120f, e -> {
        if(!(e.data instanceof Block block)) return;

        mixcol(Tmp.c1.set(Color.white).lerp(Pal.accent, e.fin()), 1f);
        alpha(e.fout());
        rect(block.fullIcon, e.x, e.y);
    }).layer(Layer.turret - 5f),

    upgradeCoreBloom = new Effect(80f, e -> {
        color(Pal.accent);
        stroke(4f * e.fout());
        Lines.square(e.x, e.y, tilesize / 2f * e.rotation + 2f);
    }),

    placeBlock = new Effect(16, e -> {
        color(Pal.accent);
        stroke(3f - e.fin() * 2f);
        Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);
    }),

    coreLaunchConstruct = new Effect(35, e -> {
        color(Pal.accent);
        stroke(4f - e.fin() * 3f);
        Lines.square(e.x, e.y, tilesize / 2f * e.rotation * 1.2f + e.fin() * 5f);

        randLenVectors(e.id, 5 + (int)(e.rotation * 5), e.rotation * 3f + (tilesize * e.rotation) * e.finpow() * 1.5f, (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * (4f + e.rotation));
        });
    }),

    tapBlock = new Effect(12, e -> {
        color(Pal.accent);
        stroke(3f - e.fin() * 2f);
        Lines.circle(e.x, e.y, 4f + (tilesize / 1.5f * e.rotation) * e.fin());
    }),

    breakBlock = new Effect(12, e -> {
        color(Pal.remove);
        stroke(3f - e.fin() * 2f);
        Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);

        randLenVectors(e.id, 3 + (int)(e.rotation * 3), e.rotation * 2f + (tilesize * e.rotation) * e.finpow(), (x, y) -> {
            Fill.square(e.x + x, e.y + y, 1f + e.fout() * (3f + e.rotation));
        });
    }),

    payloadDeposit = new Effect(30f, e -> {
        if(!(e.data instanceof YeetData data)) return;
        Tmp.v1.set(e.x, e.y).lerp(data.target, e.finpow());
        float x = Tmp.v1.x, y = Tmp.v1.y;

        scl(e.fout(Interp.pow3Out) * 1.05f);
        if(data.item instanceof Block block){
            Drawf.squareShadow(x, y, block.size * tilesize * 1.85f, 1f);
        }else if(data.item instanceof UnitType unit){
            unit.drawSoftShadow(e.x, e.y, e.rotation, 1f);
        }

        mixcol(Pal.accent, e.fin());
        rect(data.item.fullIcon, x, y, data.item instanceof Block ? 0f : e.rotation - 90f);
    }).layer(Layer.flyingUnitLow - 5f),

    select = new Effect(23, e -> {
        color(Pal.accent);
        stroke(e.fout() * 3f);
        Lines.circle(e.x, e.y, 3f + e.fin() * 14f);
    }),

    smoke = new Effect(100, e -> {
        color(Color.gray, Pal.darkishGray, e.fin());
        Fill.circle(e.x, e.y, (7f - e.fin() * 7f)/2f);
    }),

    fallSmoke = new Effect(110, e -> {
        color(Color.gray, Color.darkGray, e.rotation);
        Fill.circle(e.x, e.y, e.fout() * 3.5f);
    }),

    unitWreck = new Effect(200f, e -> {
        if(!(e.data instanceof TextureRegion reg)) return;

        Draw.mixcol(Pal.rubble, 1f);

        float vel = e.fin(Interp.pow5Out) * 2f * Mathf.randomSeed(e.id, 1f);
        float totalRot = Mathf.randomSeed(e.id + 1, 10f);
        Tmp.v1.trns(Mathf.randomSeed(e.id + 2, 360f), vel);

        Draw.z(Mathf.lerp(Layer.flyingUnitLow, Layer.debris, e.fin()));
        Draw.alpha(e.fout(Interp.pow5Out));

        Draw.rect(reg, e.x + Tmp.v1.x, e.y + Tmp.v1.y, e.rotation - 90 + totalRot * e.fin(Interp.pow5Out));
    }),

    rocketSmoke = new Effect(120, e -> {
        color(Color.gray);
        alpha(Mathf.clamp(e.fout()*1.6f - Interp.pow3In.apply(e.rotation)*1.2f));
        Fill.circle(e.x, e.y, (1f + 6f * e.rotation) - e.fin()*2f);
    }),

    rocketSmokeLarge = new Effect(220, e -> {
        color(Color.gray);
        alpha(Mathf.clamp(e.fout()*1.6f - Interp.pow3In.apply(e.rotation)*1.2f));
        Fill.circle(e.x, e.y, (1f + 6f * e.rotation * 1.3f) - e.fin()*2f);
    }),

    magmasmoke = new Effect(110, e -> {
        color(Color.gray);
        Fill.circle(e.x, e.y, e.fslope() * 6f);
    }),

    spawn = new Effect(30, e -> {
        stroke(2f * e.fout());
        color(Pal.accent);
        Lines.poly(e.x, e.y, 4, 5f + e.fin() * 12f);
    }),

    unitAssemble = new Effect(70, e -> {
        if(!(e.data instanceof UnitType type)) return;

        alpha(e.fout());
        mixcol(Pal.accent, e.fout());
        rect(type.fullIcon, e.x, e.y, e.rotation);
    }).layer(Layer.flyingUnit + 5f),

    padlaunch = new Effect(10, e -> {
        stroke(4f * e.fout());
        color(Pal.accent);
        Lines.poly(e.x, e.y, 4, 5f + e.fin() * 60f);
    }),

    breakProp = new Effect(23, e -> {
        float scl = Math.max(e.rotation, 1);
        color(Tmp.c1.set(e.color).mul(1.1f));
        randLenVectors(e.id, 6, 19f * e.finpow() * scl, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3.5f * scl + 0.3f);
        });
    }).layer(Layer.debris),

    unitDrop = new Effect(30, e -> {
        color(Pal.lightishGray);
        randLenVectors(e.id, 9, 3 + 20f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.4f);
        });
    }).layer(Layer.debris),

    unitLand = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.1f));
        //TODO doesn't respect rotation / size
        randLenVectors(e.id, 6, 17f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.3f);
        });
    }).layer(Layer.debris),

    unitDust = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.3f));
        randLenVectors(e.id, 3, 8f * e.finpow(), e.rotation, 30f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.3f);
        });
    }).layer(Layer.debris),

    unitLandSmall = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.1f));
        randLenVectors(e.id, (int)(6 * e.rotation), 12f * e.finpow() * e.rotation, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.1f);
        });
    }).layer(Layer.debris),

    unitPickup = new Effect(18, e -> {
        color(Pal.lightishGray);
        stroke(e.fin() * 2f);
        Lines.poly(e.x, e.y, 4, 13f * e.fout());
    }).layer(Layer.debris),

    crawlDust = new Effect(35, e -> {
        color(Tmp.c1.set(e.color).mul(1.6f));
        randLenVectors(e.id, 2, 10f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fslope() * 4f + 0.3f);
        });
    }).layer(Layer.debris),

    landShock = new Effect(12, e -> {
        color(Pal.lancerLaser);
        stroke(e.fout() * 3f);
        Lines.poly(e.x, e.y, 12, 20f * e.fout());
    }).layer(Layer.debris),

    pickup = new Effect(18, e -> {
        color(Pal.lightishGray);
        stroke(e.fout() * 2f);
        Lines.spikes(e.x, e.y, 1f + e.fin() * 6f, e.fout() * 4f, 6);
    }),

    titanExplosion = new Effect(30f, 160f, e -> {
        color(e.color);
        stroke(e.fout() * 3f);
        float circleRad = 6f + e.finpow() * 60f;
        Lines.circle(e.x, e.y, circleRad);

        rand.setSeed(e.id);
        for(int i = 0; i < 16; i++){
            float angle = rand.random(360f);
            float lenRand = rand.random(0.5f, 1f);
            Lines.lineAngle(e.x, e.y, angle, e.foutpow() * 50f * rand.random(1f, 0.6f) + 2f, e.finpow() * 70f * lenRand + 6f);
        }
    }),

    titanSmoke = new Effect(300f, 300f, b -> {
        float intensity = 3f;

        color(b.color, 0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.5f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * intensity), 22f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f);

                    Fill.circle(e.x + x, e.y + y, rad);
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f, b.color, 0.5f);
                });
            });
        }
    }),

    missileTrailSmoke = new Effect(180f, 300f, b -> {
        float intensity = 2f;

        color(b.color, 0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.5f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * intensity), 13f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f);

                    Fill.circle(e.x + x, e.y + y, rad);
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f, b.color, 0.5f);
                });
            });
        }
    }).layer(Layer.bullet - 1f),

    scatheExplosion = new Effect(60f, 160f, e -> {
        color(e.color);
        stroke(e.fout() * 5f);
        float circleRad = 6f + e.finpow() * 60f;
        Lines.circle(e.x, e.y, circleRad);

        rand.setSeed(e.id);
        for(int i = 0; i < 16; i++){
            float angle = rand.random(360f);
            float lenRand = rand.random(0.5f, 1f);
            Tmp.v1.trns(angle, circleRad);

            for(int s : Mathf.signs){
                Drawf.tri(e.x + Tmp.v1.x, e.y + Tmp.v1.y, e.foutpow() * 40f, e.fout() * 30f * lenRand + 6f, angle + 90f + s * 90f);
            }
        }
    }),

    scatheLight = new Effect(60f, 160f, e -> {
        float circleRad = 6f + e.finpow() * 60f;

        color(e.color, e.foutpow());
        Fill.circle(e.x, e.y, circleRad);
    }).layer(Layer.bullet + 2f),

    scatheSlash = new Effect(40f, 160f, e -> {
        Draw.color(e.color);
        for(int s : Mathf.signs){
            Drawf.tri(e.x, e.y, e.fout() * 25f, e.foutpow() * 66f + 6f, e.rotation + s * 90f);
        }
    }),

    dynamicSpikes = new Effect(40f, 100f, e -> {
        color(e.color);
        stroke(e.fout() * 2f);
        float circleRad = 4f + e.finpow() * e.rotation;
        Lines.circle(e.x, e.y, circleRad);

        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, e.rotation * 1.5f * e.fout(), i*90);
        }

        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, e.rotation * 1.45f / 3f * e.fout(), i*90);
        }

        Drawf.light(e.x, e.y, circleRad * 1.6f, Pal.heal, e.fout());
    }),

    greenBomb = new Effect(40f, 100f, e -> {
        color(Pal.heal);
        stroke(e.fout() * 2f);
        float circleRad = 4f + e.finpow() * 65f;
        Lines.circle(e.x, e.y, circleRad);

        color(Pal.heal);
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 100f * e.fout(), i*90);
        }

        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 35f * e.fout(), i*90);
        }

        Drawf.light(e.x, e.y, circleRad * 1.6f, Pal.heal, e.fout());
    }),

    greenLaserCharge = new Effect(80f, 100f, e -> {
        color(Pal.heal);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, 4f + e.fout() * 100f);

        Fill.circle(e.x, e.y, e.fin() * 20);

        randLenVectors(e.id, 20, 40f * e.fout(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fin() * 5f);
            Drawf.light(e.x + x, e.y + y, e.fin() * 15f, Pal.heal, 0.7f);
        });

        color();

        Fill.circle(e.x, e.y, e.fin() * 10);
        Drawf.light(e.x, e.y, e.fin() * 20f, Pal.heal, 0.7f);
    }).followParent(true).rotWithParent(true),

    greenLaserChargeSmall = new Effect(40f, 100f, e -> {
        color(Pal.heal);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, e.fout() * 50f);
    }).followParent(true).rotWithParent(true),

    greenCloud = new Effect(80f, e -> {
        color(Pal.heal);
        randLenVectors(e.id, e.fin(), 7, 9f, (x, y, fin, fout) -> {
            Fill.circle(e.x + x, e.y + y, 5f * fout);
        });
    }),

    healWaveDynamic = new Effect(22, e -> {
        color(Pal.heal);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * e.rotation);
    }),

    healWave = new Effect(22, e -> {
        color(Pal.heal);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 60f);
    }),

    heal = new Effect(11, e -> {
        color(Pal.heal);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 2f + e.finpow() * 7f);
    }),

    shieldWave = new Effect(22, e -> {
        color(e.color, 0.7f);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 60f);
    }),

    shieldApply = new Effect(11, e -> {
        color(e.color, 0.7f);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 2f + e.finpow() * 7f);
    }),

    disperseTrail = new Effect(13, e -> {
        color(Color.white, e.color, e.fin());
        stroke(0.6f + e.fout() * 1.7f);
        rand.setSeed(e.id);

        for(int i = 0; i < 2; i++){
            float rot = e.rotation + rand.range(15f) + 180f;
            v.trns(rot, rand.random(e.fin() * 27f));
            lineAngle(e.x + v.x, e.y + v.y, rot, e.fout() * rand.random(2f, 7f) + 1.5f);
        }
    }),


    hitBulletSmall = new Effect(14, e -> {
        color(Color.white, Pal.lightOrange, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 5f);
        });

        stroke(0.5f + e.fout());

        randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });

        Drawf.light(e.x, e.y, 20f, Pal.lightOrange, 0.6f * e.fout());
    }),

    hitBulletColor = new Effect(14, e -> {
        color(Color.white, e.color, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 5f);
        });

        stroke(0.5f + e.fout());

        randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });

        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),

    hitSquaresColor = new Effect(14, e -> {
        color(Color.white, e.color, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 5f);
        });

        stroke(0.5f + e.fout());

        randLenVectors(e.id, 5, e.fin() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            Fill.square(e.x + x, e.y + y, e.fout() * 3.2f, ang);
        });

        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),

    hitFuse = new Effect(14, e -> {
        color(Color.white, Pal.surge, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 7f);
        });

        stroke(0.5f + e.fout());

        randLenVectors(e.id, 6, e.fin() * 15f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });
    }),

    hitBulletBig = new Effect(13, e -> {
        color(Color.white, Pal.lightOrange, e.fin());
        stroke(0.5f + e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 30f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1.5f);
        });
    }),

    hitFlameSmall = new Effect(14, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());
        stroke(0.5f + e.fout());

        randLenVectors(e.id, 2, 1f + e.fin() * 15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });
    }),

    hitFlamePlasma = new Effect(14, e -> {
        color(Color.white, Pal.heal, e.fin());
        stroke(0.5f + e.fout());

        randLenVectors(e.id, 2, 1f + e.fin() * 15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });
    }),

    hitLiquid = new Effect(16, e -> {
        color(e.color);

        randLenVectors(e.id, 5, 1f + e.fin() * 15f, e.rotation, 60f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
        });
    }),
    
    hitLaserBlast = new Effect(12, e -> {
        color(e.color);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    hitEmpSpark = new Effect(40, e -> {
        color(Pal.heal);
        stroke(e.fout() * 1.6f);

        randLenVectors(e.id, 18, e.finpow() * 27f, e.rotation, 360f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 6 + 1f);
        });
    }),

    hitLancer = new Effect(12, e -> {
        color(Color.white);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    hitBeam = new Effect(12, e -> {
        color(e.color);
        stroke(e.fout() * 2f);

        randLenVectors(e.id, 6, e.finpow() * 18f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    hitFlameBeam = new Effect(19, e -> {
        color(e.color);

        randLenVectors(e.id, 7, e.finpow() * 11f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2 + 0.5f);
        });
    }),

    hitMeltdown = new Effect(12, e -> {
        color(Pal.meltdownHit);
        stroke(e.fout() * 2f);

        randLenVectors(e.id, 6, e.finpow() * 18f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    hitMeltHeal = new Effect(12, e -> {
        color(Pal.heal);
        stroke(e.fout() * 2f);

        randLenVectors(e.id, 6, e.finpow() * 18f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    instBomb = new Effect(15f, 100f, e -> {
        color(Pal.bulletYellowBack);
        stroke(e.fout() * 4f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 20f);

        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 80f * e.fout(), i*90 + 45);
        }

        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 30f * e.fout(), i*90 + 45);
        }

        Drawf.light(e.x, e.y, 150f, Pal.bulletYellowBack, 0.9f * e.fout());
    }),

    instTrail = new Effect(30, e -> {
        for(int i = 0; i < 2; i++){
            color(i == 0 ? Pal.bulletYellowBack : Pal.bulletYellow);

            float m = i == 0 ? 1f : 0.5f;

            float rot = e.rotation + 180f;
            float w = 15f * e.fout() * m;
            Drawf.tri(e.x, e.y, w, (30f + Mathf.randomSeedRange(e.id, 15f)) * m, rot);
            Drawf.tri(e.x, e.y, w, 10f * m, rot + 180f);
        }

        Drawf.light(e.x, e.y, 60f, Pal.bulletYellowBack, 0.6f * e.fout());
    }),

    instShoot = new Effect(24f, e -> {
        e.scaled(10f, b -> {
            color(Color.white, Pal.bulletYellowBack, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });

        color(Pal.bulletYellowBack);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
            Drawf.tri(e.x, e.y, 13f * e.fout(), 50f, e.rotation + 20f * i);
        }

        Drawf.light(e.x, e.y, 180f, Pal.bulletYellowBack, 0.9f * e.fout());
    }),

    instHit = new Effect(20f, 200f, e -> {
        color(Pal.bulletYellowBack);

        for(int i = 0; i < 2; i++){
            color(i == 0 ? Pal.bulletYellowBack : Pal.bulletYellow);

            float m = i == 0 ? 1f : 0.5f;

            for(int j = 0; j < 5; j++){
                float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                float w = 23f * e.fout() * m;
                Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
            }
        }

        e.scaled(10f, c -> {
            color(Pal.bulletYellow);
            stroke(c.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, c.fin() * 30f);
        });

        e.scaled(12f, c -> {
            color(Pal.bulletYellowBack);
            randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
            });
        });
    }),

    hitLaser = new Effect(8, e -> {
        color(Color.white, Pal.heal, e.fin());
        stroke(0.5f + e.fout());
        Lines.circle(e.x, e.y, e.fin() * 5f);

        Drawf.light(e.x, e.y, 23f, Pal.heal, e.fout() * 0.7f);
    }),

    hitLaserColor = new Effect(8, e -> {
        color(Color.white, e.color, e.fin());
        stroke(0.5f + e.fout());
        Lines.circle(e.x, e.y, e.fin() * 5f);

        Drawf.light(e.x, e.y, 23f, e.color, e.fout() * 0.7f);
    }),

    despawn = new Effect(12, e -> {
        color(Pal.lighterOrange, Color.gray, e.fin());
        stroke(e.fout());

        randLenVectors(e.id, 7, e.fin() * 7f, e.rotation, 40f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 2 + 1f);
        });

    }),

    airBubble = new Effect(100f, e -> {
        randLenVectors(e.id, 1, e.fin() * 12f, (x, y) -> {
            rect(renderer.bubbles[Math.min((int)(renderer.bubbles.length * Mathf.curveMargin(e.fin(), 0.11f, 0.06f)), renderer.bubbles.length - 1)], e.x + x, e.y + y);
        });
    }).layer(Layer.flyingUnitLow + 1),

    flakExplosion = new Effect(20, e -> {
        color(Pal.bulletYellow);

        e.scaled(6, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
        });

        color(Color.gray);

        randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
        });

        color(Pal.lighterOrange);
        stroke(e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

        Drawf.light(e.x, e.y, 50f, Pal.lighterOrange, 0.8f * e.fout());
    }),

    plasticExplosion = new Effect(24, e -> {
        color(Pal.plastaniumFront);

        e.scaled(7, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 24f);
        });

        color(Color.gray);

        randLenVectors(e.id, 7, 2f + 28f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.plastaniumBack);
        stroke(e.fout());

        randLenVectors(e.id + 1, 4, 1f + 25f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

        Drawf.light(e.x, e.y, 50f, Pal.plastaniumBack, 0.8f * e.fout());
    }),

    plasticExplosionFlak = new Effect(28, e -> {
        color(Pal.plastaniumFront);

        e.scaled(7, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 34f);
        });

        color(Color.gray);

        randLenVectors(e.id, 7, 2f + 30f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.plastaniumBack);
        stroke(e.fout());

        randLenVectors(e.id + 1, 4, 1f + 30f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });
    }),

    blastExplosion = new Effect(22, e -> {
        color(Pal.missileYellow);

        e.scaled(6, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 15f);
        });

        color(Color.gray);

        randLenVectors(e.id, 5, 2f + 23f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.missileYellowBack);
        stroke(e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

        Drawf.light(e.x, e.y, 45f, Pal.missileYellowBack, 0.8f * e.fout());
    }),

    sapExplosion = new Effect(25, e -> {
        color(Pal.sapBullet);

        e.scaled(6, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 80f);
        });

        color(Color.gray);

        randLenVectors(e.id, 9, 2f + 70 * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.sapBulletBack);
        stroke(e.fout());

        randLenVectors(e.id + 1, 8, 1f + 60f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

        Drawf.light(e.x, e.y, 90f, Pal.sapBulletBack, 0.8f * e.fout());
    }),

    massiveExplosion = new Effect(30, e -> {
        color(Pal.missileYellow);

        e.scaled(7, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 4f + i.fin() * 30f);
        });

        color(Color.gray);

        randLenVectors(e.id, 8, 2f + 30f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.missileYellowBack);
        stroke(e.fout());

        randLenVectors(e.id + 1, 6, 1f + 29f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 4f);
        });

        Drawf.light(e.x, e.y, 50f, Pal.missileYellowBack, 0.8f * e.fout());
    }),

    artilleryTrail = new Effect(50, e -> {
        color(e.color);
        Fill.circle(e.x, e.y, e.rotation * e.fout());
    }),

    incendTrail = new Effect(50, e -> {
        color(Pal.lightOrange);
        Fill.circle(e.x, e.y, e.rotation * e.fout());
    }),

    missileTrail = new Effect(50, e -> {
        color(e.color);
        Fill.circle(e.x, e.y, e.rotation * e.fout());
    }).layer(Layer.bullet - 0.001f), //below bullets

    missileTrailShort = new Effect(22, e -> {
        color(e.color);
        Fill.circle(e.x, e.y, e.rotation * e.fout());
    }).layer(Layer.bullet - 0.001f),

    colorTrail = new Effect(50, e -> {
        color(e.color);
        Fill.circle(e.x, e.y, e.rotation * e.fout());
    }),

    absorb = new Effect(12, e -> {
        color(Pal.accent);
        stroke(2f * e.fout());
        Lines.circle(e.x, e.y, 5f * e.fout());
    }),
    
    forceShrink = new Effect(20, e -> {
        color(e.color, e.fout());
        if(renderer.animateShields){
            Fill.poly(e.x, e.y, 6, e.rotation * e.fout());
        }else{
            stroke(1.5f);
            Draw.alpha(0.09f);
            Fill.poly(e.x, e.y, 6, e.rotation * e.fout());
            Draw.alpha(1f);
            Lines.poly(e.x, e.y, 6, e.rotation * e.fout());
        }
    }).layer(Layer.shields),

    flakExplosionBig = new Effect(30, e -> {
        color(Pal.bulletYellowBack);

        e.scaled(6, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 25f);
        });

        color(Color.gray);

        randLenVectors(e.id, 6, 2f + 23f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.5f);
        });

        color(Pal.bulletYellow);
        stroke(e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

        Drawf.light(e.x, e.y, 60f, Pal.bulletYellowBack, 0.7f * e.fout());
    }),

    burning = new Effect(35f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
        });
    }),

    fireRemove = new Effect(70f, e -> {
        if(Fire.regions[0] == null) return;
        alpha(e.fout());
        rect(Fire.regions[((int)(e.rotation + e.fin() * Fire.frames)) % Fire.frames], e.x + Mathf.randomSeedRange((int)e.y, 2), e.y + Mathf.randomSeedRange((int)e.x, 2));
        Drawf.light(e.x, e.y, 50f + Mathf.absin(5f, 5f), Pal.lightFlame, 0.6f  * e.fout());
    }),

    fire = new Effect(50f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 2, 2f + e.fin() * 9f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
        });

        color();

        Drawf.light(e.x, e.y, 20f * e.fslope(), Pal.lightFlame, 0.5f);
    }),

    fireHit = new Effect(35f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 3, 2f + e.fin() * 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.6f);
        });

        color();
    }),

    fireSmoke = new Effect(35f, e -> {
        color(Color.gray);

        randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
        });
    }),

    //TODO needs a lot of work
    neoplasmHeal = new Effect(120f, e -> {
        color(Pal.neoplasm1, Pal.neoplasm2, e.fin());

        randLenVectors(e.id, 1, e.fin() * 3f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 2f);
        });
    }).followParent(true).rotWithParent(true).layer(Layer.bullet - 2),

    steam = new Effect(35f, e -> {
        color(Color.lightGray);

        randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
        });
    }),

    ventSteam = new Effect(140f, e -> {
        color(e.color, Pal.vent2, e.fin());

        alpha(e.fslope() * 0.78f);

        float length = 3f + e.finpow() * 10f;
        rand.setSeed(e.id);
        for(int i = 0; i < rand.random(3, 5); i++){
            v.trns(rand.random(360f), rand.random(length));
            Fill.circle(e.x + v.x, e.y + v.y, rand.random(1.2f, 3.5f) + e.fslope() * 1.1f);
        }
    }).layer(Layer.darkness - 1),

    drillSteam = new Effect(220f, e -> {

        float length = 3f + e.finpow() * 20f;
        rand.setSeed(e.id);
        for(int i = 0; i < 13; i++){
            v.trns(rand.random(360f), rand.random(length));
            float sizer = rand.random(1.3f, 3.7f);

            e.scaled(e.lifetime * rand.random(0.5f, 1f), b -> {
                color(Color.gray, b.fslope() * 0.93f);

                Fill.circle(e.x + v.x, e.y + v.y, sizer + b.fslope() * 1.2f);
            });
        }
    }).startDelay(30f),

    vapor = new Effect(110f, e -> {
        color(e.color);
        alpha(e.fout());

        randLenVectors(e.id, 3, 2f + e.finpow() * 11f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.6f + e.fin() * 5f);
        });
    }),

    vaporSmall = new Effect(50f, e -> {
        color(e.color);
        alpha(e.fout());

        randLenVectors(e.id, 4, 2f + e.finpow() * 5f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1f + e.fin() * 4f);
        });
    }),

    fireballsmoke = new Effect(25f, e -> {
        color(Color.gray);

        randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
        });
    }),

    ballfire = new Effect(25f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fout() * 1.5f);
        });
    }),

    freezing = new Effect(40f, e -> {
        color(Liquids.cryofluid.color);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 1.2f);
        });
    }),

    melting = new Effect(40f, e -> {
        color(Liquids.slag.color, Color.white, e.fout() / 5f + Mathf.randomSeedRange(e.id, 0.12f));

        randLenVectors(e.id, 2, 1f + e.fin() * 3f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, .2f + e.fout() * 1.2f);
        });
    }),

    wet = new Effect(80f, e -> {
        color(Liquids.water.color);
        alpha(Mathf.clamp(e.fin() * 2f));

        Fill.circle(e.x, e.y, e.fout());
    }),

    muddy = new Effect(80f, e -> {
        color(Pal.muddy);
        alpha(Mathf.clamp(e.fin() * 2f));

        Fill.circle(e.x, e.y, e.fout());
    }),

    sapped = new Effect(40f, e -> {
        color(Pal.sap);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fslope() * 1.1f, 45f);
        });
    }),

    electrified = new Effect(40f, e -> {
        color(Pal.heal);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fslope() * 1.1f, 45f);
        });
    }),

    sporeSlowed = new Effect(40f, e -> {
        color(Pal.spore);

        Fill.circle(e.x, e.y, e.fslope() * 1.1f);
    }),

    oily = new Effect(42f, e -> {
        color(Liquids.oil.color);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout());
        });
    }),

    overdriven = new Effect(20f, e -> {
        color(e.color);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2.3f + 0.5f);
        });
    }),

    overclocked = new Effect(50f, e -> {
        color(e.color);

        Fill.square(e.x, e.y, e.fslope() * 2f, 45f);
    }),

    dropItem = new Effect(20f, e -> {
        float length = 20f * e.finpow();
        float size = 7f * e.fout();

        if(!(e.data instanceof Item item)) return;

        rect(item.fullIcon, e.x + trnsx(e.rotation, length), e.y + trnsy(e.rotation, length), size, size);
    }),

    shockwave = new Effect(10f, 80f, e -> {
        color(Color.white, Color.lightGray, e.fin());
        stroke(e.fout() * 2f + 0.2f);
        Lines.circle(e.x, e.y, e.fin() * 28f);
    }),

    bigShockwave = new Effect(10f, 80f, e -> {
        color(Color.white, Color.lightGray, e.fin());
        stroke(e.fout() * 3f);
        Lines.circle(e.x, e.y, e.fin() * 50f);
    }),

    spawnShockwave = new Effect(20f, 400f, e -> {
        color(Color.white, Color.lightGray, e.fin());
        stroke(e.fout() * 3f + 0.5f);
        Lines.circle(e.x, e.y, e.fin() * (e.rotation + 50f));
    }),

    explosion = new Effect(30, e -> {
        e.scaled(7, i -> {
            stroke(3f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 10f);
        });

        color(Color.gray);

        randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout());
        });

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        stroke(1.5f * e.fout());

        randLenVectors(e.id + 1, 8, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });
    }),

    dynamicExplosion = new Effect(30, 500f, b -> {
        float intensity = b.rotation;
        float baseLifetime = 26f + intensity * 15f;
        b.lifetime = 43f + intensity * 35f;

        color(Color.gray);
        //TODO awful borders with linear filtering here
        alpha(0.9f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(3f * intensity), 14f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    Fill.circle(e.x + x, e.y + y, fout * ((2f + intensity) * 1.8f));
                });
            });
        }

        b.scaled(baseLifetime, e -> {
            e.scaled(5 + intensity * 2.5f, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(e.x, e.y, (3f + i.fin() * 14f) * intensity);
                Drawf.light(e.x, e.y, i.fin() * 14f * 2f * intensity, Color.white, 0.9f * e.fout());
            });

            color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            stroke((1.7f * e.fout()) * (1f + (intensity - 1f) / 2f));

            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(9 * intensity), 40f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (3f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
            });
        });
    }),

    reactorExplosion = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;

        color(Pal.reactorPurple2);
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * intensity), 22f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f);

                    Fill.circle(e.x + x, e.y + y, rad);
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f, Pal.reactorPurple, 0.5f);
                });
            });
        }

        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(e.x, e.y, (3f + i.fin() * 14f) * intensity);
                Drawf.light(e.x, e.y, i.fin() * 14f * 2f * intensity, Color.white, 0.9f * e.fout());
            });

            color(Pal.lighterOrange, Pal.reactorPurple, e.fin());
            stroke((2f * e.fout()));

            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), 28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
            });
        });
    }),

    impactReactorExplosion = new Effect(30, 500f, b -> {
        float intensity = 8f;
        float baseLifetime = 25f + intensity * 15f;
        b.lifetime = 50f + intensity * 64f;

        color(Pal.lighterOrange);
        alpha(0.8f);
        for(int i = 0; i < 5; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.25f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.8f * intensity), 25f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f);

                    Fill.circle(e.x + x, e.y + y, rad);
                    Drawf.light(e.x + x, e.y + y, rad * 2.6f, Pal.lighterOrange, 0.7f);
                });
            });
        }

        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(e.x, e.y, (3f + i.fin() * 14f) * intensity);
                Drawf.light(e.x, e.y, i.fin() * 14f * 2f * intensity, Color.white, 0.9f * e.fout());
            });

            color(Color.white, Pal.lighterOrange, e.fin());
            stroke((2f * e.fout()));

            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), 30f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
            });
        });
    }),

    blockExplosionSmoke = new Effect(30, e -> {
        color(Color.gray);

        randLenVectors(e.id, 6, 4f + 30f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout());
        });
    }),

    shootSmall = new Effect(8, e -> {
        color(Pal.lighterOrange, Pal.lightOrange, e.fin());
        float w = 1f + 5 * e.fout();
        Drawf.tri(e.x, e.y, w, 15f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 3f * e.fout(), e.rotation + 180f);
    }),

    shootSmallColor = new Effect(8, e -> {
        color(e.color, Color.gray, e.fin());
        float w = 1f + 5 * e.fout();
        Drawf.tri(e.x, e.y, w, 15f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 3f * e.fout(), e.rotation + 180f);
    }),

    shootHeal = new Effect(8, e -> {
        color(Pal.heal);
        float w = 1f + 5 * e.fout();
        Drawf.tri(e.x, e.y, w, 17f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
    }),

    shootHealYellow = new Effect(8, e -> {
        color(Pal.lightTrail);
        float w = 1f + 5 * e.fout();
        Drawf.tri(e.x, e.y, w, 17f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
    }),

    shootSmallSmoke = new Effect(20f, e -> {
        color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());

        randLenVectors(e.id, 5, e.finpow() * 6f, e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 1.5f);
        });
    }),

    shootBig = new Effect(9, e -> {
        color(Pal.lighterOrange, Pal.lightOrange, e.fin());
        float w = 1.2f + 7 * e.fout();
        Drawf.tri(e.x, e.y, w, 25f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 4f * e.fout(), e.rotation + 180f);
    }),

    shootBig2 = new Effect(10, e -> {
        color(Pal.lightOrange, Color.gray, e.fin());
        float w = 1.2f + 8 * e.fout();
        Drawf.tri(e.x, e.y, w, 29f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 5f * e.fout(), e.rotation + 180f);
    }),

    shootBigColor = new Effect(11, e -> {
        color(e.color, Color.gray, e.fin());
        float w = 1.2f +9 * e.fout();
        Drawf.tri(e.x, e.y, w, 32f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 3f * e.fout(), e.rotation + 180f);
    }),

    shootTitan = new Effect(10, e -> {
        color(Pal.lightOrange, e.color, e.fin());
        float w = 1.3f + 10 * e.fout();
        Drawf.tri(e.x, e.y, w, 35f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 6f * e.fout(), e.rotation + 180f);
    }),

    shootBigSmoke = new Effect(17f, e -> {
        color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());

        randLenVectors(e.id, 8, e.finpow() * 19f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f + 0.2f);
        });
    }),

    shootBigSmoke2 = new Effect(18f, e -> {
        color(Pal.lightOrange, Color.lightGray, Color.gray, e.fin());

        randLenVectors(e.id, 9, e.finpow() * 23f, e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2.4f + 0.2f);
        });
    }),

    shootSmokeDisperse = new Effect(25f, e -> {
        color(Pal.lightOrange, Color.white, Color.gray, e.fin());

        randLenVectors(e.id, 9, e.finpow() * 29f, e.rotation, 18f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2.2f + 0.1f);
        });
    }),

    shootSmokeSquare = new Effect(20f, e -> {
        color(Color.white, e.color, e.fin());

        rand.setSeed(e.id);
        for(int i = 0; i < 6; i++){
            float rot = e.rotation + rand.range(22f);
            v.trns(rot, rand.random(e.finpow() * 21f));
            Fill.poly(e.x + v.x, e.y + v.y, 4, e.fout() * 2f + 0.2f, rand.random(360f));
        }
    }),

    shootSmokeSquareSparse = new Effect(30f, e -> {
        color(Color.white, e.color, e.fin());

        rand.setSeed(e.id);
        for(int i = 0; i < 2; i++){
            float rot = e.rotation + rand.range(30f);
            v.trns(rot, rand.random(e.finpow() * 27f));
            Fill.poly(e.x + v.x, e.y + v.y, 4, e.fout() * 3.8f + 0.2f, rand.random(360f));
        }
    }),

    shootSmokeSquareBig = new Effect(30f, e -> {
        color(Color.white, e.color, e.fin());

        rand.setSeed(e.id);
        for(int i = 0; i < 8; i++){
            float rot = e.rotation + rand.range(22f);
            v.trns(rot, rand.random(e.finpow() * 24f));
            Fill.poly(e.x + v.x, e.y + v.y, 4, e.fout() * 3.8f + 0.2f, rand.random(360f));
        }
    }),

    shootSmokeTitan = new Effect(70f, e -> {
        rand.setSeed(e.id);
        for(int i = 0; i < 13; i++){
            v.trns(e.rotation + rand.range(30f), rand.random(e.finpow() * 40f));
            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                color(e.color, Pal.lightishGray, b.fin());
                Fill.circle(e.x + v.x, e.y + v.y, b.fout() * 3.4f + 0.3f);
            });
        }
    }),

    shootSmokeSmite = new Effect(70f, e -> {
        rand.setSeed(e.id);
        for(int i = 0; i < 13; i++){
            float a = e.rotation + rand.range(30f);
            v.trns(a, rand.random(e.finpow() * 50f));
            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                color(e.color);
                Lines.stroke(b.fout() * 3f + 0.5f);
                Lines.lineAngle(e.x + v.x, e.y + v.y, a, b.fout() * 8f + 0.4f);
            });
        }
    }),

    shootSmokeMissile = new Effect(130f, 300f, e -> {
        color(Pal.redLight);
        alpha(0.5f);
        rand.setSeed(e.id);
        for(int i = 0; i < 35; i++){
            v.trns(e.rotation + 180f + rand.range(21f), rand.random(e.finpow() * 90f)).add(rand.range(3f), rand.range(3f));
            e.scaled(e.lifetime * rand.random(0.2f, 1f), b -> {
                Fill.circle(e.x + v.x, e.y + v.y, b.fout() * 9f + 0.3f);
            });
        }
    }),

    regenParticle = new Effect(100f, e -> {
        color(Pal.regen);

        Fill.square(e.x, e.y, e.fslope() * 1.5f + 0.14f, 45f);
    }),

    regenSuppressParticle = new Effect(35f, e -> {
        color(Pal.sapBullet, e.color, e.fin());
        stroke(e.fout() * 1.4f + 0.5f);

        randLenVectors(e.id, 4, 17f * e.fin(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 3f + 0.5f);
        });
    }),

    regenSuppressSeek = new Effect(140f, e -> {
        e.lifetime = Mathf.randomSeed(e.id, 120f, 200f);

        if(!(e.data instanceof Position to)) return;

        Tmp.v2.set(to).sub(e.x, e.y).nor().rotate90(1).scl(Mathf.randomSeedRange(e.id, 1f) * 50f);

        Tmp.bz2.set(Tmp.v1.set(e.x, e.y), Tmp.v2.add(e.x, e.y), Tmp.v3.set(to));

        Tmp.bz2.valueAt(Tmp.v4, e.fout());

        color(Pal.sapBullet);
        Fill.circle(Tmp.v4.x, Tmp.v4.y, e.fslope() * 2f + 0.1f);
    }).followParent(false).rotWithParent(false),

    surgeCruciSmoke = new Effect(160f, e -> {
        color(Pal.slagOrange);
        alpha(0.6f);

        rand.setSeed(e.id);
        for(int i = 0; i < 3; i++){
            float len = rand.random(6f), rot = rand.range(40f) + e.rotation;

            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                v.trns(rot, len * b.finpow());
                Fill.circle(e.x + v.x, e.y + v.y, 2f * b.fslope() + 0.2f);
            });
        }
    }),

    heatReactorSmoke = new Effect(180f, e -> {
        color(Color.gray);

        rand.setSeed(e.id);
        for(int i = 0; i < 5; i++){
            float len = rand.random(6f), rot = rand.range(50f) + e.rotation;

            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                alpha(0.9f * b.fout());
                v.trns(rot, len * b.finpow());
                Fill.circle(e.x + v.x, e.y + v.y, 2.4f * b.fin() + 0.6f);
            });
        }
    }),

    circleColorSpark = new Effect(21f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1.1f + 0.5f);

        randLenVectors(e.id, 9, 27f * e.fin(), 9f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 5f + 0.5f);
        });
    }),

    colorSpark = new Effect(21f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1.1f + 0.5f);

        randLenVectors(e.id, 5, 27f * e.fin(), e.rotation, 9f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 5f + 0.5f);
        });
    }),

    colorSparkBig = new Effect(25f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1.3f + 0.7f);

        randLenVectors(e.id, 8, 41f * e.fin(), e.rotation, 10f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 6f + 0.5f);
        });
    }),

    randLifeSpark = new Effect(24f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1.5f + 0.5f);

        rand.setSeed(e.id);
        for(int i = 0; i < 15; i++){
            float ang = e.rotation + rand.range(9f), len = rand.random(90f * e.finpow());
            e.scaled(e.lifetime * rand.random(0.5f, 1f), p -> {
                v.trns(ang, len);
                lineAngle(e.x + v.x, e.y + v.y, ang, p.fout() * 7f + 0.5f);
            });
        }
    }),

    shootPayloadDriver = new Effect(30f, e -> {
        color(Pal.accent);
        Lines.stroke(0.5f + 0.5f*e.fout());
        float spread = 9f;

        rand.setSeed(e.id);
        for(int i = 0; i < 20; i++){
            float ang = e.rotation + rand.range(17f);
            v.trns(ang, rand.random(e.fin() * 55f));
            Lines.lineAngle(e.x + v.x + rand.range(spread), e.y + v.y + rand.range(spread), ang, e.fout() * 5f * rand.random(1f) + 1f);
        }
    }),

    shootSmallFlame = new Effect(32f, 80f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, Color.gray, e.fin());

        randLenVectors(e.id, 8, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
        });
    }),

    shootPyraFlame = new Effect(33f, 80f, e -> {
        color(Pal.lightPyraFlame, Pal.darkPyraFlame, Color.gray, e.fin());

        randLenVectors(e.id, 10, e.finpow() * 70f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.6f);
        });
    }),

    shootLiquid = new Effect(15f, 80f, e -> {
        color(e.color);

        randLenVectors(e.id, 2, e.finpow() * 15f, e.rotation, 11f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.5f + e.fout() * 2.5f);
        });
    }),

    casing1 = new Effect(30f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.3f));
        float rot = Math.abs(e.rotation) + 90f;
        int i = -Mathf.sign(e.rotation);

        float len = (2f + e.finpow() * 6f) * i;
        float lr = rot + e.fin() * 30f * i;
        Fill.rect(
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            1f, 2f, rot + e.fin() * 50f * i
        );

    }).layer(Layer.bullet),

    casing2 = new Effect(34f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;
        int i = -Mathf.sign(e.rotation);
        float len = (2f + e.finpow() * 10f) * i;
        float lr = rot + e.fin() * 20f * i;
        rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2f, 3f, rot + e.fin() * 50f * i
        );
    }).layer(Layer.bullet),

    casing3 = new Effect(40f, e -> {
        color(Pal.lightOrange, Pal.lightishGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;
        int i = -Mathf.sign(e.rotation);
        float len = (4f + e.finpow() * 9f) * i;
        float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;

        rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2.5f, 4f,
            rot + e.fin() * 50f * i
        );
    }).layer(Layer.bullet),

    casing4 = new Effect(45f, e -> {
        color(Pal.lightOrange, Pal.lightishGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;
        int i = -Mathf.sign(e.rotation);
        float len = (4f + e.finpow() * 9f) * i;
        float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;

        rect(Core.atlas.find("casing"),
        e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
        e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
        3f, 6f,
        rot + e.fin() * 50f * i
        );
    }).layer(Layer.bullet),

    casing2Double = new Effect(34f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.finpow() * 10f) * i;
            float lr = rot + e.fin() * 20f * i;
            rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2f, 3f, rot + e.fin() * 50f * i
            );
        }

    }).layer(Layer.bullet),

    casing3Double = new Effect(40f, e -> {
        color(Pal.lightOrange, Pal.lightishGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;

        for(int i : Mathf.signs){
            float len = (4f + e.finpow() * 9f) * i;
            float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;

            rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2.5f, 4f,
            rot + e.fin() * 50f * i
            );
        }

    }).layer(Layer.bullet),

    railShoot = new Effect(24f, e -> {
        e.scaled(10f, b -> {
            color(Color.white, Color.lightGray, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });

        color(Pal.orangeSpark);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
        }
    }),

    railTrail = new Effect(16f, e -> {
        color(Pal.orangeSpark);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 10f * e.fout(), 24f, e.rotation + 90 + 90f * i);
        }

        Drawf.light(e.x, e.y, 60f * e.fout(), Pal.orangeSpark, 0.5f);
    }),

    railHit = new Effect(18f, 200f, e -> {
        color(Pal.orangeSpark);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 10f * e.fout(), 60f, e.rotation + 140f * i);
        }
    }),

    lancerLaserShoot = new Effect(21f, e -> {
        color(Pal.lancerLaser);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i);
        }
    }),

    lancerLaserShootSmoke = new Effect(26f, e -> {
        color(Color.white);
        float length = !(e.data instanceof Float) ? 70f : (Float)e.data;

        randLenVectors(e.id, 7, length, e.rotation, 0f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 9f);
        });
    }),

    lancerLaserCharge = new Effect(38f, e -> {
        color(Pal.lancerLaser);

        randLenVectors(e.id, 14, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 3f + 1f);
        });
    }),

    lancerLaserChargeBegin = new Effect(60f, e -> {
        float margin = 1f - Mathf.curve(e.fin(), 0.9f);
        float fin = Math.min(margin, e.fin());

        color(Pal.lancerLaser);
        Fill.circle(e.x, e.y, fin * 3f);

        color();
        Fill.circle(e.x, e.y, fin * 2f);
    }),

    lightningCharge = new Effect(38f, e -> {
        color(Pal.lancerLaser);

        randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
            Drawf.tri(e.x + x, e.y + y, e.fslope() * 3f + 1, e.fslope() * 3f + 1, Mathf.angle(x, y));
        });
    }),

    sparkShoot = new Effect(12f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1.2f + 0.6f);

        randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 3f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 5f + 0.5f);
        });
    }),

    lightningShoot = new Effect(12f, e -> {
        color(Color.white, Pal.lancerLaser, e.fin());
        stroke(e.fout() * 1.2f + 0.5f);

        randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 50f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 5f + 2f);
        });
    }),

    thoriumShoot = new Effect(12f, e -> {
        color(Color.white, Pal.thoriumPink, e.fin());
        stroke(e.fout() * 1.2f + 0.5f);

        randLenVectors(e.id, 7, 25f * e.finpow(), e.rotation, 50f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 5f + 2f);
        });
    }),

    reactorsmoke = new Effect(17, e -> {
        randLenVectors(e.id, 4, e.fin() * 8f, (x, y) -> {
            float size = 1f + e.fout() * 5f;
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    redgeneratespark = new Effect(90, e -> {
        color(Pal.redSpark);
        alpha(e.fslope());

        rand.setSeed(e.id);
        for(int i = 0; i < 2; i++){
            v.trns(rand.random(360f), rand.random(e.finpow() * 9f)).add(e.x, e.y);
            Fill.circle(v.x, v.y, rand.random(1.4f, 2.4f));
        }
    }).layer(Layer.bullet - 1f),

    turbinegenerate = new Effect(100, e -> {
        color(Pal.vent);
        alpha(e.fslope() * 0.8f);

        rand.setSeed(e.id);
        for(int i = 0; i < 3; i++){
            v.trns(rand.random(360f), rand.random(e.finpow() * 14f)).add(e.x, e.y);
            Fill.circle(v.x, v.y, rand.random(1.4f, 3.4f));
        }
    }).layer(Layer.bullet - 1f),

    generatespark = new Effect(18, e -> {
        randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
            color(Pal.orangeSpark, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f /2f);
        });
    }),

    fuelburn = new Effect(23, e -> {
        randLenVectors(e.id, 5, e.fin() * 9f, (x, y) -> {
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
        });
    }),

    incinerateSlag = new Effect(34, e -> {
        randLenVectors(e.id, 4, e.finpow() * 5f, (x, y) -> {
            color(Pal.slagOrange, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 1.7f);
        });
    }),

    coreBurn = new Effect(23, e -> {
        randLenVectors(e.id, 5, e.fin() * 9f, (x, y) -> {
            float len = e.fout() * 4f;
            color(Pal.accent, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, len/2f);
        });
    }),

    plasticburn = new Effect(40, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 5f, (x, y) -> {
            color(Pal.plasticBurn, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout());
        });
    }),

    conveyorPoof = new Effect(35, e -> {
        color(Pal.plasticBurn, Color.gray, e.fin());
        randLenVectors(e.id, 4, 3f + e.fin() * 4f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 1.11f);
        });
    }),

    pulverize = new Effect(40, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
            color(Pal.stoneGray);
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
        });
    }),

    pulverizeRed = new Effect(40, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
            color(Pal.redDust, Pal.stoneGray, e.fin());
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
        });
    }),

    pulverizeSmall = new Effect(30, e -> {
        randLenVectors(e.id, 3, e.fin() * 5f, (x, y) -> {
            color(Pal.stoneGray);
            Fill.square(e.x + x, e.y + y, e.fout() + 0.5f, 45);
        });
    }),

    pulverizeMedium = new Effect(30, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
            color(Pal.stoneGray);
            Fill.square(e.x + x, e.y + y, e.fout() + 0.5f, 45);
        });
    }),

    producesmoke = new Effect(12, e -> {
        randLenVectors(e.id, 8, 4f + e.fin() * 18f, (x, y) -> {
            color(Color.white, Pal.accent, e.fin());
            Fill.square(e.x + x, e.y + y, 1f + e.fout() * 3f, 45);
        });
    }),

    smokeCloud = new Effect(70, e -> {
        randLenVectors(e.id, e.fin(), 30, 30f, (x, y, fin, fout) -> {
            color(Color.gray);
            alpha((0.5f - Math.abs(fin - 0.5f)) * 2f);
            Fill.circle(e.x + x, e.y + y, 0.5f + fout * 4f);
        });
    }),

    smeltsmoke = new Effect(15, e -> {
        randLenVectors(e.id, 6, 4f + e.fin() * 5f, (x, y) -> {
            color(Color.white, e.color, e.fin());
            Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
        });
    }),

    coalSmeltsmoke = new Effect(40f, e -> {
        randLenVectors(e.id, 0.2f + e.fin(), 4, 6.3f, (x, y, fin, out) -> {
            color(Color.darkGray, Pal.coalBlack, e.finpowdown());
            Fill.circle(e.x + x, e.y + y, out * 2f + 0.35f);
        });
    }),

    formsmoke = new Effect(40, e -> {
        randLenVectors(e.id, 6, 5f + e.fin() * 8f, (x, y) -> {
            color(Pal.plasticSmoke, Color.lightGray, e.fin());
            Fill.square(e.x + x, e.y + y, 0.2f + e.fout() * 2f, 45);
        });
    }),

    blastsmoke = new Effect(26, e -> {
        randLenVectors(e.id, 12, 1f + e.fin() * 23f, (x, y) -> {
            float size = 2f + e.fout() * 6f;
            color(Color.lightGray, Color.darkGray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    lava = new Effect(18, e -> {
        randLenVectors(e.id, 3, 1f + e.fin() * 10f, (x, y) -> {
            float size = e.fslope() * 4f;
            color(Color.orange, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    dooropen = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, e.rotation * tilesize / 2f + e.fin() * 2f);
    }),

    doorclose = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, e.rotation * tilesize / 2f + e.fout() * 2f);
    }),

    dooropenlarge = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
    }),

    doorcloselarge = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
    }),

    generate = new Effect(11, e -> {
        color(Color.orange, Color.yellow, e.fin());
        stroke(1f);
        Lines.spikes(e.x, e.y, e.fin() * 5f, 2, 8);
    }),

    mineWallSmall = new Effect(50, e -> {
        color(e.color, Color.darkGray, e.fin());
        randLenVectors(e.id, 2, e.fin() * 6f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() + 0.5f);
        });
    }),

    mineSmall = new Effect(30, e -> {
        color(e.color, Color.lightGray, e.fin());
        randLenVectors(e.id, 3, e.fin() * 5f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() + 0.5f, 45);
        });
    }),

    mine = new Effect(20, e -> {
        color(e.color, Color.lightGray, e.fin());
        randLenVectors(e.id, 6, 3f + e.fin() * 6f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2f, 45);
        });
    }),

    mineBig = new Effect(30, e -> {
        color(e.color, Color.lightGray, e.fin());
        randLenVectors(e.id, 6, 4f + e.fin() * 8f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.2f, 45);
        });
    }),

    mineHuge = new Effect(40, e -> {
        color(e.color, Color.lightGray, e.fin());
        randLenVectors(e.id, 8, 5f + e.fin() * 10f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
        });
    }),

    mineImpact = new Effect(90, e -> {
        color(e.color, Color.lightGray, e.fin());
        randLenVectors(e.id, 12, 5f + e.finpow() * 22f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2.5f + 0.5f, 45);
        });
    }),

    mineImpactWave = new Effect(50f, e -> {
        color(e.color);

        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 12, 4f + e.finpow() * e.rotation, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 5 + 1f);
        });

        e.scaled(30f, b -> {
            Lines.stroke(5f * b.fout());
            Lines.circle(e.x, e.y, b.finpow() * 28f);
        });
    }),

    payloadReceive = new Effect(30, e -> {
        color(Color.white, Pal.accent, e.fin());
        randLenVectors(e.id, 12, 7f + e.fin() * 13f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2.1f + 0.5f, 45);
        });
    }),

    teleportActivate = new Effect(50, e -> {
        color(e.color);

        e.scaled(8f, e2 -> {
            stroke(e2.fout() * 4f);
            Lines.circle(e2.x, e2.y, 4f + e2.fin() * 27f);
        });

        stroke(e.fout() * 2f);

        randLenVectors(e.id, 30, 4f + 40f * e.fin(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 4f + 1f);
        });
    }),

    teleport = new Effect(60, e -> {
        color(e.color);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, 7f + e.fout() * 8f);

        randLenVectors(e.id, 20, 6f + 20f * e.fout(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 4f + 1f);
        });
    }),

    teleportOut = new Effect(20, e -> {
        color(e.color);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 7f + e.fin() * 8f);

        randLenVectors(e.id, 20, 4f + 20f * e.fin(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 4f + 1f);
        });
    }),

    ripple = new Effect(30, e -> {
        e.lifetime = 30f*e.rotation;

        color(Tmp.c1.set(e.color).mul(1.5f));
        stroke(e.fout() * 1.4f);
        Lines.circle(e.x, e.y, (2f + e.fin() * 4f) * e.rotation);
    }).layer(Layer.debris),

    bubble = new Effect(20, e -> {
        color(Tmp.c1.set(e.color).shiftValue(0.1f));
        stroke(e.fout() + 0.2f);
        randLenVectors(e.id, 2, e.rotation * 0.9f, (x, y) -> {
            Lines.circle(e.x + x, e.y + y, 1f + e.fin() * 3f);
        });
    }),

    launch = new Effect(28, e -> {
        color(Pal.command);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 120f);
    }),

    launchPod = new Effect(50, e -> {
        color(Pal.engine);

        e.scaled(25f, f -> {
            stroke(f.fout() * 2f);
            Lines.circle(e.x, e.y, 4f + f.finpow() * 30f);
        });

        stroke(e.fout() * 2f);

        randLenVectors(e.id, 24, e.finpow() * 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    }),

    healWaveMend = new Effect(40, e -> {
        color(e.color);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, e.finpow() * e.rotation);
    }),

    overdriveWave = new Effect(50, e -> {
        color(e.color);
        stroke(e.fout());
        Lines.circle(e.x, e.y, e.finpow() * e.rotation);
    }),

    healBlock = new Effect(20, e -> {
        color(Pal.heal);
        stroke(2f * e.fout() + 0.5f);
        Lines.square(e.x, e.y, 1f + (e.fin() * e.rotation * tilesize / 2f - 1f));
    }),

    healBlockFull = new Effect(20, e -> {
        if(!(e.data instanceof Block block)) return;

        mixcol(e.color, 1f);
        alpha(e.fout());
        Draw.rect(block.fullIcon, e.x, e.y);
    }),

    rotateBlock = new Effect(30, e -> {
        color(Pal.accent);
        alpha(e.fout() * 1);
        Fill.square(e.x, e.y, e.rotation * tilesize / 2f);
    }),

    lightBlock = new Effect(60, e -> {
        color(e.color);
        alpha(e.fout() * 1);
        Fill.square(e.x, e.y, e.rotation * tilesize / 2f);
    }),

    overdriveBlockFull = new Effect(60, e -> {
        color(e.color);
        alpha(e.fslope() * 0.4f);
        Fill.square(e.x, e.y, e.rotation * tilesize);
    }),

    shieldBreak = new Effect(40, e -> {
        color(e.color);
        stroke(3f * e.fout());
        Lines.poly(e.x, e.y, 6, e.rotation + e.fin());
    }).followParent(true),

    coreLandDust = new Effect(100f, e -> {
        color(e.color, e.fout(0.1f));
        rand.setSeed(e.id);
        Tmp.v1.trns(e.rotation, e.finpow() * 90f * rand.random(0.2f, 1f));
        Fill.circle(e.x + Tmp.v1.x, e.y + Tmp.v1.y, 8f * rand.random(0.6f, 1f) * e.fout(0.2f));
    }).layer(Layer.groundUnit + 1f),

    unitShieldBreak = new Effect(35, e -> {
        if(!(e.data instanceof Unit unit)) return;

        float radius = unit.hitSize() * 1.3f;

        e.scaled(16f, c -> {
            color(e.color, 0.9f);
            stroke(c.fout() * 2f + 0.1f);

            randLenVectors(e.id, (int)(radius * 1.2f), radius/2f + c.finpow() * radius*1.25f, (x, y) -> {
                lineAngle(c.x + x, c.y + y, Mathf.angle(x, y), c.fout() * 5 + 1f);
            });
        });

        color(e.color, e.fout() * 0.9f);
        stroke(e.fout());
        Lines.circle(e.x, e.y, radius);
    }),

    chainLightning = new Effect(20f, 300f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p).sub(e.x, e.y).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y;
        float range = 6f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;

        Lines.stroke(2.5f * e.fout());
        Draw.color(Color.white, e.color, e.fin());

        Lines.beginLine();

        Lines.linePoint(e.x, e.y);

        rand.setSeed(e.id);

        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(rand).scl(range/2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }

            Lines.linePoint(nx, ny);
        }

        Lines.endLine();
    }).followParent(false).rotWithParent(false),

    chainEmp = new Effect(30f, 300f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p).sub(e.x, e.y).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y;
        float range = 6f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;

        Lines.stroke(4f * e.fout());
        Draw.color(Color.white, e.color, e.fin());

        Lines.beginLine();

        Lines.linePoint(e.x, e.y);

        rand.setSeed(e.id);

        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(rand).scl(range/2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }

            Lines.linePoint(nx, ny);
        }

        Lines.endLine();
    }).followParent(false).rotWithParent(false),

    legDestroy = new Effect(90f, 100f, e -> {
        if(!(e.data instanceof LegDestroyData data)) return;
        rand.setSeed(e.id);

        e.lifetime = rand.random(70f, 130f);

        Tmp.v1.trns(rand.random(360f), rand.random(data.region.width / 8f) * e.finpow());
        float ox = Tmp.v1.x, oy = Tmp.v1.y;

        alpha(e.foutpowdown());

        stroke(data.region.height * scl);
        line(data.region, data.a.x + ox, data.a.y + oy, data.b.x + ox, data.b.y + oy, false);
    }).layer(Layer.groundUnit + 5f);
}
