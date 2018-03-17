package io.anuke.mindustry.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entities;

import static io.anuke.mindustry.Vars.*;

public class Placement {
    private static final Rectangle rect = new Rectangle();
    private static Array<Tile> tempTiles = new Array<>();

    /**Returns block type that was broken, or null if unsuccesful.*/
    public static Block breakBlock(Team team, int x, int y, boolean effect, boolean sound){
        Tile tile = world.tile(x, y);

        if(tile == null) return null;

        Block block = tile.isLinked() ? tile.getLinked().block() : tile.block();
        Recipe result = Recipes.getByResult(block);

        if(result != null){
            for(ItemStack stack : result.requirements){
                state.inventory.addItem(stack.item, (int)(stack.amount * breakDropAmount));
            }
        }

        if(tile.block().drops != null){
            state.inventory.addItem(tile.block().drops.item, tile.block().drops.amount);
        }

        if(sound) threads.run(() -> Effects.sound("break", x * tilesize, y * tilesize));

        if(!tile.block().isMultiblock() && !tile.isLinked()){
            tile.setBlock(Blocks.air);
            if(effect) Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
        }else{
            Tile target = tile.isLinked() ? tile.getLinked() : tile;
            Array<Tile> removals = target.getLinkedTiles(tempTiles);
            for(Tile toremove : removals){
                //note that setting a new block automatically unlinks it
                toremove.setBlock(Blocks.air);
                if(effect) Effects.effect(Fx.breakBlock, toremove.worldx(), toremove.worldy());
            }
        }

        return block;
    }

    public static void placeBlock(Team team, int x, int y, Block result, int rotation, boolean effects, boolean sound){
        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        tile.setBlock(result, rotation);
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

                    if(effects) Effects.effect(Fx.place, worldx * tilesize, worldy * tilesize);
                }
            }
        }else if(effects) Effects.effect(Fx.place, x * tilesize, y * tilesize);

        if(effects && sound) threads.run(() -> Effects.sound("place", x * tilesize, y * tilesize));
    }

    public static boolean validPlace(Team team, int x, int y, Block type){
        for(int i = 0; i < world.getSpawns().size; i ++){
            SpawnPoint spawn = world.getSpawns().get(i);
            if(Vector2.dst(x * tilesize, y * tilesize, spawn.start.worldx(), spawn.start.worldy()) < enemyspawnspace){
                return false;
            }
        }

        Recipe recipe = Recipes.getByResult(type);

        if(recipe == null || !state.inventory.hasItems(recipe.requirements)){
            return false;
        }

        rect.setSize(type.size * tilesize, type.size * tilesize);
        Vector2 offset = type.getPlaceOffset();
        rect.setCenter(offset.x + x * tilesize, offset.y + y * tilesize);

        synchronized (Entities.entityLock) {
            rect.setSize(tilesize*2f).setCenter(x*tilesize, y*tilesize);
            boolean[] result = {false};

            Units.getNearby(rect, e -> {
                if (e == null) return; //not sure why this happens?
                Rectangle rect = e.hitbox.getRect(e.x, e.y);

                if (Placement.rect.overlaps(rect)) {
                    result[0] = true;
                }
            });

            if(result[0]) return false;
        }

        if(type.solid || type.solidifes) {
            for (Player player : playerGroup.all()) {
                if (!player.mech.flying && rect.overlaps(player.hitbox.getRect(player.x, player.y))) {
                    return false;
                }
            }
        }

        Tile tile = world.tile(x, y);

        if(tile == null || (isSpawnPoint(tile) && (type.solidifes || type.solid))) return false;

        if(type.isMultiblock()){
            int offsetx = -(type.size-1)/2;
            int offsety = -(type.size-1)/2;
            for(int dx = 0; dx < type.size; dx ++){
                for(int dy = 0; dy < type.size; dy ++){
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if(other == null || (other.block() != Blocks.air && !other.block().alwaysReplace) || isSpawnPoint(other)){
                        return false;
                    }
                }
            }
            return true;
        }else {
            return tile.block() != type && (tile.getTeam() == Team.none || tile.getTeam() == team)
                    && (type.canReplace(tile.block()) || tile.block().alwaysReplace)
                    && tile.block().isMultiblock() == type.isMultiblock() || tile.block() == Blocks.air;
        }
    }

    public static boolean isSpawnPoint(Tile tile){
        return tile != null && tile.x == world.getCore().x && tile.y == world.getCore().y - 2;
    }

    public static boolean validBreak(Team team, int x, int y){
        Tile tile = world.tile(x, y);

        if(tile == null || tile.block() == ProductionBlocks.core) return false;

        if(tile.isLinked() && tile.getLinked().block() == ProductionBlocks.core){
            return false;
        }

        return tile.breakable() && (tile.getTeam() == Team.none || tile.getTeam() == team);
    }
}
