package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.state;

public class FlyingUnitType extends UnitType {
    private static Vector2 vec = new Vector2();

    public FlyingUnitType(String name) {
        super(name);
        speed = 1f;
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation);

        Draw.alpha(1f);
    }

    @Override
    public void behavior(BaseUnit unit) {
        vec.set(unit.target.x - unit.x, unit.target.y - unit.y);
        vec.setLength(speed);

        unit.velocity.lerp(vec, 0.1f * Timers.delta()); //TODO clamp it so it doesn't glitch out at low fps

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
