package mindustry.world;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class Build{
    private static final IntSet tmp = new IntSet();

    @Remote(called = Loc.server)
    public static void beginBreak(@Nullable Unit unit, Team team, int x, int y){
        if(!validBreak(team, x, y)){
            return;
        }

        Tile tile = world.tileBuilding(x, y);
        //this should never happen, but it doesn't hurt to check for links
        float prevPercent = 1f;

        if(tile.build != null){
            prevPercent = tile.build.healthf();
        }

        int rotation = tile.build != null ? tile.build.rotation : 0;
        Block previous = tile.block();

        //instantly deconstruct if necessary
        if(previous.instantDeconstruct){
            ConstructBlock.deconstructFinish(tile, previous, unit);
            return;
        }

        Block sub = ConstructBlock.get(previous.size);

        Seq<Building> prevBuild = new Seq<>(1);
        if(tile.build != null) prevBuild.add(tile.build);

        tile.setBlock(sub, team, rotation);
        var build = (ConstructBuild)tile.build;
        build.setDeconstruct(previous);
        build.prevBuild = prevBuild;
        tile.build.health = tile.build.maxHealth * prevPercent;

        if(unit != null && unit.getControllerName() != null){
            tile.build.lastAccessed = unit.getControllerName();
        }

        Events.fire(new BlockBuildBeginEvent(tile, team, unit, true));
    }

    /** Places a ConstructBlock at this location. */
    @Remote(called = Loc.server)
    public static void beginPlace(@Nullable Unit unit, Block result, Team team, int x, int y, int rotation){
        if(!validPlace(result, team, x, y, rotation)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        //auto-rotate the block to the correct orientation and bail out
        if(tile.team() == team && tile.block == result && tile.build != null && tile.block.quickRotate){
            if(unit != null && unit.getControllerName() != null) tile.build.lastAccessed = unit.getControllerName();
            int previous = tile.build.rotation;
            tile.build.rotation = Mathf.mod(rotation, 4);
            tile.build.updateProximity();
            tile.build.noSleep();
            Fx.rotateBlock.at(tile.build.x, tile.build.y, tile.build.block.size);
            Events.fire(new BuildRotateEvent(tile.build, unit, previous));
            return;
        }

        //break all props in the way
        tile.getLinkedTilesAs(result, out -> {
            if(out.block != Blocks.air && out.block.alwaysReplace){
                out.block.breakEffect.at(out.drawx(), out.drawy(), out.block.size, out.block.mapColor);
                out.remove();
            }
        });

        Block previous = tile.block();
        Block sub = ConstructBlock.get(result.size);
        var prevBuild = new Seq<Building>(9);

        result.beforePlaceBegan(tile, previous);
        tmp.clear();

        tile.getLinkedTilesAs(result, t -> {
            if(t.build != null && t.build.team == team && tmp.add(t.build.id)){
                prevBuild.add(t.build);
            }
        });

        tile.setBlock(sub, team, rotation);

        var build = (ConstructBuild)tile.build;

        build.setConstruct(previous.size == sub.size ? previous : Blocks.air, result);
        build.prevBuild = prevBuild;
        if(unit != null && unit.getControllerName() != null) build.lastAccessed = unit.getControllerName();

        result.placeBegan(tile, previous);

        Events.fire(new BlockBuildBeginEvent(tile, team, unit, false));
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation){
        return validPlace(type, team, x, y, rotation, true);
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation, boolean checkVisible){
        //the wave team can build whatever they want as long as it's visible - banned blocks are not applicable
        if(type == null || (checkVisible && (!type.environmentBuildable() || (!type.isPlaceable() && !(state.rules.waves && team == state.rules.waveTeam && type.isVisible()))))){
            return false;
        }

        if((type.solid || type.solidifes) && Units.anyEntities(x * tilesize + type.offset - type.size*tilesize/2f, y * tilesize + type.offset - type.size*tilesize/2f, type.size * tilesize, type.size*tilesize)){
            return false;
        }

        if(!state.rules.editor){
            //find closest core, if it doesn't match the team, placing is not legal
            if(state.rules.polygonCoreProtection){
                float mindst = Float.MAX_VALUE;
                CoreBuild closest = null;
                for(TeamData data : state.teams.active){
                    for(CoreBuild tile : data.cores){
                        float dst = tile.dst2(x * tilesize + type.offset, y * tilesize + type.offset);
                        if(dst < mindst){
                            closest = tile;
                            mindst = dst;
                        }
                    }
                }
                if(closest != null && closest.team != team){
                    return false;
                }
            }else if(state.teams.anyEnemyCoresWithin(team, x * tilesize + type.offset, y * tilesize + type.offset, state.rules.enemyCoreBuildRadius + tilesize)){
                return false;
            }
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        //campaign darkness check
        if(world.getDarkness(x, y) >= 3){
            return false;
        }

        if(!type.requiresWater && !contactsShallows(tile.x, tile.y, type) && !type.placeableLiquid){
            return false;
        }

        if((type.isFloor() && tile.floor() == type) || (type.isOverlay() && tile.overlay() == type)){
            return false;
        }

        if(!type.canPlaceOn(tile, team, rotation)){
            return false;
        }

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;

                Tile check = world.tile(wx, wy);

                if(
                check == null || //nothing there
                (type.size == 2 && world.getDarkness(wx, wy) >= 3) ||
                (state.rules.staticFog && state.rules.fog && !fogControl.isDiscovered(team, wx, wy)) ||
                (check.floor().isDeep() && !type.floating && !type.requiresWater && !type.placeableLiquid) || //deep water
                (type == check.block() && check.build != null && rotation == check.build.rotation && type.rotate) || //same block, same rotation
                !check.interactable(team) || //cannot interact
                !check.floor().placeableOn || //solid wall
                (!checkVisible && !check.block().alwaysReplace) || //replacing a block that should be replaced (e.g. payload placement)
                    !((type.canReplace(check.block()) || //can replace type
                        (check.build instanceof ConstructBuild build && build.current == type && check.centerX() == tile.x && check.centerY() == tile.y)) && //same type in construction
                    type.bounds(tile.x, tile.y, Tmp.r1).grow(0.01f).contains(check.block.bounds(check.centerX(), check.centerY(), Tmp.r2))) || //no replacement
                (type.requiresWater && check.floor().liquidDrop != Liquids.water) //requires water but none found
                ) return false;
            }
        }

        if(state.rules.placeRangeCheck && !state.isEditor() && getEnemyOverlap(type, team, x, y) != null){
            return false;
        }

        return true;
    }

    public static @Nullable Building getEnemyOverlap(Block block, Team team, int x, int y){
        return indexer.findEnemyTile(team, x * tilesize + block.size, y * tilesize + block.size, block.placeOverlapRange + 4f, p -> true);
    }

    public static boolean contactsGround(int x, int y, Block block){
        if(block.isMultiblock()){
            for(Point2 point : Edges.getEdges(block.size)){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
        }else{
            for(Point2 point : Geometry.d4){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
        }
        return false;
    }

    public static boolean contactsShallows(int x, int y, Block block){
        if(block.isMultiblock()){
            for(Point2 point : block.getInsideEdges()){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isDeep()) return true;
            }

            for(Point2 point : block.getEdges()){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isDeep()) return true;
            }
        }else{
            for(Point2 point : Geometry.d4){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isDeep()) return true;
            }
            Tile tile = world.tile(x, y);
            return tile != null && !tile.floor().isDeep();
        }
        return false;
    }

    /** Returns whether the tile at this position is breakable by this team */
    public static boolean validBreak(Team team, int x, int y){
        Tile tile = world.tile(x, y);
        return tile != null && tile.block().canBreak(tile) && tile.breakable() && tile.interactable(team);
    }
}
