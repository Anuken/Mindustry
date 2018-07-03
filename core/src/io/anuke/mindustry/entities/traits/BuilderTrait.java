package io.anuke.mindustry.entities.traits;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BreakBlock;
import io.anuke.mindustry.world.blocks.BreakBlock.BreakEntity;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.*;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.tmptr;
import static io.anuke.mindustry.Vars.world;

/**Interface for units that build, break or mine things.*/
public interface BuilderTrait {
    //these are not instance variables!
    float placeDistance = 140f;
    float mineDistance = 70f;

    /**Returns the queue for storing build requests.*/
    Queue<BuildRequest> getPlaceQueue();

    /**Returns the tile this builder is currently mining.*/
    Tile getMineTile();

    /**Sets the tile this builder is currently mining.*/
    void setMineTile(Tile tile);

    /**Returns the minining speed of this miner. 1 = standard, 0.5 = half speed, 2 = double speed, etc.*/
    float getMinePower();

    /**Build power, can be any float. 1 = builds recipes in normal time, 0 = doesn't build at all.*/
    float getBuildPower(Tile tile);

    /**Return whether this builder's place queue contains items.*/
    default boolean isBuilding(){
        return getPlaceQueue().size != 0;
    }

    /**If a place request matching this signature is present, it is removed.
     * Otherwise, a new place request is added to the queue.*/
    default void replaceBuilding(int x, int y, int rotation, Recipe recipe){
        synchronized (getPlaceQueue()) {
            for (BuildRequest request : getPlaceQueue()) {
                if (request.x == x && request.y == y) {
                    clearBuilding();
                    addBuildRequest(request);
                    return;
                }
            }
        }

        addBuildRequest(new BuildRequest(x, y, rotation, recipe));
    }

    /**Clears the placement queue.*/
    default void clearBuilding(){
        if(this instanceof Player) {
            CallBlocks.onBuildDeselect((Player) this);
        }else{
            getPlaceQueue().clear();
        }
    }

    /**Add another build requests to the tail of the queue, if it doesn't exist there yet.*/
    default void addBuildRequest(BuildRequest place){
        synchronized (getPlaceQueue()) {
            for (BuildRequest request : getPlaceQueue()) {
                if (request.x == place.x && request.y == place.y) {
                    return;
                }
            }
            getPlaceQueue().addLast(place);
        }
    }

    /**Return the build requests currently active, or the one at the top of the queue.
     * May return null.*/
    default BuildRequest getCurrentRequest(){
        synchronized (getPlaceQueue()) {
            return getPlaceQueue().size == 0 ? null : getPlaceQueue().first();
        }
    }

    /**Update building mechanism for this unit.
     * This includes mining.*/
    default void updateBuilding(Unit unit){
        BuildRequest current = getCurrentRequest();

        //update mining here
        if(current == null){
            if(getMineTile() != null){
                updateMining(unit);
            }
            return;
        }else{
            setMineTile(null);
        }

        Tile tile = world.tile(current.x, current.y);

        if(unit.distanceTo(tile) > placeDistance || //out of range, skip it
                (current.lastEntity != null && current.lastEntity.isDead())) { //build/destroy request has died, skip it
            getPlaceQueue().removeFirst();
        }else if(current.remove){

            if (!(tile.block() instanceof BreakBlock)) { //check if haven't started placing
                if(Build.validBreak(unit.getTeam(), current.x, current.y)){

                    //if it's valid, place it
                    if(!current.requested && unit instanceof Player){
                        CallBlocks.breakBlock((Player)unit, unit.getTeam(), current.x, current.y);
                        current.requested = true;
                    }
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
                BreakEntity entity = tile.entity();
                current.lastEntity = entity;

                entity.addProgress(core, unit, 1f / entity.breakTime * Timers.delta() * getBuildPower(tile));
                unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(entity), 0.4f);
                getCurrentRequest().progress = entity.progress();
            }
        }else{
            if (!(tile.block() instanceof BuildBlock)) { //check if haven't started placing
                if(Build.validPlace(unit.getTeam(), current.x, current.y, current.recipe.result, current.rotation)){

                    //if it's valid, place it
                    if(!current.requested && unit instanceof Player){
                        CallBlocks.placeBlock((Player)unit, unit.getTeam(), current.x, current.y, current.recipe, current.rotation);
                        current.requested = true;
                    }

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
                current.lastEntity = entity;

                entity.addProgress(core.items, 1f / entity.recipe.cost * Timers.delta() * getBuildPower(tile));
                if(unit instanceof Player){
                    entity.lastBuilder = (Player)unit;
                }
                unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(entity), 0.4f);
                getCurrentRequest().progress = entity.progress();
            }
        }
    }

    /**Do not call directly.*/
    default void updateMining(Unit unit){
        Tile tile = getMineTile();

        if(tile.block() != Blocks.air || unit.distanceTo(tile.worldx(), tile.worldy()) > mineDistance || !unit.inventory.canAcceptItem(tile.floor().drops.item)){
            setMineTile(null);
        }else{
            Item item = tile.floor().drops.item;
            unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(tile.worldx(), tile.worldy()), 0.4f);

            if(unit.inventory.canAcceptItem(item) &&
                    Mathf.chance(Timers.delta() * (0.06 - item.hardness * 0.01) * getMinePower())){
                CallEntity.transferItemToUnit(item,
                        tile.worldx() + Mathf.range(tilesize/2f),
                        tile.worldy() + Mathf.range(tilesize/2f),
                        unit);
            }

            if(Mathf.chance(0.06 * Timers.delta())){
                Effects.effect(BlockFx.pulverizeSmall,
                        tile.worldx() + Mathf.range(tilesize/2f),
                        tile.worldy() + Mathf.range(tilesize/2f), 0f, item.color);
            }
        }
    }

    /**Draw placement effects for an entity. This includes mining*/
    default void drawBuilding(Unit unit){
        BuildRequest request;

        synchronized (getPlaceQueue()) {
            if (!isBuilding()) {
                if (getMineTile() != null) {
                    drawMining(unit);
                }
                return;
            }

            request = getCurrentRequest();
        }

        Tile tile = world.tile(request.x, request.y);

        Draw.color(unit.distanceTo(tile) > placeDistance || request.remove ? Palette.remove : Palette.accent);
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

        Fill.circle(px, py, 1.6f + Mathf.absin(Timers.time(), 0.8f, 1.5f));

        Draw.color();
    }

    /**Internal use only.*/
    default void drawMining(Unit unit){
        Tile tile = getMineTile();

        if(tile == null) return;

        float focusLen = 4f + Mathf.absin(Timers.time(), 1.1f, 0.5f);
        float swingScl = 12f, swingMag = tilesize/8f;
        float flashScl = 0.3f;

        float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
        float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

        float ex = tile.worldx() + Mathf.sin(Timers.time() + 48, swingScl, swingMag);
        float ey = tile.worldy() + Mathf.sin(Timers.time() + 48, swingScl + 2f, swingMag);

        Draw.color(Color.LIGHT_GRAY, Color.WHITE, 1f-flashScl + Mathf.absin(Timers.time(), 0.5f, flashScl));
        Shapes.laser("minelaser", "minelaser-end", px, py, ex, ey);

        if(unit instanceof Player && ((Player) unit).isLocal) {
            Draw.color(Palette.accent);
            Lines.poly(tile.worldx(), tile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Timers.time());
        }

        Draw.color();
    }

    /**Class for storing build requests. Can be either a place or remove request.*/
    class BuildRequest {
        public final int x, y, rotation;
        public final Recipe recipe;
        public final boolean remove;

        public boolean requested;
        public TileEntity lastEntity;

        public float progress;

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
            this.recipe = Recipe.getByResult(world.tile(x, y).block());
            this.remove = true;
        }
    }
}
