package io.anuke.mindustry.world;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.EventType.BlockBuildBeginEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;

import static io.anuke.mindustry.Vars.*;

public class Build{
    private static final Rectangle rect = new Rectangle();

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

        world.setBlock(tile, sub, team, rotation);
        tile.<BuildEntity>entity().setDeconstruct(previous);
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

        world.setBlock(tile, sub, team, rotation);
        tile.<BuildEntity>entity().setConstruct(previous, result);

        Core.app.post(() -> Events.fire(new BlockBuildBeginEvent(tile, team, false)));
    }

    /** Returns whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation){
        if(type == null || !type.isVisible() || type.isHidden()){
            return false;
        }

        if(state.rules.bannedBlocks.contains(type) && !(state.rules.waves && team == waveTeam)){
            return false;
        }

        if((type.solid || type.solidifes) && Units.anyEntities(x * tilesize + type.offset() - type.size*tilesize/2f, y * tilesize + type.offset() - type.size*tilesize/2f, type.size * tilesize, type.size*tilesize)){
            return false;
        }

        //check for enemy cores
        for(Team enemy : state.teams.enemiesOf(team)){
            for(Tile core : state.teams.get(enemy).cores){
                if(Mathf.dst(x * tilesize + type.offset(), y * tilesize + type.offset(), core.drawx(), core.drawy()) < state.rules.enemyCoreBuildRadius + type.size * tilesize / 2f){
                    return false;
                }
            }
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
