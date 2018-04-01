package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.bullets.BulletType;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.state;

public abstract class GroundUnitType extends UnitType{
    //only use for drawing!
    protected Translator tr1 = new Translator();
    //only use for updating!
    protected Translator tr2 = new Translator();

    protected float stopDistance = 30f;

    public GroundUnitType(String name) {
        super(name);
        maxVelocity = 2f;
        speed = 0.05f;
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.rotation = unit.velocity.angle();
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        float walktime = unit.walkTime;

        float ft = Mathf.sin(walktime, 6f, 2f);

        for (int i : Mathf.signs) {
            tr1.trns(unit.baseRotation, ft * i);
            Draw.rect(name + "-leg", unit.x + tr1.x, unit.y + tr1.y, 12f * i, 12f - Mathf.clamp(ft * i, 0, 2), unit.baseRotation - 90);
        }

        Draw.rect(name + "-base", unit.x, unit.y, unit.baseRotation- 90);

        Draw.rect(name, unit.x, unit.y, unit.rotation -90);

        Draw.alpha(1f);
    }

    @Override
    public void updateTargeting(BaseUnit unit) {
        if(!unit.timer.get(timerTarget, 20)) return;

        unit.target = Units.findEnemyTile(unit.team, unit.x, unit.y, range, t -> true);

        if(unit.target != null) return;

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

    @Override
    public void behavior(BaseUnit unit) {
        //TODO actually pathfind
        tr2.set(unit.target.x, unit.target.y).sub(unit.x, unit.y);

        if(tr2.len() > stopDistance){
            tr2.limit(speed);

            unit.walkTime += Timers.delta();

            unit.velocity.add(tr2);
        }

        if(unit.timer.get(timerReload, reload)){
            shoot(unit, BulletType.shot, tr2.angle(), 4f);
        }
    }
}
