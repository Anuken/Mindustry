package io.anuke.mindustry.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.ValidateException;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entities;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Build {
    private static final Rectangle rect = new Rectangle();
    private static final Rectangle hitrect = new Rectangle();
    private static Array<Tile> tempTiles = new Array<>();

    /**Returns block type that was broken, or null if unsuccesful.*/
    @Remote(targets = Loc.both, forward = true, called = Loc.server, in = In.blocks)
    public static Block breakBlock(Player player, Team team, int x, int y, boolean effect, boolean sound){

        if(Net.server() && !validBreak(team, x, y)){
            throw new ValidateException(player, "An invalid block has been broken.");
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return null;

        Block block = tile.isLinked() ? tile.getLinked().block() : tile.block();

        //todo add break results to core inventory

        if(sound) Effects.sound("break", x * tilesize, y * tilesize);
        if(effect) Effects.effect(Fx.breakBlock, tile.drawx(), tile.drawy(), block.size);

        if(!tile.block().isMultiblock() && !tile.isLinked()){
            tile.setBlock(Blocks.air);
        }else{
            Tile target = tile.isLinked() ? tile.getLinked() : tile;
            Array<Tile> removals = target.getLinkedTiles(tempTiles);
            for(Tile toremove : removals){
                //note that setting a new block automatically unlinks it
                toremove.setBlock(Blocks.air);
            }
        }

        return block;
    }

    /**Places a BuildBlock at this location. Call validPlace first.*/
    @Remote(targets = Loc.both, forward = true, called = Loc.server, in = In.blocks)
    public static void placeBlock(Player player, Team team, int x, int y, Recipe recipe, int rotation){

        if(Net.server() && !validPlace(team, x, y, recipe.result, rotation)){
            throw new ValidateException(player, "An invalid block has been placed.");
        }

        Tile tile = world.tile(x, y);
        Block result = recipe.result;
        Block previous = tile.block();

        //just in case
        if(tile == null) return;

        //remote players only (?)
        if(player.getPlaceQueue().size == 0){
            player.getPlaceQueue().addFirst(new BuildRequest(x, y, rotation, recipe));
        }

        Block sub = Block.getByName("build" + result.size);

        tile.setBlock(sub, rotation);
        tile.<BuildEntity>entity().set(previous, recipe);
        tile.setTeam(team);

        if(result.isMultiblock()){
            int offsetx = -(result.size-1)/2;
            int offsety = -(result.size-1)/2;

            for(int dx = 0; dx < result.size; dx ++){
                for(int dy = 0; dy < result.size; dy ++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!(worldx == x && worldy == y)){
                        Tile toplace = world.tile(worldx, worldy);
                        if(toplace != null) {
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }
    }

    /**Returns whether a tile can be placed at this location by this team.*/
    public static boolean validPlace(Team team, int x, int y, Block type, int rotation) {
        Recipe recipe = Recipe.getByResult(type);

        if (recipe == null) {
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

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;
            for (int dx = 0; dx < type.size; dx++) {
                for (int dy = 0; dy < type.size; dy++) {
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if (other == null || (other.block() != Blocks.air && !other.block().alwaysReplace)  || !other.floor().placeableOn) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return (tile.getTeam() == Team.none || tile.getTeam() == team) && tile.floor().placeableOn
                    && ((type.canReplace(tile.block()) && !(type == tile.block() && rotation == tile.getRotation() && type.rotate)) || tile.block().alwaysReplace || tile.block() == Blocks.air)
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
