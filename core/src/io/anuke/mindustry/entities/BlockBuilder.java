package io.anuke.mindustry.entities;

import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.BuildBlock;
import io.anuke.mindustry.world.blocks.types.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.world;

/**Interface for units that build thing.*/
public interface BlockBuilder {
    //temporary static final values
    Translator[] tmptr = {new Translator(), new Translator(), new Translator(), new Translator()};
    float placeDistance = 80f;

    /**Returns the queue for storing build requests.*/
    Queue<BuildRequest> getPlaceQueue();

    /**Clears the placement queue.*/
    default void clearBuilding(){
        getPlaceQueue().clear();
    }

    /**Add another build requests to the tail of the queue.*/
    default void addBuildRequest(BuildRequest place){
        getPlaceQueue().addLast(place);
    }

    /**Return the build requests currently active, or the one at the top of the queue.
     * May return null.*/
    default BuildRequest getCurrentRequest(){
        return getPlaceQueue().size == 0 ? null : getPlaceQueue().first();
    }

    /**Update building mechanism for this unit.*/
    default void updateBuilding(Unit unit){
        BuildRequest current = getCurrentRequest();

        if(current == null) return; //nothing to do here

        Tile tile = world.tile(current.x, current.y);

        if(unit.distanceTo(tile) > placeDistance) { //out of range, skip it.
            getPlaceQueue().removeFirst();
        }else if(current.remove){
            if(Build.validBreak(unit.team, current.x, current.y)){ //if it's valid, break it
                current.removeProgress += 1f / tile.getBreakTime();

                if(current.removeProgress >= 1f){
                    Build.breakBlock(unit.team, current.x, current.y, true, true);
                }
            }else{
                //otherwise, skip it
                getPlaceQueue().removeFirst();
            }
        }else{
            if (!(tile.block() instanceof BuildBlock)) { //check if haven't started placing
                if(Build.validPlace(unit.team, current.x, current.y, current.recipe.result, current.rotation)){
                    //if it's valid, place it
                    Build.placeBlock(unit.team, current.x, current.y, current.recipe, current.rotation);
                }else{
                    //otherwise, skip it
                    getPlaceQueue().removeFirst();
                }
            }else{
                TileEntity core = unit.getClosestCore();

                //if there is no core to build with, stop building!
                if(core == null){
                    return;
                }

                //otherwise, update it.
                BuildEntity entity = tile.entity();

                entity.addProgress(core.items, 1f / entity.recipe.cost);
                unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(entity), 0.4f);
            }
        }
    }

    /**Draw placement effects for an entity.*/
    default void drawBuilding(Unit unit){
        Tile tile = world.tile(getCurrentRequest().x, getCurrentRequest().y);

        Draw.color(unit.distanceTo(tile) > placeDistance ? "placeInvalid" : "accent");
        float focusLen = 3.8f + Mathf.absin(Timers.time(), 1.1f, 0.6f);
        float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
        float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

        float sz = Vars.tilesize*tile.block().size/2f;
        float ang = unit.angleTo(tile);

        tmptr[0].set(tile.drawx() - sz, tile.drawy() - sz);
        tmptr[1].set(tile.drawx() + sz, tile.drawy() - sz);
        tmptr[2].set(tile.drawx() - sz, tile.drawy() + sz);
        tmptr[3].set(tile.drawx() + sz, tile.drawy() + sz);

        Arrays.sort(tmptr, (a, b) -> -Float.compare(Angles.angleDist(Angles.angle(unit.x, unit.y, a.x, a.y), ang),
                Angles.angleDist(Angles.angle(unit.x, unit.y, b.x, b.y), ang)));

        float x1 = tmptr[0].x, y1 = tmptr[0].y,
                x3 = tmptr[1].x, y3 = tmptr[1].y;
        Translator close = Geometry.findClosest(unit.x, unit.y, tmptr);
        float x2 = close.x, y2 = close.y;

        Draw.alpha(0.3f + Mathf.absin(Timers.time(), 0.9f, 0.2f));

        Fill.tri(px, py, x2, y2, x1, y1);
        Fill.tri(px, py, x2, y2, x3, y3);

        Draw.alpha(1f);

        Lines.line(px, py, x1, y1);
        Lines.line(px, py, x3, y3);

        Fill.circle(px, py, 1.5f + Mathf.absin(Timers.time(), 1f, 1.8f));

        Draw.color();
    }

    /**Class for storing build requests. Can be either a place or remove request.*/
    class BuildRequest {
        public final int x, y, rotation;
        public final Recipe recipe;
        public final boolean remove;

        float removeProgress;

        /**This creates a build request.*/
        public BuildRequest(int x, int y, int rotation, Recipe recipe) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.recipe = recipe;
            this.remove = false;
        }

        /**This creates a remove request.*/
        public BuildRequest(int x, int y) {
            this.x = x;
            this.y = y;
            this.rotation = -1;
            this.recipe = null;
            this.remove = true;
        }
    }
}
