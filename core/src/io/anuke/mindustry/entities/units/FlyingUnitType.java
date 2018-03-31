package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.graphics.fx.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.state;

public class FlyingUnitType extends UnitType {
    private static Vector2 vec = new Vector2();

    protected float boosterLength = 4.5f;

    public FlyingUnitType(String name) {
        super(name);
        speed = 0.2f;
        maxVelocity = 4f;
        drag = 0.01f;
        isFlying = true;
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.rotation = unit.velocity.angle();

        if(unit.velocity.len() > 0.2f && unit.timer.get(timerBoost, 2f)){
            Effects.effect(Fx.dash, unit.x + Angles.trnsx(unit.rotation + 180f, boosterLength),
                    unit.y + Angles.trnsy(unit.rotation + 180f, boosterLength));
        }
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);

        Draw.alpha(1f);
    }

    @Override
    public void behavior(BaseUnit unit) {
        vec.set(unit.target.x - unit.x, unit.target.y - unit.y);

        float ang = vec.angle();
        float len = vec.len();

        float circleLength = 40f;

        if(vec.len() < circleLength){
            vec.rotate((circleLength-vec.len())/circleLength * 180f);
        }

        vec.setLength(speed * Timers.delta());

        unit.velocity.add(vec); //TODO clamp it so it doesn't glitch out at low fps

        if(unit.timer.get(timerReload, reload) && len < range){
            shoot(unit, BulletType.shot, ang, 4f);
        }
    }

    @Override
    public void updateTargeting(BaseUnit unit) {
        if(!unit.timer.get(timerTarget, 20)) return;

        ObjectSet<TeamData> teams = state.teams.enemyDataOf(unit.team);

        Tile closest = null;
        float cdist = 0f;

        for(TeamData data : teams){
            for(Tile tile : data.cores){
                float dist = Vector2.dst(unit.x, unit.y, tile.drawx(), tile.drawy());
                if(closest == null || dist < cdist){
                    closest = tile;
                    cdist = dist;
                }
            }
        }

        if(closest != null){
            unit.target = closest.entity;
        }else{
            unit.target = null;
        }
    }
}
