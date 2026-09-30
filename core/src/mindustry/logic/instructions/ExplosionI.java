package mindustry.logic.instructions;

import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class ExplosionI implements LogicInstruction{
    public LogicVar team, x, y, radius, damage, air, ground, pierce, effect;

    public ExplosionI(LogicVar team, LogicVar x, LogicVar y, LogicVar radius, LogicVar damage, LogicVar air, LogicVar ground, LogicVar pierce, LogicVar effect){
        this.team = team;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.damage = damage;
        this.air = air;
        this.ground = ground;
        this.pierce = pierce;
        this.effect = effect;
    }

    public ExplosionI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(net.client()) return;

        Team t = team.team();
        //note that there is a radius cap
        Call.logicExplosion(t, World.unconv(x.numf()), World.unconv(y.numf()), World.unconv(Math.min(radius.numf(), 100)), damage.numf(), air.bool(), ground.bool(), pierce.bool(), effect.bool());
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void logicExplosion(Team team, float x, float y, float radius, float damage, boolean air, boolean ground, boolean pierce, boolean effect){
        if(damage < 0f) return;

        Damage.damage(team, x, y, radius, damage, pierce, air, ground, true, null);
        if(effect){
            if(pierce){
                Fx.spawnShockwave.at(x, y, World.conv(radius));
            }else{
                Fx.dynamicExplosion.at(x, y, World.conv(radius) / 8f);
            }
        }
    }
}
