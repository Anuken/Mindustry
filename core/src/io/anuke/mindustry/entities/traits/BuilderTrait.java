package io.anuke.mindustry.entities.traits;

import io.anuke.arc.*;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.BuildBlock.*;

import java.io.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.entities.traits.BuilderTrait.BuildDataStatic.*;

/** Interface for units that build things.*/
public interface BuilderTrait extends Entity, TeamTrait{
    //these are not instance variables!
    float placeDistance = 220f;
    float mineDistance = 70f;

    /** Updates building mechanism for this unit.*/
    default void updateBuilding(){
        float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : placeDistance;
        Unit unit = (Unit)this;

        //remove already completed build requests
        removal.clear();
        removal.addAll(buildQueue());

        Structs.filter(buildQueue(), req -> {
            Tile tile = world.tile(req.x, req.y);
            return tile == null || (req.breaking && tile.block() == Blocks.air) || (!req.breaking && (tile.rotation() == req.rotation || !req.block.rotate) && tile.block() == req.block);
        });

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

        Tile tile = world.tile(current.x, current.y);

        if(!(tile.block() instanceof BuildBlock)){
            if(!current.initialized && canCreateBlocks() && !current.breaking && Build.validPlace(getTeam(), current.x, current.y, current.block, current.rotation)){
                Call.beginPlace(getTeam(), current.x, current.y, current.block, current.rotation);
            }else if(!current.initialized && canCreateBlocks() && current.breaking && Build.validBreak(getTeam(), current.x, current.y)){
                Call.beginBreak(getTeam(), current.x, current.y);
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
        BuildEntity entity = tile.entity();

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
        if(state.rules.infiniteResources || request.breaking || !request.initialized) return false;
        return request.stuck && !core.items.has(request.block.requirements);
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
            place.progress = tile.<BuildEntity>entity().progress;
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
        static Array<BuildRequest> removal = new Array<>();
        static Vector2[] tmptr = new Vector2[]{new Vector2(), new Vector2(), new Vector2(), new Vector2()};
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

    /** Class for storing build requests. Can be either a place or remove request. */
    class BuildRequest{
        /** Position and rotation of this request. */
        public int x, y, rotation;
        /** Block being placed. If null, this is a breaking request.*/
        public @Nullable Block block;
        /** Whether this is a break request.*/
        public boolean breaking;
        /** Whether this request comes with a config int. If yes, any blocks placed with this request will not call playerPlaced.*/
        public boolean hasConfig;
        /** Config int. Not used unless hasConfig is true.*/
        public int config;
        /** Original position, only used in schematics.*/
        public int originalX, originalY, originalWidth, originalHeight;

        /** Last progress.*/
        public float progress;
        /** Whether construction has started for this request, and other special variables.*/
        public boolean initialized, worldContext = true, stuck;

        /** Visual scale. Used only for rendering.*/
        public float animScale = 0f;

        /** This creates a build request. */
        public BuildRequest(int x, int y, int rotation, Block block){
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.block = block;
            this.breaking = false;
        }

        /** This creates a remove request. */
        public BuildRequest(int x, int y){
            this.x = x;
            this.y = y;
            this.rotation = -1;
            this.block = world.tile(x, y).block();
            this.breaking = true;
        }

        public BuildRequest(){

        }

        public BuildRequest copy(){
            BuildRequest copy = new BuildRequest();
            copy.x = x;
            copy.y = y;
            copy.rotation = rotation;
            copy.block = block;
            copy.breaking = breaking;
            copy.hasConfig = hasConfig;
            copy.config = config;
            copy.originalX = originalX;
            copy.originalY = originalY;
            copy.progress = progress;
            copy.initialized = initialized;
            copy.animScale = animScale;
            return copy;
        }

        public BuildRequest original(int x, int y, int originalWidth, int originalHeight){
            originalX = x;
            originalY = y;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
            return this;
        }

        public Rectangle bounds(Rectangle rect){
            if(breaking){
                return rect.set(-100f, -100f, 0f, 0f);
            }else{
                return block.bounds(x, y, rect);
            }
        }

        public BuildRequest set(int x, int y, int rotation, Block block){
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.block = block;
            this.breaking = false;
            return this;
        }

        public float drawx(){
            return x*tilesize + block.offset();
        }

        public float drawy(){
            return y*tilesize + block.offset();
        }

        public BuildRequest configure(int config){
            this.config = config;
            this.hasConfig = true;
            return this;
        }

        public @Nullable Tile tile(){
            return world.tile(x, y);
        }

        @Override
        public String toString(){
            return "BuildRequest{" +
            "x=" + x +
            ", y=" + y +
            ", rotation=" + rotation +
            ", recipe=" + block +
            ", breaking=" + breaking +
            ", progress=" + progress +
            ", initialized=" + initialized +
            '}';
        }
    }
}
