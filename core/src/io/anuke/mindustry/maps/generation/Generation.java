package io.anuke.mindustry.maps.generation;

import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.Drill;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SeedRandom;

public class Generation{
    public final Sector sector;
    public final Tile[][] tiles;
    public final int width, height;
    public final SeedRandom random;

    public Generation(Sector sector, Tile[][] tiles, int width, int height, SeedRandom random){
        this.sector = sector;
        this.tiles = tiles;
        this.width = width;
        this.height = height;
        this.random = random;
    }

    Tile tile(int x, int y){
        if(!Mathf.inBounds(x, y, tiles)){
            return null;
        }
        return tiles[x][y];
    }

    //TODO implement
    Item drillItem(int x, int y, Drill block){
        if(block.isMultiblock()){
            Item result = null;
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!Mathf.inBounds(worldx, worldy, tiles)){
                        return null;
                    }

                    if(!block.isValid(tiles[worldx][worldy]) || tiles[worldx][worldy].floor().drops == null) continue;

                    Item drop = tiles[worldx][worldy].floor().drops.item;

                    if(result == null || drop.id < result.id){
                        result = drop;
                    }

                }
            }
            return result;
        }else{
            return tiles[x][y].floor().drops == null ? null : tiles[x][y].floor().drops.item;
        }
    }



    public boolean canPlace(int x, int y, Block block){
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!Mathf.inBounds(worldx, worldy, tiles) || !tiles[worldx][worldy].block().alwaysReplace){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return tiles[x][y].block().alwaysReplace;
        }
    }

    public void setBlock(int x, int y, Block block, Team team){
        tiles[x][y].setBlock(block, team);
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!(worldx == x && worldy == y) && Mathf.inBounds(worldx, worldy, tiles)){
                        Tile toplace = tiles[worldx][worldy];
                        if(toplace != null){
                            toplace.setLinked((byte) (dx + offsetx), (byte) (dy + offsety));
                            toplace.setTeam(team);
                        }
                    }
                }
            }
        }
    }
}
