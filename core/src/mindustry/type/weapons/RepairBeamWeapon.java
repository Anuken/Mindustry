package mindustry.type.weapons;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

/**
 * Note that this weapon requires a bullet with a positive maxRange.
 * Rotation must be set to true. Fixed repair points are not supported.
 * */
public class RepairBeamWeapon extends Weapon{
    public boolean targetBuildings = false;

    public float repairSpeed = 0.3f;
    public float beamWidth = 1f;
    public float pulseRadius = 6f;
    public float pulseStroke = 2f;

    public TextureRegion laser, laserEnd, laserTop, laserTopEnd;

    public Color laserColor = Color.valueOf("98ffa9"), laserTopColor = Color.white.cpy();

    public RepairBeamWeapon(String name){
        super(name);
    }

    public RepairBeamWeapon(){
    }

    {
        //must be >0 to prevent various bugs
        reload = 1f;
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        useAmmo = false;
        mountType = HealBeamMount::new;
        recoil = 0f;
    }

    @Override
    public void addStats(UnitType u, Table w){
        w.row();
        w.add("[lightgray]" + Stat.repairSpeed.localized() + ": " + (mirror ? "2x " : "") + "[white]" + (int)(repairSpeed * 60) + " " + StatUnit.perSecond.localized());
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
        var out = Units.closest(unit.team, x, y, range, u -> u != unit && u.damaged());
        if(out != null || !targetBuildings) return out;
        //TODO maybe buildings shouldn't be allowed at all
        return Units.findAllyTile(unit.team, x, y, range, Building::damaged);
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return !(target.within(unit, range + unit.hitSize/2f) && target.team() == unit.team && target instanceof Healthc u && u.damaged() && u.isValid());
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float aimX, float aimY, float mountX, float mountY, float rotation, int side){
        //does nothing, shooting is handled in update()
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        super.update(unit, mount);

        HealBeamMount heal = (HealBeamMount)mount;
        heal.strength = Mathf.lerpDelta(heal.strength, Mathf.num(mount.target != null), 0.2f);

        if(mount.target instanceof Healthc u){
            u.heal(repairSpeed * heal.strength * Time.delta);
        }
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);

        HealBeamMount heal = (HealBeamMount)mount;

        float
        weaponRotation = unit.rotation - 90,
        wx = unit.x + Angles.trnsx(weaponRotation, x, y),
        wy = unit.y + Angles.trnsy(weaponRotation, x, y);

        float z = Draw.z();
        RepairPoint.drawBeam(wx, wy, unit.rotation + mount.rotation, shootY, unit.id, mount.target == null ? null : (Sized)mount.target, unit.team, heal.strength,
            pulseStroke, pulseRadius, beamWidth, heal.lastEnd, heal.offset, laserColor, laserTopColor,
            laser, laserEnd, laserTop, laserTopEnd);
        Draw.z(z);
    }

    public static class HealBeamMount extends WeaponMount{
        public Vec2 offset = new Vec2(), lastEnd = new Vec2();
        public float strength;

        public HealBeamMount(Weapon weapon){
            super(weapon);
        }
    }
}
