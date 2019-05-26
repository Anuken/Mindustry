package io.anuke.mindustry.entities.traits;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.BuildSelectEvent;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.graphics.Shapes;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;

import java.io.*;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.entities.traits.BuilderTrait.BuildDataStatic.removal;
import static io.anuke.mindustry.entities.traits.BuilderTrait.BuildDataStatic.tmptr;

/**
 * Interface for units that build, break or mine things.
 */
public interface BuilderTrait extends Entity, TeamTrait{
    //these are not instance variables!
    float placeDistance = 220f;
    float mineDistance = 70f;

    /**
     * Update building mechanism for this unit.
     * This includes mining.
     */
    default void updateBuilding(){
        float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : placeDistance;
        Unit unit = (Unit)this;
        //remove already completed build requests
        removal.clear();
        for(BuildRequest req : getPlaceQueue()){
            removal.add(req);
        }

        getPlaceQueue().clear();

        for(BuildRequest request : removal){
            if(!((request.breaking && world.tile(request.x, request.y).block() == Blocks.air) ||
            (!request.breaking && (world.tile(request.x, request.y).rotation() == request.rotation || !request.block.rotate)
            && world.tile(request.x, request.y).block() == request.block))){
                getPlaceQueue().addLast(request);
            }
        }

        BuildRequest current = getCurrentRequest();

        //update mining here
        if(current == null){
            if(getMineTile() != null){
                updateMining();
            }
            return;
        }else{
            setMineTile(null);
        }

        Tile tile = world.tile(current.x, current.y);

        if(dst(tile) > finalPlaceDst){
            if(getPlaceQueue().size > 1){
                getPlaceQueue().removeFirst();
                getPlaceQueue().addLast(current);
            }
            return;
        }

        if(!(tile.block() instanceof BuildBlock)){
            if(canCreateBlocks() && !current.breaking && Build.validPlace(getTeam(), current.x, current.y, current.block, current.rotation)){
                Call.beginPlace(getTeam(), current.x, current.y, current.block, current.rotation);
            }else if(canCreateBlocks() && current.breaking && Build.validBreak(getTeam(), current.x, current.y)){
                Call.beginBreak(getTeam(), current.x, current.y);
            }else{
                getPlaceQueue().removeFirst();
                return;
            }
        }

        TileEntity core = unit.getClosestCore();

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

        //progress is synced, thus not updated clientside
        if(!Net.client()){
            //deconstructing is 2x as fast
            if(current.breaking){
                entity.deconstruct(unit, core, 2f / entity.buildCost * Time.delta() * getBuildPower(tile) * state.rules.buildSpeedMultiplier);
            }else{
                entity.construct(unit, core, 1f / entity.buildCost * Time.delta() * getBuildPower(tile) * state.rules.buildSpeedMultiplier);
            }

            current.progress = entity.progress();
        }else{
            entity.progress = current.progress;
        }

        if(!current.initialized){
            Core.app.post(() -> Events.fire(new BuildSelectEvent(tile, unit.getTeam(), this, current.breaking)));
            current.initialized = true;
        }
    }

    /** Returns the queue for storing build requests. */
    Queue<BuildRequest> getPlaceQueue();

    /** Returns the tile this builder is currently mining. */
    Tile getMineTile();

    /** Sets the tile this builder is currently mining. */
    void setMineTile(Tile tile);

    /** Returns the minining speed of this miner. 1 = standard, 0.5 = half speed, 2 = double speed, etc. */
    float getMinePower();

    /** Build power, can be any float. 1 = builds recipes in normal time, 0 = doesn't build at all. */
    float getBuildPower(Tile tile);

    /** Returns whether or not this builder can mine a specific item type. */
    boolean canMine(Item item);

    /** Whether this type of builder can begin creating new blocks. */
    default boolean canCreateBlocks(){
        return true;
    }

    default void writeBuilding(DataOutput output) throws IOException{
        BuildRequest request = getCurrentRequest();

        if(request != null){
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
        if(applyChanges) getPlaceQueue().clear();

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
                getPlaceQueue().addLast(request);
            }else if(isBuilding()){
                getCurrentRequest().progress = progress;
            }
        }
    }

    /** Return whether this builder's place queue contains items. */
    default boolean isBuilding(){
        return getPlaceQueue().size != 0;
    }

    /** Clears the placement queue. */
    default void clearBuilding(){
        getPlaceQueue().clear();
    }

    /** Add another build requests to the tail of the queue, if it doesn't exist there yet. */
    default void addBuildRequest(BuildRequest place){
        for(BuildRequest request : getPlaceQueue()){
            if(request.x == place.x && request.y == place.y){
                return;
            }
        }
        Tile tile = world.tile(place.x, place.y);
        if(tile != null && tile.entity instanceof BuildEntity){
            place.progress = tile.<BuildEntity>entity().progress;
        }
        getPlaceQueue().addLast(place);
    }

    /**
     * Return the build requests currently active, or the one at the top of the queue.
     * May return null.
     */
    default BuildRequest getCurrentRequest(){
        return getPlaceQueue().size == 0 ? null : getPlaceQueue().first();
    }

    //due to iOS wierdness, this is apparently required
    class BuildDataStatic{
        static Array<BuildRequest> removal = new Array<>();
        static Vector2[] tmptr = new Vector2[]{new Vector2(), new Vector2(), new Vector2(), new Vector2()};
    }

    /** Do not call directly. */
    default void updateMining(){
        Unit unit = (Unit)this;
        Tile tile = getMineTile();
        TileEntity core = unit.getClosestCore();

        if(core == null || tile.block() != Blocks.air || dst(tile.worldx(), tile.worldy()) > mineDistance
        || tile.drop() == null || !unit.acceptsItem(tile.drop()) || !canMine(tile.drop())){
            setMineTile(null);
        }else{
            Item item = tile.drop();
            unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(tile.worldx(), tile.worldy()), 0.4f);

            if(Mathf.chance(Time.delta() * (0.06 - item.hardness * 0.01) * getMinePower())){

                if(unit.dst(core) < mineTransferRange && core.tile.block().acceptStack(item, 1, core.tile, unit) == 1){
                    Call.transferItemTo(item, 1,
                    tile.worldx() + Mathf.range(tilesize / 2f),
                    tile.worldy() + Mathf.range(tilesize / 2f), core.tile);
                }else if(unit.acceptsItem(item)){
                    Call.transferItemToUnit(item,
                    tile.worldx() + Mathf.range(tilesize / 2f),
                    tile.worldy() + Mathf.range(tilesize / 2f),
                    unit);
                }
            }

            if(Mathf.chance(0.06 * Time.delta())){
                Effects.effect(Fx.pulverizeSmall,
                tile.worldx() + Mathf.range(tilesize / 2f),
                tile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
            }
        }
    }

    /** Draw placement effects for an entity. This includes mining */
    default void drawBuilding(){
        Unit unit = (Unit)this;
        BuildRequest request;
        if(!isBuilding()){
            if(getMineTile() != null){
                drawMining();
            }
            return;
        }

        request = getCurrentRequest();

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

    /** Internal use only. */
    default void drawMining(){
        Unit unit = (Unit)this;
        Tile tile = getMineTile();

        if(tile == null) return;

        float focusLen = 4f + Mathf.absin(Time.time(), 1.1f, 0.5f);
        float swingScl = 12f, swingMag = tilesize / 8f;
        float flashScl = 0.3f;

        float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
        float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

        float ex = tile.worldx() + Mathf.sin(Time.time() + 48, swingScl, swingMag);
        float ey = tile.worldy() + Mathf.sin(Time.time() + 48, swingScl + 2f, swingMag);

        Draw.color(Color.LIGHT_GRAY, Color.WHITE, 1f - flashScl + Mathf.absin(Time.time(), 0.5f, flashScl));

        Shapes.laser("minelaser", "minelaser-end", px, py, ex, ey, 0.75f);

        if(unit instanceof Player && ((Player)unit).isLocal){
            Lines.stroke(1f, Pal.accent);
            Lines.poly(tile.worldx(), tile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time());
        }

        Draw.color();
    }

    /** Class for storing build requests. Can be either a place or remove request. */
    class BuildRequest{
        public final int x, y, rotation;
        public final Block block;
        public final boolean breaking;

        public float progress;
        public boolean initialized;

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
