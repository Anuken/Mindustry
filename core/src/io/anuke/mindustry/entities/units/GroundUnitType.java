package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public abstract class GroundUnitType extends UnitType{
    private static final int nodeStateNone = -2;
    private static final int nodeStateCalculating = -1;

    //only use for drawing!
    protected Translator tr1 = new Translator();
    //only use for updating!
    protected Translator tr2 = new Translator();

    protected float jumpDistance = 4f;

    public GroundUnitType(String name) {
        super(name);
        maxVelocity = 1.1f;
        speed = 0.1f;
        drag = 0.4f;
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

        Floor floor = unit.getFloorOn();

        if(floor.liquid){
            Draw.tint(Hue.mix(Color.WHITE, floor.liquidColor, 0.5f));
        }

        for (int i : Mathf.signs) {
            tr1.trns(unit.baseRotation, ft * i);
            Draw.rect(name + "-leg", unit.x + tr1.x, unit.y + tr1.y, 12f * i, 12f - Mathf.clamp(ft * i, 0, 2), unit.baseRotation - 90);
        }

        if(floor.liquid) {
            Draw.tint(Color.WHITE, floor.liquidColor, unit.drownTime * 0.4f);
        }else {
            Draw.tint(Color.WHITE);
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
        if(unit.node == nodeStateNone){
            world.pathfinder().findPath(world.tileWorld(unit.x, unit.y), world.tileWorld(unit.target.x, unit.target.y), unit.path, unit.finder, () -> {
                unit.node = 0;
            });

            unit.node = nodeStateCalculating;
        }

        if(!(unit.node >= 0 && unit.node < unit.path.nodes.size)) return;

        Tile nodeTarget = unit.path.get(unit.node);

        tr2.set(nodeTarget.worldx(), nodeTarget.worldy()).sub(unit.x, unit.y);

        if(tr2.len() < jumpDistance){
            unit.node ++;
        }

        tr2.limit(speed);
        unit.walkTime += Timers.delta();
        unit.velocity.add(tr2);


        if(unit.timer.get(timerReload, reload)){
            //shoot(unit, BulletType.shot, tr2.angle(), 4f);
        }
    }
}
