package io.anuke.mindustry.world;

import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.EventType.BlockBuildEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.entities.Entities;

import static io.anuke.mindustry.Vars.*;

public class Build {
    private static final Rectangle rect = new Rectangle();
    private static final Rectangle hitrect = new Rectangle();

    /**Returns block type that was broken, or null if unsuccesful.*/
    //@Remote(targets = Loc.both, forward = true, called = Loc.server, in = In.blocks)
    public static void beginBreak(Team team, int x, int y){
        if(!validBreak(team, x, y)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        tile = tile.target();

        Block previous = tile.block();

        Block sub = Block.getByName("build" + previous.size);

        tile.setBlock(sub);
        tile.<BuildEntity>entity().setDeconstruct(previous);
        tile.setTeam(team);

        if (previous.isMultiblock()) {
            int offsetx = -(previous.size - 1) / 2;
            int offsety = -(previous.size - 1) / 2;

            for (int dx = 0; dx < previous.size; dx++) {
                for (int dy = 0; dy < previous.size; dy++) {
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if (!(worldx == x && worldy == y)) {
                        Tile toplace = world.tile(worldx, worldy);
                        if (toplace != null) {
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }

    }

    /**Places a BuildBlock at this location.*/
    //@Remote(targets = Loc.both, forward = true, called = Loc.server, in = In.blocks)
    public static void beginPlace(Team team, int x, int y, Recipe recipe, int rotation){
        if(!validPlace(team, x, y, recipe.result, rotation)){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        Block result = recipe.result;
        Block previous = tile.block();

        Block sub = Block.getByName("build" + result.size);

        tile.setBlock(sub, rotation);
        tile.<BuildEntity>entity().setConstruct(previous, recipe);
        tile.setTeam(team);

        if (result.isMultiblock()) {
            int offsetx = -(result.size - 1) / 2;
            int offsety = -(result.size - 1) / 2;

            for (int dx = 0; dx < result.size; dx++) {
                for (int dy = 0; dy < result.size; dy++) {
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if (!(worldx == x && worldy == y)) {
                        Tile toplace = world.tile(worldx, worldy);
                        if (toplace != null) {
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }


        threads.runDelay(() -> Events.fire(BlockBuildEvent.class, team, tile));
    }

    /**Returns whether a tile can be placed at this location by this team.*/
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation) {
        Recipe recipe = Recipe.getByResult(type);

        if (recipe == null || (recipe.debugOnly && !debug)) {
            return false;
        }

        rect.setSize(type.size * tilesize, type.size * tilesize);
        rect.setCenter(type.offset() + x * tilesize, type.offset() + y * tilesize);

        if (type.solid || type.solidifes) {
            synchronized (Entities.entityLock) {
                try {

                    rect.setSize(tilesize * type.size).setCenter(x * tilesize + type.offset(), y * tilesize + type.offset());
                    boolean[] result = {false};

                    Units.getNearby(rect, e -> {
                        if (e == null) return; //not sure why this happens?
                        e.getHitbox(hitrect);

                        if (rect.overlaps(hitrect) && !e.isFlying()) {
                            result[0] = true;
                        }
                    });

                    if (result[0]) return false;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        Tile tile = world.tile(x, y);

        if (tile == null) return false;

        if (type.isMultiblock()) {
            if (type.canReplace(tile.block()) && tile.block().size == type.size) {
                return true;
            }

            if(!type.canPlaceOn(tile)){
                return false;
            }

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;
            for (int dx = 0; dx < type.size; dx++) {
                for (int dy = 0; dy < type.size; dy++) {
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if (other == null || (other.block() != Blocks.air && !other.block().alwaysReplace)
                             || other.cliffs != 0  || !other.floor().placeableOn ||
                            (tile.floor().liquidDrop != null && !type.floating)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return (tile.getTeam() == Team.none || tile.getTeam() == team)
                    && (tile.floor().liquidDrop == null || type.floating)
                    && tile.floor().placeableOn && tile.cliffs == 0
                    && ((type.canReplace(tile.block())
                    && !(type == tile.block() && rotation == tile.getRotation() && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
                    && tile.block().isMultiblock() == type.isMultiblock() && type.canPlaceOn(tile);
        }
    }

    /**Returns whether the tile at this position is breakable by this team*/
    public static boolean validBreak(Team team, int x, int y) {
        Tile tile = world.tile(x, y);

        return tile != null && !tile.block().unbreakable
                && (!tile.isLinked() || !tile.getLinked().block().unbreakable) && tile.breakable() && (tile.getTeam() == Team.none || tile.getTeam() == team);
    }
}
