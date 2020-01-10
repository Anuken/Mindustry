package mindustry.world;

import mindustry.annotations.Annotations.Loc;
import mindustry.annotations.Annotations.Remote;
import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.*;
import mindustry.content.Blocks;
import mindustry.entities.Units;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.Team;
import mindustry.world.blocks.BuildBlock;
import mindustry.world.blocks.BuildBlock.BuildEntity;

import static mindustry.Vars.*;

public class Build{

    /** Returns block type that was broken, or null if unsuccesful. */
    @Remote(called = Loc.server)
    public static void beginBreak(Team team, int x, int y){
        if(!validBreak(team, x, y)){
            return;
        }

        Tile tile = world.ltile(x, y);
        float prevPercent = 1f;

        //just in case
        if(tile == null) return;

        if(tile.entity != null){
            prevPercent = tile.entity.healthf();
        }

        int rotation = tile.rotation();
        Block previous = tile.block();
        Block sub = BuildBlock.get(previous.size);

        tile.set(sub, team, rotation);
        tile.<BuildEntity>ent().setDeconstruct(previous);
        tile.entity.health = tile.entity.maxHealth() * prevPercent;

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

        tile.set(sub, team, rotation);
        tile.<BuildEntity>ent().setConstruct(previous, result);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, false)));
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation){
        if(type == null || !type.isVisible() || type.isHidden()){
            return false;
        }

        if(state.rules.bannedBlocks.contains(type) && !(state.rules.waves && team == state.rules.waveTeam)){
            return false;
        }

        if((type.solid || type.solidifes) && Units.anyEntities(x * tilesize + type.offset() - type.size*tilesize/2f, y * tilesize + type.offset() - type.size*tilesize/2f, type.size * tilesize, type.size*tilesize)){
            return false;
        }

        if(state.teams.eachEnemyCore(team, core -> Mathf.dst(x * tilesize + type.offset(), y * tilesize + type.offset(), core.x, core.y) < state.rules.enemyCoreBuildRadius + type.size * tilesize / 2f)){
            return false;
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        if(type.isMultiblock()){
            if(type.canReplace(tile.block()) && tile.block().size == type.size && type.canPlaceOn(tile) && tile.interactable(team)){
                return true;
            }

            if(!contactsGround(tile.x, tile.y, type)){
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
                    if(other == null || (other.block() != Blocks.air && !other.block().alwaysReplace) ||
                    !other.floor().placeableOn ||
                    (other.floor().isDeep() && !type.floating)){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return tile.interactable(team)
            && contactsGround(tile.x, tile.y, type)
            && (!tile.floor().isDeep() || type.floating)
            && tile.floor().placeableOn
            && ((type.canReplace(tile.block())
            && !(type == tile.block() && rotation == tile.rotation() && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
            && tile.block().isMultiblock() == type.isMultiblock() && type.canPlaceOn(tile);
        }
    }

    private static boolean contactsGround(int x, int y, Block block){
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
        Tile tile = world.ltile(x, y);
        return tile != null && tile.block().canBreak(tile) && tile.breakable() && tile.interactable(team);
    }
}
