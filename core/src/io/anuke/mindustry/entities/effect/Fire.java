package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.effectGroup;
import static io.anuke.mindustry.Vars.world;

public class Fire extends TimedEntity {
    private Tile tile;
    private float flammability = -1;

    public Fire(Tile tile){
        this.tile = tile;
        lifetime = 1000f;
    }

    @Override
    public void update() {
        super.update();

        TileEntity entity = tile.target().entity;
        boolean damage = entity != null;

        if(!damage){
            time += Timers.delta()*8;
        }else if (flammability < 0){
            flammability = tile.block().getFlammability(tile);
        }

        if(damage) {

            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Timers.delta();

            if (flammability > 1f && Mathf.chance(0.03 * Timers.delta() * Mathf.clamp(flammability/5f, 0.3f, 2f))) {
                GridPoint2 p = Mathf.select(Geometry.d4);
                Tile other = world.tile(tile.x + p.x, tile.y + p.y);
                new Fire(other).add();
                //if(other != null && other.target().entity != null && !other.entity.hasFire()){
                    //other.entity.setFire();
                //}
            }
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.fire, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));

            if(damage){
                entity.damage(0.4f);
            }
        }

        if(Mathf.chance(0.05 * Timers.delta())){
            Effects.effect(EnvironmentFx.smoke, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));
        }
    }

    @Override
    public Fire add(){
        return add(effectGroup);
    }
}
