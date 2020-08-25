package mindustry.world;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.BuildBlock.*;

import static mindustry.Vars.*;

public class Build{

    @Remote(called = Loc.server)
    public static void beginBreak(Team team, int x, int y){
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
        Block sub = BuildBlock.get(previous.size);

        tile.setBlock(sub, team, rotation);
        tile.<BuildEntity>bc().setDeconstruct(previous);
        tile.build.health(tile.build.maxHealth() * prevPercent);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, true)));
    }

    /** Places a BuildBlock at this location. */
    @Remote(called = Loc.server)
    public static void beginPlace(Block result, Team team, int x, int y, int rotation){
        if(!validPlace(result, team, x, y, rotation)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        Block previous = tile.block();
        Block sub = BuildBlock.get(result.size);

        result.beforePlaceBegan(tile, previous);

        tile.setBlock(sub, team, rotation);
        tile.<BuildEntity>bc().setConstruct(previous, result);

        result.placeBegan(tile, previous);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, false)));
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation){
        //the wave team can build whatever they want as long as it's visible - banned blocks are not applicable
        if(type == null || (!type.isPlaceable() && !(state.rules.waves && team == state.rules.waveTeam && type.isVisible()))){
            return false;
        }

        if((type.solid || type.solidifes) && Units.anyEntities(x * tilesize + type.offset - type.size*tilesize/2f, y * tilesize + type.offset - type.size*tilesize/2f, type.size * tilesize, type.size*tilesize)){
            return false;
        }

        if(state.teams.eachEnemyCore(team, core -> Mathf.dst(x * tilesize + type.offset, y * tilesize + type.offset, core.x, core.y) < state.rules.enemyCoreBuildRadius + type.size * tilesize / 2f)){
            return false;
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        //campaign darkness check
        if(world.getDarkness(x, y) >= 3){
            return false;
        }

        if(type.isMultiblock()){
            if((type.canReplace(tile.block()) || (tile.block instanceof BuildBlock && tile.<BuildEntity>bc().cblock == type)) &&
                type.canPlaceOn(tile, team) && tile.interactable(team)){

                //if the block can be replaced but the sizes differ, check all the spaces around the block to make sure it can fit
                if(type.size != tile.block().size){
                    int offsetx = -(type.size - 1) / 2;
                    int offsety = -(type.size - 1) / 2;

                    //this does not check *all* the conditions for placeability yet
                    for(int dx = 0; dx < type.size; dx++){
                        for(int dy = 0; dy < type.size; dy++){
                            int wx = dx + offsetx + x, wy = dy + offsety + y;

                            Tile check = world.tile(wx, wy);
                            if(check == null || (!check.block.alwaysReplace && check.block != tile.block)) return false;
                        }
                    }
                }

                //make sure that the new block can fit the old one
                return type.bounds(x, y, Tmp.r1).grow(0.01f).contains(tile.block.bounds(tile.centerX(), tile.centerY(), Tmp.r2));
            }

            if(!type.requiresWater && !contactsShallows(tile.x, tile.y, type) && !type.placeableLiquid){
                return false;
            }

            if(!type.canPlaceOn(tile, team)){
                return false;
            }

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;
            for(int dx = 0; dx < type.size; dx++){
                for(int dy = 0; dy < type.size; dy++){
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if(
                        other == null ||
                        !other.block().alwaysReplace ||
                        !other.floor().placeableOn ||
                        (other.floor().isDeep() && !type.floating && !type.requiresWater && !type.placeableLiquid) ||
                        (type.requiresWater && tile.floor().liquidDrop != Liquids.water)
                    ){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return tile.interactable(team)
                && (contactsShallows(tile.x, tile.y, type) || type.requiresWater || type.placeableLiquid)
                && (!tile.floor().isDeep() || type.floating || type.requiresWater || type.placeableLiquid)
                && tile.floor().placeableOn
                && (!type.requiresWater || tile.floor().liquidDrop == Liquids.water)
                && (((type.canReplace(tile.block()) || (tile.block instanceof BuildBlock && tile.<BuildEntity>bc().cblock == type))
                && !(type == tile.block() && (tile.build != null && rotation == tile.build.rotation) && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
                && tile.block().isMultiblock() == type.isMultiblock() && type.canPlaceOn(tile, team);
        }
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
            for(Point2 point : Edges.getInsideEdges(block.size)){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isDeep()) return true;
            }

            for(Point2 point : Edges.getEdges(block.size)){
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
