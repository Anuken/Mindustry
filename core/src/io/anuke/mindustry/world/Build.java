package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.EventType.BlockBuildBeginEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.Geometry;

import static io.anuke.mindustry.Vars.*;

public class Build{
    private static final Rectangle rect = new Rectangle();

    /**Returns block type that was broken, or null if unsuccesful.*/
    public static void beginBreak(Team team, int x, int y){
        if(!validBreak(team, x, y)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        tile = tile.target();

        Block previous = tile.block();

        Block sub = content.getByName(ContentType.block, "build" + previous.size);

        tile.setBlock(sub);
        tile.<BuildEntity>entity().setDeconstruct(previous);
        tile.setTeam(team);

        if(previous.isMultiblock()){
            int offsetx = -(previous.size - 1) / 2;
            int offsety = -(previous.size - 1) / 2;

            for(int dx = 0; dx < previous.size; dx++){
                for(int dy = 0; dy < previous.size; dy++){
                    int worldx = dx + offsetx + tile.x;
                    int worldy = dy + offsety + tile.y;
                    if(!(worldx == tile.x && worldy == tile.y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null){
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }

        Tile ftile = tile;
        threads.runDelay(() -> Events.fire(new BlockBuildBeginEvent(ftile, team, true)));
    }

    /**Places a BuildBlock at this location.*/
    public static void beginPlace(Team team, int x, int y, Recipe recipe, int rotation){
        if(!validPlace(team, x, y, recipe.result, rotation)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        Block result = recipe.result;
        Block previous = tile.block();

        Block sub = content.getByName(ContentType.block, "build" + result.size);

        tile.setBlock(sub, rotation);
        tile.<BuildEntity>entity().setConstruct(previous, recipe);
        tile.setTeam(team);

        if(result.isMultiblock()){
            int offsetx = -(result.size - 1) / 2;
            int offsety = -(result.size - 1) / 2;

            for(int dx = 0; dx < result.size; dx++){
                for(int dy = 0; dy < result.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!(worldx == x && worldy == y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null){
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }

        threads.runDelay(() -> Events.fire(new BlockBuildBeginEvent(tile, team, false)));
    }

    /**Returns whether a tile can be placed at this location by this team.*/
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation){
        Recipe recipe = Recipe.getByResult(type);

        if(recipe == null || (recipe.mode != null && recipe.mode != state.mode)){
            return false;
        }

        if((type.solid || type.solidifes) &&
            Units.anyEntities(rect.setSize(tilesize * type.size).setCenter(x * tilesize + type.offset(), y * tilesize + type.offset()))){
            return false;
        }

        //check for enemy cores
        for(Team enemy : state.teams.enemiesOf(team)){
            for(Tile core : state.teams.get(enemy).cores){
                if(Vector2.dst(x*tilesize + type.offset(), y*tilesize + type.offset(), core.drawx(), core.drawy()) < state.mode.enemyCoreBuildRadius + type.size*tilesize/2f){
                    return false;
                }
            }
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;

        if(type.isMultiblock()){
            if(type.canReplace(tile.block()) && tile.block().size == type.size && type.canPlaceOn(tile)){
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
                    if(other == null || (other.block() != Blocks.air && !other.block().alwaysReplace)
                            || other.hasCliffs() || !other.floor().placeableOn ||
                            (other.floor().isLiquid && !type.floating)){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return (tile.getTeam() == Team.none || tile.getTeam() == team)
                    && contactsGround(tile.x, tile.y, type)
                    && (!tile.floor().isLiquid || type.floating)
                    && tile.floor().placeableOn && !tile.hasCliffs()
                    && ((type.canReplace(tile.block())
                    && !(type == tile.block() && rotation == tile.getRotation() && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
                    && tile.block().isMultiblock() == type.isMultiblock() && type.canPlaceOn(tile);
        }
    }

    private static boolean contactsGround(int x, int y, Block block){
        if(block.isMultiblock()){
            for(GridPoint2 point : Edges.getInsideEdges(block.size)){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }

            for(GridPoint2 point : Edges.getEdges(block.size)){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
        }else{
            for(GridPoint2 point : Geometry.d4){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
            return world.tile(x, y) != null && !world.tile(x, y).floor().isLiquid;
        }
        return false;
    }

    /**Returns whether the tile at this position is breakable by this team*/
    public static boolean validBreak(Team team, int x, int y){
        Tile tile = world.tile(x, y);
        if(tile != null) tile = tile.target();

        return tile != null && tile.block().canBreak(tile) && tile.breakable() && (!tile.block().synthetic() || tile.getTeam() == team);
    }
}
