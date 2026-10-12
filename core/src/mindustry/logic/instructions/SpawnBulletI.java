package mindustry.logic.instructions;

import mindustry.core.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;

public class SpawnBulletI implements LogicInstruction{
    public LogicVar result, from, weapon, x, y, rotation, team, owner, damage, velocityScl, lifeScl, aimX, aimY;

    public SpawnBulletI(LogicVar result, LogicVar from, LogicVar index, LogicVar x, LogicVar y, LogicVar rotation, LogicVar team, LogicVar owner, LogicVar damage, LogicVar velocityScl, LogicVar lifeScl, LogicVar aimX, LogicVar aimY){
        this.result = result;
        this.from = from;
        this.weapon = index;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.team = team;
        this.owner = owner;
        this.damage = damage;
        this.velocityScl = velocityScl;
        this.lifeScl = lifeScl;
        this.aimX = aimX;
        this.aimY = aimY;
    }

    public SpawnBulletI(){
    }

    @Override
    public void run(LogicExecutor exec){
        Team teamVal = team.team();

        Object fromVal = from.obj();
        Entityc ownerVal = owner.obj() instanceof Entityc e ? e : null;
        if(teamVal == null && ownerVal instanceof Teamc t) teamVal = t.team();
        if(teamVal == null) teamVal = Team.derelict;
        BulletType type;

        if(fromVal instanceof UnitType u){
            int index = weapon.numi();
            type = index < 0 || index >= u.weapons.size ? null : u.weapons.get(index).bullet;
        }else if(fromVal instanceof ItemTurret t){
            var item = weapon.obj() instanceof Item i ? i : null;
            type = item == null ? null : t.ammoTypes.get(item);
        }else if(fromVal instanceof LiquidTurret t){
            var item = weapon.obj() instanceof Liquid i ? i : null;
            type = item == null ? null : t.ammoTypes.get(item);
        }else if(fromVal instanceof ContinuousLiquidTurret t){
            var item = weapon.obj() instanceof Liquid i ? i : null;
            type = item == null ? null : t.ammoTypes.get(item);
        }else if(fromVal instanceof PowerTurret t){
            type = t.shootType;
        }else if(fromVal instanceof ContinuousTurret t){
            type = t.shootType;
        }else{
            return;
        }

        if(type == null) return;

        result.setobj(type.create(ownerVal, teamVal, World.unconv(x.numf()), World.unconv(y.numf()), rotation.numf(), damage.numf(), velocityScl.numf(), lifeScl.numf(), null, null, World.unconv(aimX.numf()), World.unconv(aimY.numf())));
    }
}
