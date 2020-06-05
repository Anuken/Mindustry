package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.pooling.*;
import mindustry.ai.BaseRegistry.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

public class BaseGenerator{
    private static final Schematic tmpSchem = new Schematic(new Array<>(), new StringMap(), 0, 0);
    private static final Schematic tmpSchem2 = new Schematic(new Array<>(), new StringMap(), 0, 0);
    private static final Vec2 axis = new Vec2(), rotator = new Vec2();

    private final static int range = 180;

    private Tiles tiles;
    private Team team;
    private ObjectMap<Item, OreBlock> ores = new ObjectMap<>();
    private Array<Tile> cores;

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){
        this.tiles = tiles;
        this.team = team;
        this.cores = cores;
        Mathf.random.setSeed(sector.id);

        for(Block block : content.blocks()){
            if(block instanceof OreBlock && block.asFloor().itemDrop != null){
                ores.put(block.asFloor().itemDrop, (OreBlock)block);
            }
        }

        float costBudget = 1000;

        Array<Block> wallsSmall = content.blocks().select(b -> b instanceof Wall && b.size == 1);
        Array<Block> wallsLarge = content.blocks().select(b -> b instanceof Wall && b.size == 2);

        float bracket = 0.1f;
        int wallAngle = 70; //180 for full coverage
        double resourceChance = 0.4;
        double nonResourceChance = 0.0005;
        BasePart coreschem = bases.cores.getFrac(bracket);

        Block wall = wallsSmall.getFrac(bracket), wallLarge = wallsLarge.getFrac(bracket);

        for(Tile tile : cores){
            tile.clearOverlay();
            Schematics.placeLoadout(coreschem.schematic, tile.x, tile.y, team, coreschem.required instanceof Item ? ores.get((Item)coreschem.required) : Blocks.oreCopper);

            //fill core with every type of item (even non-material)
            Tilec entity = tile.entity;
            for(Item item : content.items()){
                entity.items().add(item, entity.block().itemCapacity);
            }
        }

        //random schematics
        pass(tile -> {
            if(!tile.block().alwaysReplace) return;

            if((tile.drop() != null || (tile.floor().liquidDrop != null && Mathf.chance(nonResourceChance * 2))) && Mathf.chance(resourceChance)){
                Array<BasePart> parts = bases.forResource(tile.drop() != null ? tile.drop() : tile.floor().liquidDrop);
                if(!parts.isEmpty()){
                    tryPlace(parts.random(), tile.x, tile.y);
                }
            }else if(Mathf.chance(nonResourceChance)){
                tryPlace(bases.parts.random(), tile.x, tile.y);
            }
        });

        if(wallAngle > 0){

            //small walls
            pass(tile -> {
                if(tile.block().alwaysReplace){
                    boolean any = false;

                    for(Point2 p : Geometry.d8){
                        if(Angles.angleDist(Angles.angle(p.x, p.y), spawn.angleTo(tile)) > wallAngle){
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

            //large walls
            pass(curr -> {
                int walls = 0;
                for(int cx = 0; cx < 2; cx++){
                    for(int cy = 0; cy < 2; cy++){
                        Tile tile = tiles.get(curr.x + cx, curr.y + cy);
                        if(tile == null || tile.block().size != 1 || (tile.block() != wall && !tile.block().alwaysReplace)) return;

                        if(tile.block() == wall){
                            walls ++;
                        }
                    }
                }

                if(walls >= 3){
                    curr.setBlock(wallLarge, team);
                }
            });
        }
    }

    public void postGenerate(){
        for(Tile tile : tiles){
            if(tile.isCenter() && tile.block() instanceof PowerNode){
                tile.entity.placed();
            }
        }
    }

    void pass(Cons<Tile> cons){
        Tile core = cores.first();
        //for(Tile core : cores){
        core.circle(range, (x, y) -> cons.get(tiles.getn(x, y)));
        //}
    }

    boolean tryPlace(BasePart part, int x, int y){
        int rotation = Mathf.range(2);
        axis.set((int)(part.schematic.width / 2f), (int)(part.schematic.height / 2f));
        Schematic result = rotate(part.schematic, rotation);
        int rotdeg = rotation*90;

        rotator.set(part.centerX, part.centerY).rotateAround(axis, rotdeg);
        //bottom left schematic corner
        int cx = x - (int)rotator.x;
        int cy = y - (int)rotator.y;

        for(Stile tile : result.tiles){
            int realX = tile.x + cx, realY = tile.y + cy;
            if(isTaken(tile.block, realX, realY)){
                return false;
            }
        }

        if(part.required instanceof Item){
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){

                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {

                        if(!tiles.getn(ex, ey).floor().isLiquid){
                            tiles.getn(ex, ey).setOverlay(ores.get((Item)part.required));
                        }

                        Tile rand = tiles.getc(ex + Mathf.range(1), ey + Mathf.range(1));
                        if(!rand.floor().isLiquid){
                            //random ores nearby to make it look more natural
                            rand.setOverlay(ores.get((Item)part.required));
                        }
                    });
                }
            }
        }

        Schematics.place(result, cx + result.width/2, cy + result.height/2, team);

        return true;
    }

    boolean isTaken(Block block, int x, int y){
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    if(overlaps(dx + offsetx + x, dy + offsety + y)){
                        return true;
                    }
                }
            }

        }else{
            return overlaps(x, y);
        }

        return false;
    }

    boolean overlaps(int x, int y){
        Tile tile = tiles.get(x, y);

        return tile == null || !tile.block().alwaysReplace || world.getDarkness(x, y) > 0;
    }

    Schematic rotate(Schematic input, int times){
        if(times == 0) return input;

        boolean sign = times > 0;
        for(int i = 0; i < Math.abs(times); i++){
            input = rotated(input, sign);
        }
        return input;
    }

    Schematic rotated(Schematic input, boolean counter){
        int direction = Mathf.sign(counter);
        Schematic schem = input == tmpSchem ? tmpSchem2 : tmpSchem2;
        schem.width = input.width;
        schem.height = input.height;
        Pools.freeAll(schem.tiles);
        schem.tiles.clear();
        for(Stile tile : input.tiles){
            schem.tiles.add(Pools.obtain(Stile.class, Stile::new).set(tile));
        }

        int ox = schem.width/2, oy = schem.height/2;

        schem.tiles.each(req -> {
            req.config = BuildRequest.pointConfig(req.config, p -> {
                int cx = p.x, cy = p.y;
                int lx = cx;

                if(direction >= 0){
                    cx = -cy;
                    cy = lx;
                }else{
                    cx = cy;
                    cy = -lx;
                }
                p.set(cx, cy);
            });

            //rotate actual request, centered on its multiblock position
            float wx = (req.x - ox) * tilesize + req.block.offset(), wy = (req.y - oy) * tilesize + req.block.offset();
            float x = wx;
            if(direction >= 0){
                wx = -wy;
                wy = x;
            }else{
                wx = wy;
                wy = -x;
            }
            req.x = (short)(world.toTile(wx - req.block.offset()) + ox);
            req.y = (short)(world.toTile(wy - req.block.offset()) + oy);
            req.rotation = (byte)Mathf.mod(req.rotation + direction, 4);
        });

        //assign flipped values, since it's rotated
        schem.width = input.height;
        schem.height = input.width;

        return schem;
    }
}
