package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RepairTurret extends Block{
    static final Rect rect = new Rect();
    static final Rand rand = new Rand();

    public int timerTarget = timers++;
    public int timerEffect = timers++;

    public float repairRadius = 50f;
    public float repairSpeed = 0.3f;
    public float powerUse;
    public float length = 5f;
    public float beamWidth = 1f;
    public float pulseRadius = 6f;
    public float pulseStroke = 2f;
    public boolean acceptCoolant = false;

    public float coolantUse = 0.5f;
    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;
    /** How much healing is increased by with heat capacity. */
    public float coolantMultiplier = 1f;

    public @Load(value = "@-base", fallback = "block-@size") TextureRegion baseRegion;
    public @Load("laser-white") TextureRegion laser;
    public @Load("laser-white-end") TextureRegion laserEnd;
    public @Load("laser-top") TextureRegion laserTop;
    public @Load("laser-top-end") TextureRegion laserTopEnd;

    public Color laserColor = Color.valueOf("98ffa9"), laserTopColor = Color.white.cpy();

    public RepairTurret(String name){
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.repair);
        hasPower = true;
        outlineIcon = true;
        //yeah, this isn't the same thing, but it's close enough
        group = BlockGroup.projectors;

        envEnabled |= Env.space;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, repairRadius / tilesize, StatUnit.blocks);
        stats.add(Stat.repairSpeed, repairSpeed * 60f, StatUnit.perSecond);

        if(acceptCoolant){
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.speedBoosters(Core.bundle.get("bar.strength"), coolantUse, coolantMultiplier, true, this::consumesLiquid));
        }
    }

    @Override
    public void init(){
        if(acceptCoolant){
            hasLiquids = true;
            consume(new ConsumeCoolant(coolantUse)).optional(true, true);
        }

        consumePowerCond(powerUse, (RepairPointBuild entity) -> entity.target != null);
        updateClipRadius(repairRadius);
        super.init();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, repairRadius, Pal.accent);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    public static void drawBeam(float x, float y, float rotation, float length, int id, @Nullable Sized target, Team team,
                                float strength, float pulseStroke, float pulseRadius, float beamWidth,
                                Vec2 lastEnd, Vec2 offset,
                                Color laserColor, Color laserTopColor,
                                TextureRegion laser, TextureRegion laserEnd, TextureRegion laserTop, TextureRegion laserTopEnd){
        rand.setSeed(id + (target instanceof Entityc e ? e.id() : 0));

        if(target != null){
            float
            originX = x + Angles.trnsx(rotation, length),
            originY = y + Angles.trnsy(rotation, length);

            lastEnd.set(target).sub(originX, originY);
            lastEnd.setLength(Math.max(2f, lastEnd.len()));

            lastEnd.add(offset.trns(
            rand.random(360f) + Time.time/2f,
            Mathf.sin(Time.time + rand.random(200f), 55f, rand.random(target.hitSize() * 0.2f, target.hitSize() * 0.45f))
            ).rotate(target instanceof Rotc rot ? rot.rotation() : 0f));

            lastEnd.add(originX, originY);
        }

        if(strength > 0.01f){
            float
            originX = x + Angles.trnsx(rotation, length),
            originY = y + Angles.trnsy(rotation, length);

            Draw.z(Layer.flyingUnit + 1); //above all units

            Draw.color(laserColor);

            float f = (Time.time / 85f + rand.random(1f)) % 1f;

            Draw.alpha(1f - Interp.pow5In.apply(f));
            Lines.stroke(strength * pulseStroke);
            Lines.circle(lastEnd.x, lastEnd.y, 1f + f * pulseRadius);

            Draw.color(laserColor);
            Drawf.laser(laser, laserEnd, originX, originY, lastEnd.x, lastEnd.y, strength * beamWidth);
            Draw.z(Layer.flyingUnit + 1.1f);
            Draw.color(laserTopColor);
            Drawf.laser(laserTop, laserTopEnd, originX, originY, lastEnd.x, lastEnd.y, strength * beamWidth);
            Draw.color();
        }
    }

    public class RepairPointBuild extends Building implements Ranged{
        public Unit target;
        public Vec2 offset = new Vec2(), lastEnd = new Vec2();
        public float strength, rotation = 90;

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);

            Draw.z(Layer.turret);
            Drawf.shadow(region, x - (size / 2f), y - (size / 2f), rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            drawBeam(x, y, rotation, length, id, target, team, strength,
                pulseStroke, pulseRadius, beamWidth, lastEnd, offset, laserColor, laserTopColor,
                laser, laserEnd, laserTop, laserTopEnd);
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, repairRadius, Pal.accent);
        }

        @Override
        public void updateTile(){
            float multiplier = 1f;
            if(acceptCoolant){
                multiplier = 1f + liquids.current().heatCapacity * coolantMultiplier * optionalEfficiency;
            }

            if(target != null && (target.dead() || target.dst(this) - target.hitSize/2f > repairRadius || target.health() >= target.maxHealth())){
                target = null;
            }

            if(target == null){
                offset.setZero();
            }

            boolean healed = false;

            if(target != null && efficiency > 0){
                float angle = Angles.angle(x, y, target.x + offset.x, target.y + offset.y);
                if(Angles.angleDist(angle, rotation) < 30f){
                    healed = true;
                    target.heal(repairSpeed * strength * edelta() * multiplier);
                }
                rotation = Mathf.slerpDelta(rotation, angle, 0.5f * efficiency * timeScale);
            }

            strength = Mathf.lerpDelta(strength, healed ? 1f : 0f, 0.08f * Time.delta);

            if(timer(timerTarget, 20)){
                rect.setSize(repairRadius * 2).setCenter(x, y);
                target = Units.closest(team, x, y, repairRadius, Unit::damaged);
            }
        }

        @Override
        public boolean shouldConsume(){
            return target != null && enabled;
        }

        @Override
        public BlockStatus status(){
            return Mathf.equal(potentialEfficiency, 0f, 0.01f) ? BlockStatus.noInput : super.status();
        }

        @Override
        public float range(){
            return repairRadius;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                rotation = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
