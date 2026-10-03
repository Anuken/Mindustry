package mindustry.entities.abilities;

import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public abstract class BaseShieldAbility extends Ability implements UnitShieldProvider{
    /** Shield radius. */
    public float radius = 60f;
    /** Offset parameters for shield. */
    public float angleOffset = 0f, x = 0f, y = 0f;
    /** Shield regen speed in damage/tick. */
    public float regen = 0.1f;
    /** Maximum shield. */
    public float max = 200f;
    /** Cooldown after the shield is broken, in ticks. */
    public float cooldown = 60f * 5;
    /** If true, only activates when shooting. */
    public boolean whenShooting = false;
    /** Whether the shield absorbs direct damage to the unit or not. See {@link ShieldRegenFieldAbility} on unit shielding. */
    public boolean unitShield = true;

    /** Multiplier on unit speed when its shield is hit. */
    public float unitSlowdown = -1f;
    /** Number of ticks unit slowdown is applied after being hit. */
    public float slowdownTime = 80f;
    /** Number of hits required to reach maximum unit slowdown. */
    public int shotThreshold = 5;

    /** Bullet/target reflection chance. <= 0 to disable. */
    public float chanceReflect = -1f;
    /** Whether to reflect missile units instead of absorbing them. */
    public boolean reflectMissiles = true;
    /** Multiplier for shield damage taken by missile units. <= 0 to disable missile mechanics entirely. */
    public float missileUnitMultiplier = -1f;
    /** Multiplier for reflected bullet/target building damage. <= 0 to disable. */
    public float reflectBuildingDamage = 1f;
    /** Velocity multiplier for reflected bullets/targets on the opposite axis. Negative values = concave, positive values = convex. */
    public float reflectVel = 1f;
    /** Time multiplier for reflected targets. 1 = full distance, 0 = no distance. */
    public float reflectBulletTime = 0.5f, reflectMissileTime = 0.5f;
    /** Reflection sound. */
    public Sound reflectSound = Sounds.none;
    /** Reflection effect on shield. */
    public Effect reflectEffect = Fx.shieldReflect;
    /** Effect shown on reflected unit missiles. */
    public Effect reflectUnitEffect = Fx.missileReflect;
    /** Minimum target size required for the {@link #reflectEffect} to appear. */
    public float reflectEffectSize = 50f;

    public Sound breakSound = Sounds.shieldBreakSmall;
    public Sound hitSound = Sounds.shieldHit;
    public float hitSoundVolume = 0.12f;

    /** Color override of the shield. Uses unit shield color by default. */
    public @Nullable Color color;
    /** If true, enemy units are pushed out. */
    public boolean pushUnits = false;
    /** If pushUnits is true, allow ground units to push air or air units to push ground */
    public boolean pushDiffLayer = true;
    public Effect pushEffect = Fx.circleColorSpark;
    public Effect breakEffect = Fx.shieldBreak;

    /** State. */
    protected float radiusScale, widthScale, alpha, shots;
    protected boolean wasBroken = true;

    protected static float realRad;
    protected static Unit paramUnit;
    protected static BaseShieldAbility paramField;
    protected static Vec2 paramPos = new Vec2();
    protected static final Cons<Bullet> shieldConsumer = b -> paramField.checkBullets(b);
    protected static final Cons<Unit> unitConsumer = unit -> paramField.checkUnits(unit);
    protected static final Vec2 laserHit = new Vec2();

    public BaseShieldAbility(){
    }

    public float getShield(Unit unit){
        return unitShield ? unit.shield : data;
    }

    public void addShield(Unit unit, float val){
        if(unitShield){
            unit.shield += val;
        }else{
            data += val;
        }
    }

    public float scaledMax(Unit unit){
        return max * Vars.state.rules.unitHealth(unit.team);
    }

    public Color shieldColor(){
        return shieldColor(paramUnit);
    }

    public Color shieldColor(Unit unit){
        return color == null ? unit.type.shieldColor(unit) : color;
    }

    public Vec2 shieldPos(Unit unit, Vec2 out){
        return out.set(x, y).rotate(unit.rotation - 90f).add(unit);
    }

    public boolean active(Unit unit){
        return getShield(unit) > 0f && (unit.isShooting || !whenShooting);
    }

    public abstract boolean isBulletInside(Bullet b);

    public abstract boolean isUnitInside(Unit unit);

    public void checkBullets(Bullet b){
        if(b.team != paramUnit.team && b.type.absorbable && getShield(paramUnit) > 0 && isBulletInside(b)){
            boolean reflected = false;
            if(b.vel.len() >= 0.1f && b.type.reflectable && chanceReflect > 0f){
                reflected = true;
                reflect(b, b.vel, b.type.clipSize() * 4f + b.type.speed * 2f, len -> {
                    b.owner = paramUnit;
                    b.team = paramUnit.team;
                    b.time = b.lifetime * (1 - reflectBulletTime);
                    if(reflectBuildingDamage > 0f) b.buildingDamageMultiplier = reflectBuildingDamage;
                });
            }else{
                b.absorb();
            }
            // shieldDamage for consistency
            absorb(paramUnit, b.x, b.y, b.type.shieldDamage(b), reflected);
        }
    }

    public void checkUnits(Unit unit){
        // ignore core units
        if(getShield(paramUnit) > 0 && unit.targetable(paramUnit.team) && isUnitInside(unit)){
            boolean reflected = false;
            if(unit.isMissile() && missileUnitMultiplier >= 0f){
                if(reflectMissiles){
                    reflected = true;
                    reflect(unit, unit.vel(), unit.type.clipSize, len -> {
                        unit.team(paramUnit.team);
                        unit.rotation(unit.vel().angle());
                        reflectUnitEffect.at(unit.x + Angles.trnsx(unit.rotation, -8f), unit.y,
                        unit.rotation, shieldColor(), unit.type.clipSize); //units are heavy I guess

                        if(unit.controller() instanceof MissileAI ai) ai.shooter = paramUnit;
                        if(unit instanceof TimedKillUnit ut) ut.time = unit.type.lifetime * (1 - reflectMissileTime);
                    });
                }else{
                    Call.unitSafeDeath(unit);
                    pushEffect.at(unit.x, unit.y, shieldColor());
                }
                // consider total missile damage and gamerule to damage the shield
                absorb(paramUnit, unit.x, unit.y, unit.type.damageEstimate * missileUnitMultiplier * Vars.state.rules.unitDamage(unit.team), reflected);

            }else if(pushUnits && (pushDiffLayer || paramUnit.isFlying() == unit.isFlying())){
                float overlapDst = shieldBounds() - unit.dst(paramPos);

                if(overlapDst > 0){
                    //only nullify velocity if it's heading towards the shield
                    if(Angles.angleDist(unit.angleTo(paramPos), unit.vel.angle()) < 90f){
                        unit.vel.setZero();
                    }
                    // get out
                    unit.move(Tmp.v1.set(unit).sub(paramPos).setLength(overlapDst + 0.01f));

                    if(Mathf.chanceDelta(0.3f * Time.delta)){
                        pushEffect.at(unit.x, unit.y, shieldColor());
                    }
                }
            }
        }
    }

    public void checkRadius(){
        //timer2 is used to store radius scale as an effect
        realRad = radiusScale * radius;
    }

    @Override
    public float absorbLaser(Unit unit, float lx, float ly, float damage){
        if(!active(unit)) return 0f;
        return absorb(unit, lx, ly, damage, false);
    }

    public float absorb(Unit unit, float lx, float ly, float damage){
        return absorb(unit, lx, ly, damage, false);
    }

    /**
     * Damages the shield.
     * @param hit if true, creates the hit sound.
     */
    public float absorb(Unit unit, float lx, float ly, float damage, boolean hit){
        float absorbed = Math.min(damage, Math.max(getShield(unit), 0f));
        if(absorbed > 0f){
            if(getShield(unit) <= damage){
                Vec2 pos = shieldPos(unit, Tmp.v1);
                addShield(unit, -cooldown * regen);

                if(breakEffect == Fx.arcShieldBreak){
                    breakEffect.at(pos.x, pos.y, 0, shieldColor(unit), unit);
                }else{
                    breakEffect.at(pos.x, pos.y, radius, shieldColor(unit), unit);
                }
                breakSound.at(pos.x, pos.y);
            }
            if(hit){
                hitSound.at(lx, ly, 1f + Mathf.range(0.1f), hitSoundVolume);
            }else{
                Fx.absorb.at(lx, ly);
            }

            addShield(unit, -damage);
            alpha = 1f;

            if(unitSlowdown > 0f){
                shots = Math.min(shots + 1f, shotThreshold);
            }
        }
        return absorbed;
    }

    public void reflect(Posc pos, Vec2 vel, float size, Cons<Float> reflect){
        if(!Mathf.chance(chanceReflect)) return;

        float ux = paramPos.x, uy = paramPos.y;
        //we cannot trust source position or rotation for fx
        if(size > reflectEffectSize){
            float dst = shieldBounds() - 5f;
            float angle = Angles.angle(ux, uy, pos.getX(), pos.getY());
            //vanilla projectile sizes visually are fairly small
            reflectEffect.at(ux + Angles.trnsx(angle, dst), uy + Angles.trnsy(angle, dst), angle + 180f, shieldColor(), Math.min(size, 100f) * 0.8f);
        }

        // make sound
        reflectSound.at(pos, Mathf.random(0.9f, 1.1f));

        // translate position back to where it was upon collision
        pos.trns(-vel.x, -vel.y);
        float nx = pos.getX() - ux, ny = pos.getY() - uy;
        float nlen = Mathf.len(nx, ny);
        if(nlen > 0.0001f){
            nx /= nlen;
            ny /= nlen;
        }

        float dot = vel.x * nx + vel.y * ny;
        float rx = vel.x - 2f * dot * nx;
        float ry = vel.y - 2f * dot * ny;
        float outDot = rx * nx + ry * ny;
        float normalX = outDot * nx, normalY = outDot * ny;
        float tangX = rx - normalX, tangY = ry - normalY;

        vel.set(normalX + tangX * reflectVel, normalY + tangY * reflectVel);
        reflect.get(vel.len());
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("shield", Strings.autoFixed(max, 2)));
        t.row();
        t.add(abilityStat("repairspeed", Strings.autoFixed(regen * 60f, 2)));
        t.row();
        t.add(abilityStat("cooldown", Strings.autoFixed(cooldown / 60f, 2)));
        if(chanceReflect > 0f){
            t.row();
            t.add(abilityStat("deflectchance", Strings.autoFixed(chanceReflect * 100f, 2)));
        }
    }

    @Override
    public void update(Unit unit){
        float shield = getShield(unit);
        if(unitShield){
            if(shield <= 0f && !wasBroken){
                addShield(unit, -cooldown * regen);

                breakEffect.at(unit.x, unit.y, radius, shieldColor(unit), this);
                breakSound.at(unit.x, unit.y);
            }

            wasBroken = shield <= 0f;
        }

        if(unitSlowdown > 0f){
            //slowdown changes are % based
            shots = Mathf.approachDelta(shots, 0f, shotThreshold / slowdownTime);
            unit.speedMultiplier = Mathf.approachDelta(1f, unitSlowdown, Mathf.clamp(shots / shotThreshold));
        }

        if(shield < scaledMax(unit)){
            addShield(unit, Time.delta * regen);
        }

        alpha = Math.max(alpha - Time.delta / 10f, 0f);

        boolean active = active(unit);
        if(active){
            radiusScale = Mathf.lerpDelta(radiusScale, 1f, 0.06f);
            widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
            paramUnit = unit;
            paramField = this;
            checkRadius();
            shieldPos(unit, paramPos);

            float reach = shieldBounds();
            Vars.state.entities.bullet.intersect(paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, shieldConsumer);
            if(pushUnits || reflectMissiles || missileUnitMultiplier >= 0f){
                Units.nearbyEnemies(paramUnit.team, paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, unitConsumer);
            }
        }else{
            radiusScale = 0f;
            widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
        }
    }

    @Override
    public void death(Unit unit){
        if(getShield(unit) > 0f && !wasBroken){
            Fx.shieldBreak.at(unit.x, unit.y, radius, shieldColor(unit), unit);
            breakSound.at(unit.x, unit.y);
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("stat.shieldhealth", Pal.accent, () -> getShield(unit) / scaledMax(unit))).row();
    }

    @Override
    public void created(Unit unit){
        if(unitShield){
            unit.shield = scaledMax(unit);
        }else{
            data = scaledMax(unit);
        }
    }
}