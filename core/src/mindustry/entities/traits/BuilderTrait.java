package mindustry.entities.traits;

import arc.*;
import arc.struct.Queue;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.BuildBlock.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.*;
import static mindustry.entities.traits.BuilderTrait.BuildDataStatic.*;

/** Interface for units that build things.*/
public interface BuilderTrait extends Entity, TeamTrait{
    //these are not instance variables!
    float placeDistance = 220f;
    float mineDistance = 70f;

    /** Updates building mechanism for this unit.*/
    default void updateBuilding(){
        float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : placeDistance;
        Unit unit = (Unit)this;

        Iterator<BuildRequest> it = buildQueue().iterator();
        while(it.hasNext()){
            BuildRequest req = it.next();
            Tile tile = world.tile(req.x, req.y);
            if(tile == null || (req.breaking && tile.block() == Blocks.air) || (!req.breaking && (tile.rotation() == req.rotation || !req.block.rotate) && tile.block() == req.block)){
                it.remove();
            }
        }

        TileEntity core = unit.getClosestCore();

        //nothing to build.
        if(buildRequest() == null) return;

        //find the next build request
        if(buildQueue().size > 1){
            int total = 0;
            BuildRequest req;
            while((dst((req = buildRequest()).tile()) > finalPlaceDst || shouldSkip(req, core)) && total < buildQueue().size){
                buildQueue().removeFirst();
                buildQueue().addLast(req);
                total++;
            }
        }

        BuildRequest current = buildRequest();

        if(dst(current.tile()) > finalPlaceDst) return;

        Tile tile = world.tile(current.x, current.y);

        if(!(tile.block() instanceof BuildBlock)){
            if(!current.initialized && canCreateBlocks() && !current.breaking && Build.validPlace(getTeam(), current.x, current.y, current.block, current.rotation)){
                Build.beginPlace(getTeam(), current.x, current.y, current.block, current.rotation);
            }else if(!current.initialized && canCreateBlocks() && current.breaking && Build.validBreak(getTeam(), current.x, current.y)){
                Build.beginBreak(getTeam(), current.x, current.y);
            }else{
                buildQueue().removeFirst();
                return;
            }
        }

        if(tile.entity instanceof BuildEntity && !current.initialized){
            Core.app.post(() -> Events.fire(new BuildSelectEvent(tile, unit.getTeam(), this, current.breaking)));
            current.initialized = true;
        }

        //if there is no core to build with or no build entity, stop building!
        if((core == null && !state.rules.infiniteResources) || !(tile.entity instanceof BuildEntity)){
            return;
        }

        //otherwise, update it.
        BuildEntity entity = tile.ent();

        if(entity == null){
            return;
        }

        if(unit.dst(tile) <= finalPlaceDst){
            unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(entity), 0.4f);
        }

        if(current.breaking){
            entity.deconstruct(unit, core, 1f / entity.buildCost * Time.delta() * getBuildPower(tile) * state.rules.buildSpeedMultiplier);
        }else{
            if(entity.construct(unit, core, 1f / entity.buildCost * Time.delta() * getBuildPower(tile) * state.rules.buildSpeedMultiplier, current.hasConfig)){
                if(current.hasConfig){
                    Call.onTileConfig(null, tile, current.config);
                }
            }
        }

        current.stuck = Mathf.equal(current.progress, entity.progress);
        current.progress = entity.progress;
    }

    /** @return whether this request should be skipped, in favor of the next one. */
    default boolean shouldSkip(BuildRequest request, @Nullable TileEntity core){
        //requests that you have at least *started* are considered
        if(state.rules.infiniteResources || request.breaking || !request.initialized || core == null) return false;
        return request.stuck && !core.items.has(request.block.requirements);
    }

    default void removeRequest(int x, int y, boolean breaking){
        //remove matching request
        int idx = player.buildQueue().indexOf(req -> req.breaking == breaking && req.x == x && req.y == y);
        if(idx != -1){
            player.buildQueue().removeIndex(idx);
        }
    }

    /** Returns the queue for storing build requests. */
    Queue<BuildRequest> buildQueue();

    /** Build power, can be any float. 1 = builds recipes in normal time, 0 = doesn't build at all. */
    float getBuildPower(Tile tile);

    /** Whether this type of builder can begin creating new blocks. */
    default boolean canCreateBlocks(){
        return true;
    }

    default void writeBuilding(DataOutput output) throws IOException{
        BuildRequest request = buildRequest();

        if(request != null && (request.block != null || request.breaking)){
            output.writeByte(request.breaking ? 1 : 0);
            output.writeInt(Pos.get(request.x, request.y));
            output.writeFloat(request.progress);
            if(!request.breaking){
                output.writeShort(request.block.id);
                output.writeByte(request.rotation);
            }
        }else{
            output.writeByte(-1);
        }
    }

    default void readBuilding(DataInput input) throws IOException{
        readBuilding(input, true);
    }

    default void readBuilding(DataInput input, boolean applyChanges) throws IOException{
        if(applyChanges) buildQueue().clear();

        byte type = input.readByte();
        if(type != -1){
            int position = input.readInt();
            float progress = input.readFloat();
            BuildRequest request;

            if(type == 1){ //remove
                request = new BuildRequest(Pos.x(position), Pos.y(position));
            }else{ //place
                short block = input.readShort();
                byte rotation = input.readByte();
                request = new BuildRequest(Pos.x(position), Pos.y(position), rotation, content.block(block));
            }

            request.progress = progress;

            if(applyChanges){
                buildQueue().addLast(request);
            }else if(isBuilding()){
                BuildRequest last = buildRequest();
                last.progress = progress;
                if(last.tile() != null && last.tile().entity instanceof BuildEntity){
                    ((BuildEntity)last.tile().entity).progress = progress;
                }
            }
        }
    }

    /** Return whether this builder's place queue contains items. */
    default boolean isBuilding(){
        return buildQueue().size != 0;
    }

    /** Clears the placement queue. */
    default void clearBuilding(){
        buildQueue().clear();
    }

    /** Add another build requests to the tail of the queue, if it doesn't exist there yet. */
    default void addBuildRequest(BuildRequest place){
        addBuildRequest(place, true);
    }

    /** Add another build requests to the queue, if it doesn't exist there yet. */
    default void addBuildRequest(BuildRequest place, boolean tail){
        BuildRequest replace = null;
        for(BuildRequest request : buildQueue()){
            if(request.x == place.x && request.y == place.y){
                replace = request;
                break;
            }
        }
        if(replace != null){
            buildQueue().remove(replace);
        }
        Tile tile = world.tile(place.x, place.y);
        if(tile != null && tile.entity instanceof BuildEntity){
            place.progress = tile.<BuildEntity>ent().progress;
        }
        if(tail){
            buildQueue().addLast(place);
        }else{
            buildQueue().addFirst(place);
        }
    }

    /**
     * Return the build requests currently active, or the one at the top of the queue.
     * May return null.
     */
    default @Nullable
    BuildRequest buildRequest(){
        return buildQueue().size == 0 ? null : buildQueue().first();
    }

    //due to iOS weirdness, this is apparently required
    class BuildDataStatic{
        static Vec2[] tmptr = new Vec2[]{new Vec2(), new Vec2(), new Vec2(), new Vec2()};
    }

    /** Draw placement effects for an entity. */
    default void drawBuilding(){
        if(!isBuilding()) return;

        Unit unit = (Unit)this;
        BuildRequest request = buildRequest();
        Tile tile = world.tile(request.x, request.y);

        if(dst(tile) > placeDistance && !state.isEditor()){
            return;
        }

        Lines.stroke(1f, Pal.accent);
        float focusLen = 3.8f + Mathf.absin(Time.time(), 1.1f, 0.6f);
        float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
        float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

        float sz = Vars.tilesize * tile.block().size / 2f;
        float ang = unit.angleTo(tile);

        tmptr[0].set(tile.drawx() - sz, tile.drawy() - sz);
        tmptr[1].set(tile.drawx() + sz, tile.drawy() - sz);
        tmptr[2].set(tile.drawx() - sz, tile.drawy() + sz);
        tmptr[3].set(tile.drawx() + sz, tile.drawy() + sz);

        Arrays.sort(tmptr, (a, b) -> -Float.compare(Angles.angleDist(Angles.angle(unit.x, unit.y, a.x, a.y), ang),
                Angles.angleDist(Angles.angle(unit.x, unit.y, b.x, b.y), ang)));

        float x1 = tmptr[0].x, y1 = tmptr[0].y,
                x3 = tmptr[1].x, y3 = tmptr[1].y;

        Draw.alpha(1f);

        Lines.line(px, py, x1, y1);
        Lines.line(px, py, x3, y3);

        Fill.circle(px, py, 1.6f + Mathf.absin(Time.time(), 0.8f, 1.5f));

        Draw.color();
    }

}
