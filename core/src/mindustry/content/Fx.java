package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static arc.graphics.g2d.Draw.rect;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.tilesize;

public class Fx{
    public static final Effect

    none = new Effect(0, 0f, e -> {}),

    unitSpawn = new Effect(30f, e -> {
        if(!(e.data instanceof Unit)) return;

        alpha(e.fin());

        float scl = 1f + e.fout() * 2f;

        Unit unit = e.data();
        rect(unit.type().region, e.x, e.y,
        unit.type().region.getWidth() * Draw.scl * scl, unit.type().region.getHeight() * Draw.scl * scl, 180f);

    }),

    unitCapKill = new Effect(80f, e -> {
        color(Color.scarlet);
        alpha(e.fout(Interp.pow4Out));

        float size = 10f + e.fout(Interp.pow10In) * 25f;
        Draw.rect(Icon.warning.getRegion(), e.x, e.y, size, size);
    }),

    unitControl = new Effect(30f, e -> {
        if(!(e.data instanceof Unit)) return;

        Unit select = e.data();

        mixcol(Pal.accent, 1f);
        alpha(e.fout());
        rect(select.type().icon(Cicon.full), select.x, select.y, select.rotation - 90f);
        alpha(1f);
        Lines.stroke(e.fslope() * 1f);
        Lines.square(select.x, select.y, e.fout() * select.hitSize * 2f, 45);
        Lines.stroke(e.fslope() * 2f);
        Lines.square(select.x, select.y, e.fout() * select.hitSize * 3f, 45f);
        reset();
    }),

    unitDespawn = new Effect(100f, e -> {
        if(!(e.data instanceof Unit) || e.<Unit>data().type() == null) return;

        Unit select = e.data();
        float scl = e.fout(Interp.pow2Out);
        float p = Draw.scl;
        Draw.scl *= scl;

        mixcol(Pal.accent, 1f);
        rect(select.type().icon(Cicon.full), select.x, select.y, select.rotation - 90f);
        reset();

        Draw.scl = p;
    }),

    unitSpirit = new Effect(17f, e -> {
        if(!(e.data instanceof Position)) return;
        Position to = e.data();

        color(Pal.accent);

        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow2In);
        float x = Tmp.v1.x, y = Tmp.v1.y;
        float size = 2.5f * e.fin();

        Fill.square(x, y, 1.5f * size, 45f);

        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow5In);
        x = Tmp.v1.x;
        y = Tmp.v1.y;

        Fill.square(x, y, 1f * size, 45f);
    }),

    itemTransfer = new Effect(12f, e -> {
        if(!(e.data instanceof Position)) return;
        Position to = e.data();
        Tmp.v1.set(e.x, e.y).interpolate(Tmp.v2.set(to), e.fin(), Interp.pow3)
        .add(Tmp.v2.sub(e.x, e.y).nor().rotate90(1).scl(Mathf.randomSeedRange(e.id, 1f) * e.fslope() * 10f));
        float x = Tmp.v1.x, y = Tmp.v1.y;
        float size = Math.min(0.8f + e.rotation / 5f, 2);

        stroke(e.fslope() * 2f * size, Pal.accent);
        Lines.circle(x, y, e.fslope() * 2f * size);

        color(e.color);
        Fill.circle(x, y, e.fslope() * 1.5f * size);
    }),

    pointBeam = new Effect(25f, e -> {
        if(!(e.data instanceof Position)) return;

        Position pos = e.data();

        Draw.color(e.color);
        Draw.alpha(e.fout());
        Lines.stroke(1.5f);
        Lines.line(e.x, e.y, pos.getX(), pos.getY());
    }),

    pointHit = new Effect(8f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 1f + 0.2f);
        Lines.circle(e.x, e.y, e.fin() * 6f);
    }),

    lightning = new Effect(10f, 500f, e -> {
        if(!(e.data instanceof Seq)) return;
        Seq<Vec2> lines = e.data();

        stroke(3f * e.fout());
        color(e.color, Color.white, e.fin());

        beginLine();
        lines.each(Lines::linePoint);
        linePoint(e.x, e.y);
        endLine();

        int i = 0;
        for(Vec2 p : lines){
            Fill.square(p.x, p.y, (5f - (float)i++ / lines.size * 2f) * e.fout(), 45);
        }
    }),

    commandSend = new Effect(28, e -> {
        color(Pal.command);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 120f);
    }),

    upgradeCore = new Effect(120f, e -> {
        color(Color.white, Pal.accent, e.fin());
        alpha(e.fout());
        Fill.square(e.x, e.y, tilesize / 2f * e.rotation);
    }),

    placeBlock = new Effect(16, e -> {
        color(Pal.accent);
        stroke(3f - e.fin() * 2f);
        Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);
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
        if(!(e.data instanceof TextureRegion)) return;

        Draw.mixcol(Pal.rubble, 1f);

        TextureRegion reg = e.data();
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

    padlaunch = new Effect(10, e -> {
        stroke(4f * e.fout());
        color(Pal.accent);
        Lines.poly(e.x, e.y, 4, 5f + e.fin() * 60f);
    }),

    vtolHover = new Effect(40f, e -> {
        float len = e.finpow() * 10f;
        float ang = e.rotation + Mathf.randomSeedRange(e.id, 30f);
        color(Pal.lightFlame, Pal.lightOrange, e.fin());
        Fill.circle(e.x + trnsx(ang, len), e.y + trnsy(ang, len), 2f * e.fout());
    }),

    unitDrop = new Effect(30, e -> {
        color(Pal.lightishGray);
        randLenVectors(e.id, 9, 3 + 20f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.4f);
        });
    }).ground(),

    unitLand = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.1f));
        randLenVectors(e.id, 6, 17f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 4f + 0.3f);
        });
    }).ground(),

    unitLandSmall = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.1f));
        randLenVectors(e.id, (int)(6 * e.rotation), 12f * e.finpow() * e.rotation, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.1f);
        });
    }).ground(),

    unitPickup = new Effect(18, e -> {
        color(Pal.lightishGray);
        stroke(e.fin() * 2f);
        Lines.poly(e.x, e.y, 4, 13f * e.fout());
    }).ground(),

    landShock = new Effect(12, e -> {
        color(Pal.lancerLaser);
        stroke(e.fout() * 3f);
        Lines.poly(e.x, e.y, 12, 20f * e.fout());
    }).ground(),

    pickup = new Effect(18, e -> {
        color(Pal.lightishGray);
        stroke(e.fout() * 2f);
        Lines.spikes(e.x, e.y, 1f + e.fin() * 6f, e.fout() * 4f, 6);
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
        color(Pal.shield);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 60f);
    }),

    shieldApply = new Effect(11, e -> {
        color(Pal.shield);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 2f + e.finpow() * 7f);
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

        randLenVectors(e.id, 2, e.fin() * 15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });

    }),

    hitLiquid = new Effect(16, e -> {
        color(e.color);

        randLenVectors(e.id, 5, e.fin() * 15f, e.rotation + 180f, 60f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
        });

    }),

    hitLancer = new Effect(12, e -> {
        color(Color.white);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 17f, e.rotation, 360f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });

    }),

    hitMeltdown = new Effect(12, e -> {
        color(Pal.meltdownHit);
        stroke(e.fout() * 2f);

        randLenVectors(e.id, 6, e.finpow() * 18f, e.rotation, 360f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });

    }),

    hitLaser = new Effect(8, e -> {
        color(Color.white, Pal.heal, e.fin());
        stroke(0.5f + e.fout());
        Lines.circle(e.x, e.y, e.fin() * 5f);
    }),

    hitYellowLaser = new Effect(8, e -> {
        color(Color.white, Pal.lightTrail, e.fin());
        stroke(0.5f + e.fout());
        Lines.circle(e.x, e.y, e.fin() * 5f);
    }),

    despawn = new Effect(12, e -> {
        color(Pal.lighterOrange, Color.gray, e.fin());
        stroke(e.fout());

        randLenVectors(e.id, 7, e.fin() * 7f, e.rotation, 40f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 2 + 1f);
        });

    }),

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
        stroke(1f * e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

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
        stroke(1f * e.fout());

        randLenVectors(e.id + 1, 4, 1f + 25f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

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
        stroke(1f * e.fout());

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
        stroke(1f * e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

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
        stroke(1f * e.fout());

        randLenVectors(e.id + 1, 6, 1f + 29f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 4f);
        });

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
    }),

    absorb = new Effect(12, e -> {
        color(Pal.accent);
        stroke(2f * e.fout());
        Lines.circle(e.x, e.y, 5f * e.fout());
    }),

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
        stroke(1f * e.fout());

        randLenVectors(e.id + 1, 4, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

    }),

    burning = new Effect(35f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
        });

    }),

    fire = new Effect(50f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, e.fin());

        randLenVectors(e.id, 2, 2f + e.fin() * 9f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
        });

        color();

        Drawf.light(Team.derelict, e.x, e.y, 20f * e.fslope(), Pal.lightFlame, 0.5f);
    }),

    fireSmoke = new Effect(35f, e -> {
        color(Color.gray);

        randLenVectors(e.id, 1, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
        });

    }),

    steam = new Effect(35f, e -> {
        color(Color.lightGray);

        randLenVectors(e.id, 2, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.2f + e.fslope() * 1.5f);
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

    wet = new Effect(40f, e -> {
        color(Liquids.water.color);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
        });

    }),

    sapped = new Effect(40f, e -> {
        color(Pal.sap);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fslope() * 1.1f, 45f);
        });

    }),

    oily = new Effect(42f, e -> {
        color(Liquids.oil.color);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
        });

    }),

    overdriven = new Effect(20f, e -> {
        color(Pal.accent);

        randLenVectors(e.id, 2, 1f + e.fin() * 2f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2.3f + 0.5f);
        });

    }),

    overclocked = new Effect(50f, e -> {
        color(Pal.accent);

        Fill.square(e.x, e.y, e.fslope() * 2f, 45f);
    }),

    dropItem = new Effect(20f, e -> {
        float length = 20f * e.finpow();
        float size = 7f * e.fout();

        rect(((Item)e.data).icon(Cicon.medium), e.x + trnsx(e.rotation, length), e.y + trnsy(e.rotation, length), size, size);
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

    nuclearShockwave = new Effect(10f, 200f, e -> {
        color(Color.white, Color.lightGray, e.fin());
        stroke(e.fout() * 3f + 0.2f);
        Lines.circle(e.x, e.y, e.fin() * 140f);
    }),

    impactShockwave = new Effect(13f, 300f, e -> {
        color(Pal.lighterOrange, Color.lightGray, e.fin());
        stroke(e.fout() * 4f + 0.2f);
        Lines.circle(e.x, e.y, e.fin() * 200f);
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
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
        });

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        stroke(1.5f * e.fout());

        randLenVectors(e.id + 1, 8, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });
    }),

    dynamicExplosion = new Effect(30, e -> {
        float intensity = e.rotation;

        e.scaled(5 + intensity * 2, i -> {
            stroke(3.1f * i.fout());
            Lines.circle(e.x, e.y, (3f + i.fin() * 14f) * intensity);
        });

        color(Color.gray);

        randLenVectors(e.id, e.finpow(), (int)(6 * intensity), 21f * intensity, (x, y, in, out) -> {
            Fill.circle(e.x + x, e.y + y, out * (2f + intensity) * 3 + 0.5f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, out * (intensity) * 3);
        });

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        stroke((1.7f * e.fout()) * (1f + (intensity - 1f) / 2f));

        randLenVectors(e.id + 1, e.finpow(), (int)(9 * intensity), 40f * intensity, (x, y, in, out) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (3f + intensity));
        });
    }),

    blockExplosion = new Effect(30, e -> {
        e.scaled(7, i -> {
            stroke(3.1f * i.fout());
            Lines.circle(e.x, e.y, 3f + i.fin() * 14f);
        });

        color(Color.gray);

        randLenVectors(e.id, 6, 2f + 19f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f + 0.5f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
        });

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        stroke(1.7f * e.fout());

        randLenVectors(e.id + 1, 9, 1f + 23f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f);
        });

    }),

    blockExplosionSmoke = new Effect(30, e -> {
        color(Color.gray);

        randLenVectors(e.id, 6, 4f + 30f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 1f);
        });

    }),

    shootSmall = new Effect(8, e -> {
        color(Pal.lighterOrange, Pal.lightOrange, e.fin());
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

    shootSmallFlame = new Effect(32f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, Color.gray, e.fin());

        randLenVectors(e.id, 8, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
        });

    }),

    shootPyraFlame = new Effect(33f, e -> {
        color(Pal.lightPyraFlame, Pal.darkPyraFlame, Color.gray, e.fin());

        randLenVectors(e.id, 10, e.finpow() * 70f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.6f);
        });

    }),

    shootLiquid = new Effect(40f, e -> {
        color(e.color, Color.white, e.fout() / 6f + Mathf.randomSeedRange(e.id, 0.1f));

        randLenVectors(e.id, 6, e.finpow() * 60f, e.rotation, 11f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.5f + e.fout() * 2.5f);
        });

    }),

    shellEjectSmall = new Effect(30f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        float rot = Math.abs(e.rotation) + 90f;

        int i = Mathf.sign(e.rotation);

        float len = (2f + e.finpow() * 6f) * i;
        float lr = rot + e.fin() * 30f * i;
        Fill.rect(e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
        e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
        1f, 2f, rot + e.fin() * 50f * i);

    }).ground(400f),

    shellEjectMedium = new Effect(34f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (2f + e.finpow() * 10f) * i;
            float lr = rot + e.fin() * 20f * i;
            rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2f, 3f, rot);
        }

        color(Color.lightGray, Color.gray, e.fin());

        for(int i : Mathf.signs){
            float ex = e.x, ey = e.y, fout = e.fout();
            randLenVectors(e.id, 4, 1f + e.finpow() * 11f, e.rotation + 90f * i, 20f, (x, y) -> {
                Fill.circle(ex + x, ey + y, fout * 1.5f);
            });
        }

    }).ground(400f),

    shellEjectBig = new Effect(22f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        float rot = e.rotation + 90f;
        for(int i : Mathf.signs){
            float len = (4f + e.finpow() * 8f) * i;
            float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;
            rect(Core.atlas.find("casing"),
            e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
            e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
            2.5f, 4f,
            rot + e.fin() * 30f * i + Mathf.randomSeedRange(e.id + i + 9, 40f * e.fin()));
        }

        color(Color.lightGray);

        for(int i : Mathf.signs){
            float ex = e.x, ey = e.y, fout = e.fout();
            randLenVectors(e.id, 4, -e.finpow() * 15f, e.rotation + 90f * i, 25f, (x, y) -> {
                Fill.circle(ex + x, ey + y, fout * 2f);
            });
        }

    }).ground(400f),

    lancerLaserShoot = new Effect(21f, e -> {
        color(Pal.lancerLaser);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i);
        }

    }),

    lancerLaserShootSmoke = new Effect(26f, e -> {
        color(Color.white);
        float length = e.data == null ? 70f : (Float)e.data;

        randLenVectors(e.id, 7, length, e.rotation, 0f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 9f);
        });

    }),

    lancerLaserCharge = new Effect(38f, e -> {
        color(Pal.lancerLaser);

        randLenVectors(e.id, 2, 1f + 20f * e.fout(), e.rotation, 120f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 3f + 1f);
        });

    }),

    lancerLaserChargeBegin = new Effect(60f, e -> {
        color(Pal.lancerLaser);
        Fill.circle(e.x, e.y, e.fin() * 3f);

        color();
        Fill.circle(e.x, e.y, e.fin() * 2f);
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

    reactorsmoke = new Effect(17, e -> {
        randLenVectors(e.id, 4, e.fin() * 8f, (x, y) -> {
            float size = 1f + e.fout() * 5f;
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    nuclearsmoke = new Effect(40, e -> {
        randLenVectors(e.id, 4, e.fin() * 13f, (x, y) -> {
            float size = e.fslope() * 4f;
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    nuclearcloud = new Effect(90, 200f, e -> {
        randLenVectors(e.id, 10, e.finpow() * 90f, (x, y) -> {
            float size = e.fout() * 14f;
            color(Color.lime, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    impactsmoke = new Effect(60, e -> {
        randLenVectors(e.id, 7, e.fin() * 20f, (x, y) -> {
            float size = e.fslope() * 4f;
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    impactcloud = new Effect(140, 400f, e -> {
        randLenVectors(e.id, 20, e.finpow() * 160f, (x, y) -> {
            float size = e.fout() * 15f;
            color(Pal.lighterOrange, Color.lightGray, e.fin());
            Fill.circle(e.x + x, e.y + y, size/2f);
        });
    }),

    redgeneratespark = new Effect(18, e -> {
        randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
            float len = e.fout() * 4f;
            color(Pal.redSpark, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, len/2f);
        });
    }),

    generatespark = new Effect(18, e -> {
        randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
            float len = e.fout() * 4f;
            color(Pal.orangeSpark, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, len/2f);
        });
    }),

    fuelburn = new Effect(23, e -> {
        randLenVectors(e.id, 5, e.fin() * 9f, (x, y) -> {
            float len = e.fout() * 4f;
            color(Color.lightGray, Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, len/2f);
        });
    }),

    plasticburn = new Effect(40, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 5f, (x, y) -> {
            color(Color.valueOf("e9ead3"), Color.gray, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
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

    pulverizeRedder = new Effect(40, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 9f, (x, y) -> {
            color(Pal.redderDust, Pal.stoneGray, e.fin());
            Fill.square(e.x + x, e.y + y, e.fout() * 2.5f + 0.5f, 45);
        });
    }),
    
    pulverizeSmall = new Effect(30, e -> {
        randLenVectors(e.id, 3, e.fin() * 5f, (x, y) -> {
            color(Pal.stoneGray);
            Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
        });
    }),
    
    pulverizeMedium = new Effect(30, e -> {
        randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
            color(Pal.stoneGray);
            Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
        });
    }),
    
    producesmoke = new Effect(12, e -> {
        randLenVectors(e.id, 8, 4f + e.fin() * 18f, (x, y) -> {
            color(Color.white, Pal.accent, e.fin());
            Fill.square(e.x + x, e.y + y, 1f + e.fout() * 3f, 45);
        });
    }),
    
    smeltsmoke = new Effect(15, e -> {
        randLenVectors(e.id, 6, 4f + e.fin() * 5f, (x, y) -> {
            color(Color.white, e.color, e.fin());
            Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
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
        Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 2f);
    }),
    
    doorclose = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 2f);
    }),
    dooropenlarge = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
    }),
    doorcloselarge = new Effect(10, e -> {
        stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
    }),
    purify = new Effect(10, e -> {
        color(Color.royal, Color.gray, e.fin());
        stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
    }),
    purifyoil = new Effect(10, e -> {
        color(Color.black, Color.gray, e.fin());
        stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
    }),
    purifystone = new Effect(10, e -> {
        color(Color.orange, Color.gray, e.fin());
        stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
    }),
    generate = new Effect(11, e -> {
        color(Color.orange, Color.yellow, e.fin());
        stroke(1f);
        Lines.spikes(e.x, e.y, e.fin() * 5f, 2, 8);
    }),
    mine = new Effect(20, e -> {
        randLenVectors(e.id, 6, 3f + e.fin() * 6f, (x, y) -> {
            color(e.color, Color.lightGray, e.fin());
            Fill.square(e.x + x, e.y + y, e.fout() * 2f, 45);
        });
    }),
    mineBig = new Effect(30, e -> {
        randLenVectors(e.id, 6, 4f + e.fin() * 8f, (x, y) -> {
            color(e.color, Color.lightGray, e.fin());
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.2f, 45);
        });
    }),

    mineHuge = new Effect(40, e -> {
        randLenVectors(e.id, 8, 5f + e.fin() * 10f, (x, y) -> {
            color(e.color, Color.lightGray, e.fin());
            Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
        });
    }),
    smelt = new Effect(20, e -> {
        randLenVectors(e.id, 6, 2f + e.fin() * 5f, (x, y) -> {
            color(Color.white, e.color, e.fin());
            Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
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

    //TODO fix false in constructor
    ripple = new Effect(30, e -> {
        color(Tmp.c1.set(e.color).mul(1.5f));
        stroke(e.fout() + 0.4f);
        Lines.circle(e.x, e.y, (2f + e.fin() * 4f) * e.rotation);
    }).ground(),

    bubble = new Effect(20, e -> {
        color(Tmp.c1.set(e.color).shiftValue(0.1f));
        stroke(e.fout() + 0.2f);
        randLenVectors(e.id, 2, 8f, (x, y) -> {
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
        stroke(e.fout() * 1f);
        Lines.circle(e.x, e.y, e.finpow() * e.rotation);
    }),

    healBlock = new Effect(20, e -> {
        color(Pal.heal);
        stroke(2f * e.fout() + 0.5f);
        Lines.square(e.x, e.y, 1f + (e.fin() * e.rotation * tilesize / 2f - 1f));
    }),

    healBlockFull = new Effect(20, e -> {
        color(e.color);
        alpha(e.fout());
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
    }),

    unitShieldBreak = new Effect(35, e -> {
        if(!(e.data instanceof Unitc)) return;

        Unit unit = e.data();

        float radius = unit.hitSize() * 1.3f;


        e.scaled(16f, c -> {
            color(Pal.shield);
            stroke(c.fout() * 2f + 0.1f);

            randLenVectors(e.id, (int)(radius * 1.2f), radius/2f + c.finpow() * radius*1.25f, (x, y) -> {
                lineAngle(c.x + x, c.y + y, Mathf.angle(x, y), c.fout() * 5 + 1f);
            });
        });

        color(Pal.shield, e.fout());
        stroke(1f * e.fout());
        Lines.circle(e.x, e.y, radius);
    }),

    coreLand = new Effect(120f, e -> {
    });
}
