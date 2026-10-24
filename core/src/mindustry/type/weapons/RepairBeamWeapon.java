package mindustry.type.weapons;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.Units.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Note that this weapon requires a bullet with a positive maxRange.
 * Rotation must be set to true. Fixed repair points are not supported.
 * */
public class RepairBeamWeapon extends Weapon{
    public float unitRepairSpeed = 0.3f, blockRepairSpeed = 0f;
    public float blockRepairPercent = 0f, unitRepairPercent = 0f;
    /** Only when controllable is false. */
    public float snapRadius = 8f;
    public float beamWidth = 1f;
    public float pulseRadius = 6f;
    public float pulseStroke = 2f;
    public float widthSinMag = 0f, widthSinScl = 4f;
    /** A multiplier on repair when recently damaged. Set to <= 0 to disable. */
    public float blockRecentDmgMult = -1f, unitRecentDmgMult = -1f;
    /** How much time after being damaged counts as "recently damaged". */
    public float blockRecentDmgTime = 60f * 5f, unitRecentDmgTime = 60f * 5f;
    /** Whether to linearly scale the recent damage multiplier back to 1 over time. */
    public boolean scaleRecentDmg = true;
    /** Function for choosing which unit to target. */
    public Sortf unitSort = UnitSorts.closest;

    public TextureRegion laser, laserEnd, laserTop, laserTopEnd;

    public Color laserColor = Color.valueOf("98ffa9"), laserTopColor = Color.white.cpy();
    //only for blocks
    public Color healColor = Pal.heal;
    public Effect healEffect = Fx.healBlockFull;

    private boolean targetUnits;
    private boolean targetBlocks;

    protected static Teamc snapTarget;
    public RepairBeamWeapon(String name){
        super(name);
    }

    public RepairBeamWeapon(){
        targetUnits = unitRepairSpeed != 0f || unitRepairPercent != 0f;
        targetBlocks = blockRepairSpeed != 0f || blockRepairPercent != 0f;
    }

    {
        //must be >0 to prevent various bugs
        reload = 1f;
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        mountType = HealBeamMount::new;
        recoil = 0f;
        noAttack = true;
        useAttackRange = false;
        activeSound = Sounds.beamHeal;
    }

    @Override
    public void addStats(UnitType u, Table w){

        if(targetUnits) w.add("[lightgray]" + Stat.unitRepairSpeed.localized() + formatStats(unitRepairSpeed, unitRepairPercent)).row();
        if(targetBlocks) w.add("[lightgray]" + Stat.blockRepairSpeed.localized() + formatStats(blockRepairSpeed, blockRepairPercent)).row();
        if(unitRecentDmgMult > 0 || blockRecentDmgMult > 0) w.add("[lightgray]" + Stat.recentDamageMultiplier.localized() + ": [white]" + Strings.joinEntries(
            () -> unitRecentDmgMult > 0, () -> blockRecentDmgMult > 0, formatRecent(unitRecentDmgMult, unitRecentDmgTime), formatRecent(blockRecentDmgMult, blockRecentDmgTime))).row();
    }

    public String formatStats(float base, float percent){
        return ": " + (mirror ? "2x " : "") + "[white]" + Strings.joinEntries(() -> base != 0, () -> percent != 0,
        Strings.autoFixed(base * 60, 0) + StatUnit.perSecond.localized(), Core.bundle.format("ability.stat.unitrepairpercentspeed", Strings.autoFixed(percent, 2)));
    }

    public String formatRecent(float mult, float time){
        return Strings.signedMultStat(mult) + "x [lightgray]" + Core.bundle.format(scaleRecentDmg ? "bar.durationafterhitfaded" : "bar.durationafterhit", time / 60f);
    }

    @Override
    public float dps(){
        return 0f;
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("laser-white");
        laserEnd = Core.atlas.find("laser-white-end");
        laserTop = Core.atlas.find("laser-top");
        laserTopEnd = Core.atlas.find("laser-top-end");
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        if(targetUnits) return Units.closest(unit.team, x, y, range, u -> u != unit && isDamaged(u));
        if(targetBlocks) return Units.findAllyTile(unit.team, x, y, range, this::isDamaged);
        return null;
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return !(target.within(unit, range + unit.hitSize/2f) && target.team() == unit.team && target instanceof Healthc u && isDamaged(u) && u.isValid());
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        //does nothing, shooting is handled in update()
    }

    public boolean isDamaged(Healthc h){
        if(h instanceof Unit u) return isDamaged(u);
        if(h instanceof Building u) return isDamaged(u);
        return false;
    }

    public boolean isDamaged(Unit u){
        return u.damaged() || unitRepairSpeed < 0f || unitRepairPercent < 0f;
    }

    public boolean isDamaged(Building b){
        return b.damaged() || blockRepairSpeed < 0f || blockRepairPercent < 0f;
    }

    public float healValue(Unit u, float value, float result){
        return value * value < 0 ? Vars.state.rules.unitDamage(u.team) : result;
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        super.update(unit, mount);

        float
        weaponRotation = unit.rotation - 90,
        wx = unit.x + Angles.trnsx(weaponRotation, x, y),
        wy = unit.y + Angles.trnsy(weaponRotation, x, y);

        HealBeamMount heal = (HealBeamMount)mount;
        boolean canShoot = mount.shoot;

        if(!autoTarget){
            heal.target = null;
            if(canShoot){
                heal.lastEnd.set(heal.aimX, heal.aimY);

                if(!rotate && !Angles.within(Angles.angle(wx, wy, heal.aimX, heal.aimY), unit.rotation, shootCone)){
                    canShoot = false;
                }
            }

            //limit range
            heal.lastEnd.sub(wx, wy).limit(range()).add(wx, wy);

            //snap to closest target
            if(targetBlocks || targetUnits){
                snapTarget = null;
                World.raycastEachWorld(wx, wy, heal.lastEnd.x, heal.lastEnd.y, (x, y) -> {
                    float tx = x * tilesize, ty = y * tilesize;
                    if(targetBlocks){
                        Units.closestBuilding(unit.team, tx, ty, snapRadius, (b) -> {
                            if(isDamaged(b)){
                                snapTarget = b;
                                return true;
                            }
                            return false;
                        });
                    }
                    if(targetUnits){
                        Units.closest(unit.team, tx, ty, snapRadius, (u) -> {
                            if(isDamaged(u)){
                                snapTarget = u;
                                return true;
                            }
                            return false;
                        });
                    }

                    if(snapTarget instanceof Sized s){
                        float angle = Angles.angle(wx, wy, tx, ty);
                        float size = Math.max(4f, s.hitSize() / 2f);

                        heal.target = snapTarget;
                        heal.lastEnd.set(Math.abs(snapTarget.getX() - tx) > size ? snapTarget.getX() - Angles.trnsx(angle, size) : tx,
                        Math.abs(snapTarget.getY() - ty) > size ? snapTarget.getY() - Angles.trnsy(angle, size) : ty);
                        return true;
                    }
                    return false;
                });
            }
        }

        heal.strength = Mathf.lerpDelta(heal.strength, Mathf.num(autoTarget ? mount.target != null : canShoot), 0.2f);

        if(canShoot && mount.target instanceof Healthc u){
            float base = 0f, mult = 1f, recentMult = 1f, percent = 0f, rotation = 0f;
            Object effectData = null;

            if(u instanceof Building d){
                effectData = d.block;
                base = healValue(unit, blockRepairSpeed, base);
                percent = healValue(unit, blockRepairPercent, percent);
                if(blockRecentDmgMult > 0f && d.wasRecentlyDamaged(blockRecentDmgTime)) recentMult = blockRecentDmgMult;

            }
            if(u instanceof Unit d){
                effectData = d.type;
                base = healValue(unit, unitRepairSpeed, base);
                percent = healValue(unit, unitRepairPercent, percent);

                if(unitRecentDmgMult > 0f && d.wasRecentlyDamaged(unitRecentDmgTime)) recentMult = unitRecentDmgMult;
                rotation = d.rotation - 90f;
            }
            if(scaleRecentDmg){
                mult = Mathf.approachDelta(recentMult, 1f, Mathf.clamp((Vars.state.time - u.lastDamageTime()) / recentMult));
            }

            //create heal effect periodically
            if((heal.effectTimer += Time.delta) >= reload && effectData != null){
                healEffect.at(u.x(), u.y(), rotation, healColor, effectData);
                heal.effectTimer = 0f;
            }

            u.heal(mult * (base * heal.strength * Time.delta + percent * heal.strength * Time.delta * u.maxHealth() / 100f));
        }
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);

        HealBeamMount heal = (HealBeamMount)mount;

        if(unit.canShoot()){
            float
                weaponRotation = unit.rotation - 90,
                wx = unit.x + Angles.trnsx(weaponRotation, x, y),
                wy = unit.y + Angles.trnsy(weaponRotation, x, y),
                z = Draw.z();
            RepairTurret.drawBeam(wx, wy, unit.rotation + mount.rotation, shootY, unit.id, mount.target != null ? (Sized)mount.target : null, unit.team, heal.strength,
            pulseStroke, pulseRadius, beamWidth + Mathf.absin(state.time, widthSinScl, widthSinMag), heal.lastEnd, heal.offset, laserColor, laserTopColor,
            laser, laserEnd, laserTop, laserTopEnd);
            Draw.z(z);
        }
    }

    @Override
    public void init(){
        super.init();
        bullet.healPercent = blockRepairPercent;
    }

    public static class HealBeamMount extends WeaponMount{
        public Vec2 offset = new Vec2(), lastEnd = new Vec2();
        public float strength, effectTimer;

        public HealBeamMount(Weapon weapon){
            super(weapon);
        }
    }
}
