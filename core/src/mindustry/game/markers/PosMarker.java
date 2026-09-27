package mindustry.game.markers;

import arc.math.geom.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** A marker that has a position in the world in world coordinates. */
public abstract class PosMarker extends ObjectiveMarker{
    /** Position of marker, in world coordinates */
    public @TilePos Vec2 pos = new Vec2();

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            if(type == LMarkerControl.pos){
                pos.x = (float)p1 * tilesize;
            }
        }

        if(!Double.isNaN(p2)){
            if(type == LMarkerControl.pos){
                pos.y = (float)p2 * tilesize;
            }
        }
    }
}
