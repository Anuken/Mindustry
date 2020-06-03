package mindustry.world;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
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

        Tile tile = world.tilec(x, y);
        //this should never happen, but it doesn't hurt to check for links
        float prevPercent = 1f;

        if(tile.entity != null){
            prevPercent = tile.entity.healthf();
        }

        int rotation = tile.rotation();
        Block previous = tile.block();
        Block sub = BuildBlock.get(previous.size);

        tile.setBlock(sub, team, rotation);
        tile.<BuildEntity>ent().setDeconstruct(previous);
        tile.entity.health(tile.entity.maxHealth() * prevPercent);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, true)));
    }

    /** Places a BuildBlock at this location. */
    @Remote(called = Loc.server)
    public static void beginPlace(Team team, int x, int y, Block result, int rotation){
        if(!validPlace(team, x, y, result, rotation)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        Block previous = tile.block();
        Block sub = BuildBlock.get(result.size);

        tile.setBlock(sub, team, rotation);
        tile.<BuildEntity>ent().setConstruct(previous, result);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, false)));
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation){
        //the wave team can build whatever they want as long as it's visible - banned blocks are not applicable
        if(type == null || (!type.isPlaceable() && !(state.rules.waves && team == state.rules.waveTeam && type.isVisible()))){
            return false;
        }

        if((type.solid || type.solidifes) && Units.anyEntities(x * tilesize + type.offset() - type.size*tilesize/2f, y * tilesize + type.offset() - type.size*tilesize/2f, type.size * tilesize, type.size*tilesize)){
            return false;
        }

        if(state.teams.eachEnemyCore(team, core -> Mathf.dst(x * tilesize + type.offset(), y * tilesize + type.offset(), core.x(), core.y()) < state.rules.enemyCoreBuildRadius + type.size * tilesize / 2f)){
            return false;
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        //ca check
        if(world.getDarkness(x, y) >= 3){
            return false;
        }

        if(type.isMultiblock()){
            if((type.canReplace(tile.block()) || (tile.block instanceof BuildBlock && tile.<BuildEntity>ent().cblock == type)) && tile.block().size == type.size && type.canPlaceOn(tile) && tile.interactable(team)){
                return true;
            }

            //TODO should water blocks be placeable here?
            if(/*!type.requiresWater && */!contactsShallows(tile.x, tile.y, type)){
                return false;
            }

            if(!type.canPlaceOn(tile)){
                return false;
            }

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;
            for(int dx = 0; dx < type.size; dx++){
                for(int dy = 0; dy < type.size; dy++){
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if(
                        other == null ||
                        (other.block() != Blocks.air && !other.block().alwaysReplace) ||
                        !other.floor().placeableOn ||
                        (other.floor().isDeep() && !type.floating && !type.requiresWater) ||
                        (type.requiresWater && tile.floor().liquidDrop != Liquids.water)
                    ){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return tile.interactable(team)
                && contactsShallows(tile.x, tile.y, type)
                && (!tile.floor().isDeep() || type.floating || type.requiresWater)
                && tile.floor().placeableOn
                && (!type.requiresWater || tile.floor().liquidDrop == Liquids.water)
                && (((type.canReplace(tile.block()) || (tile.block instanceof BuildBlock && tile.<BuildEntity>ent().cblock == type))
                && !(type == tile.block() && rotation == tile.rotation() && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
                && tile.block().isMultiblock() == type.isMultiblock() && type.canPlaceOn(tile);
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
