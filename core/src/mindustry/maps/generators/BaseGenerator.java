package mindustry.maps.generators;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.BaseRegistry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

public class BaseGenerator{
    private Tiles tiles;
    private Team team;
    private ObjectMap<Item, OreBlock> ores = new ObjectMap<>();

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){
        this.tiles = tiles;
        this.team = team;

        for(Block block : content.blocks()){
            if(block instanceof OreBlock && block.asFloor().itemDrop != null){
                ores.put(block.asFloor().itemDrop, (OreBlock)block);
            }
        }

        Array<Block> wallsSmall = content.blocks().select(b -> b instanceof Wall && b.size == 1);
        Array<Block> wallsLarge = content.blocks().select(b -> b instanceof Wall && b.size == 2);

        float bracket = 0.1f;
        int range = 200;
        int wallAngle = 180;
        BasePart coreschem = bases.cores.getFrac(bracket);

        Block wall = wallsSmall.getFrac(bracket), wallLarge = wallsLarge.getFrac(bracket);

        //TODO random flipping and rotation

        for(Tile tile : cores){
            tile.clearOverlay();
            Schematics.placeLoadout(coreschem.schematic, tile.x, tile.y, team, coreschem.requiredItem == null ? Blocks.oreCopper : ores.get(coreschem.requiredItem));

            //fill core with every type of item (even non-material)
            Tilec entity = tile.entity;
            for(Item item : content.items()){
                entity.items().add(item, entity.block().itemCapacity);
            }
        }

        //first pass: random schematics
        for(Tile core : cores){
            core.circle(range, (x, y) -> {
                Tile tile = tiles.getn(x, y);
                if(tile.overlay().itemDrop != null){
                    Array<BasePart> parts = bases.forItem(tile.overlay().itemDrop);
                    if(!parts.isEmpty()){
                        tryPlace(parts.random(), x, y);
                    }
                }
            });
        }

        //second pass: small walls
        for(Tile core : cores){
            core.circle(range, (x, y) -> {
                Tile tile = tiles.getn(x, y);
                if(tile.block().alwaysReplace){
                    boolean any = false;

                    for(Point2 p : Geometry.d8){
                        if(Angles.angleDist(Angles.angle(p.x, p.y), spawn.angleTo(core)) > wallAngle){
                            continue;
                        }

                        Tile o = tiles.get(tile.x + p.x, tile.y + p.y);
                        if(o != null && o.team() == team && !(o.block() instanceof Wall)){
                            any = true;
                            break;
                        }
                    }

                    if(any){
                        tile.setBlock(wall, team);
                    }
                }
            });
        }

        //third pass: large walls
        for(Tile core : cores){
            core.circle(range, (x, y) -> {
                int walls = 0;
                for(int cx = 0; cx < 2; cx++){
                    for(int cy = 0; cy < 2; cy++){
                        Tile tile = tiles.get(x + cx, y + cy);
                        if(tile == null || tile.block().size != 1 || (tile.block() != wall && !tile.block().alwaysReplace)) return;

                        if(tile.block() == wall){
                            walls ++;
                        }
                    }
                }

                if(walls >= 3){
                    tiles.getn(x, y).setBlock(wallLarge, team);
                }
            });
        }
    }

    boolean tryPlace(BasePart part, int x, int y){
        int cx = x - part.schematic.width/2, cy = y - part.schematic.height/2;
        for(int rx = cx; rx <= cx + part.schematic.width; rx++){
            for(int ry = cy; ry <= cy + part.schematic.height; ry++){
                Tile tile = tiles.get(rx, ry);
                if(tile == null || ((!tile.block().alwaysReplace || world.getDarkness(rx, ry) > 0) && part.occupied.get(rx - cx, ry - cy))){
                    return false;
                }
            }
        }

        if(part.requiredItem != null){
            for(Stile tile : part.schematic.tiles){
                if(tile.block instanceof Drill){
                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {
                        tiles.getn(ex, ey).setOverlay(ores.get(part.requiredItem));
                    });
                }
            }
        }

        Schematics.place(part.schematic, x, y, team);

        return true;
    }
}
