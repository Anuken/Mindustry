package mindustry.maps.generators;

import arc.func.*;
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
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BaseGenerator{
    private static final Vec2 axis = new Vec2(), rotator = new Vec2();

    private static final int range = 180;

    private Tiles tiles;
    private Team team;
    private ObjectMap<Item, OreBlock> ores = new ObjectMap<>();
    private ObjectMap<Item, Floor> oreFloors = new ObjectMap<>();
    private Seq<Tile> cores;

    public void generate(Tiles tiles, Seq<Tile> cores, Tile spawn, Team team, Sector sector, float difficulty){
        this.tiles = tiles;
        this.team = team;
        this.cores = cores;

        //don't generate bases when there are no loaded schematics
        if(bases.cores.isEmpty()) return;

        Mathf.rand.setSeed(sector.id);

        for(Block block : content.blocks()){
            if(block instanceof OreBlock && block.asFloor().itemDrop != null){
                ores.put(block.asFloor().itemDrop, (OreBlock)block);
            }else if(block.isFloor() && block.asFloor().itemDrop != null && !oreFloors.containsKey(block.asFloor().itemDrop)){
                oreFloors.put(block.asFloor().itemDrop, block.asFloor());
            }
        }

        //TODO limit base size
        float costBudget = 1000;

        Seq<Block> wallsSmall = content.blocks().select(b -> b instanceof Wall && b.size == 1 && b.buildVisibility == BuildVisibility.shown && !(b instanceof Door));
        Seq<Block> wallsLarge = content.blocks().select(b -> b instanceof Wall && b.size == 2 && b.buildVisibility == BuildVisibility.shown && !(b instanceof Door));

        //sort by cost for correct fraction
        wallsSmall.sort(b -> b.buildCost);
        wallsLarge.sort(b -> b.buildCost);

        //TODO proper difficulty selection
        float bracket = difficulty;
        float bracketRange = 0.2f;
        int wallAngle = 70; //180 for full coverage
        double resourceChance = 0.5;
        double nonResourceChance = 0.0005;
        BasePart coreschem = bases.cores.getFrac(bracket);

        Block wall = wallsSmall.getFrac(bracket), wallLarge = wallsLarge.getFrac(bracket);

        for(Tile tile : cores){
            tile.clearOverlay();
            Schematics.placeLoadout(coreschem.schematic, tile.x, tile.y, team, coreschem.required instanceof Item ? ores.get((Item)coreschem.required) : Blocks.oreCopper);

            //fill core with every type of item (even non-material)
            Building entity = tile.build;
            for(Item item : content.items()){
                entity.items.add(item, entity.block.itemCapacity);
            }
        }

        //random schematics
        pass(tile -> {
            if(!tile.block().alwaysReplace) return;

            if(((tile.overlay().asFloor().itemDrop != null || (tile.drop() != null && Mathf.chance(nonResourceChance)))
                || (tile.floor().liquidDrop != null && Mathf.chance(nonResourceChance * 2))) && Mathf.chance(resourceChance)){
                Seq<BasePart> parts = bases.forResource(tile.drop() != null ? tile.drop() : tile.floor().liquidDrop);
                if(!parts.isEmpty()){
                    tryPlace(parts.getFrac(bracket + Mathf.range(bracketRange)), tile.x, tile.y);
                }
            }else if(Mathf.chance(nonResourceChance)){
                tryPlace(bases.parts.getFrac(bracket + Mathf.range(bracketRange)), tile.x, tile.y);
            }
        });

        //replace walls with the correct type (disabled)
        if(false)
        pass(tile -> {
            if(tile.block() instanceof Wall && tile.team() == team && tile.block() != wall && tile.block() != wallLarge){
                tile.setBlock(tile.block().size == 2 ? wallLarge : wall, team);
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
        if(tiles == null) return;

        for(Tile tile : tiles){
            if(tile.isCenter() && tile.block() instanceof PowerNode){
                tile.build.placed();
            }
        }
    }

    void pass(Cons<Tile> cons){
        Tile core = cores.first();
        core.circle(range, (x, y) -> cons.get(tiles.getn(x, y)));
    }

    boolean tryPlace(BasePart part, int x, int y){
        int rotation = Mathf.range(2);
        axis.set((int)(part.schematic.width / 2f), (int)(part.schematic.height / 2f));
        Schematic result = Schematics.rotate(part.schematic, rotation);
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
            Item item = (Item)part.required;
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){

                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {

                        if(!tiles.getn(ex, ey).floor().isLiquid){
                            set(tiles.getn(ex, ey), item);
                        }

                        Tile rand = tiles.getc(ex + Mathf.range(1), ey + Mathf.range(1));
                        if(!rand.floor().isLiquid){
                            //random ores nearby to make it look more natural
                            set(rand, item);
                        }
                    });
                }
            }
        }

        Schematics.place(result, cx + result.width/2, cy + result.height/2, team);

        //fill drills with items after placing
        if(part.required instanceof Item){
            Item item = (Item)part.required;
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){

                    Building build = world.tile(tile.x + cx, tile.y + cy).build;

                    if(build != null){
                        build.items.add(item, build.block.itemCapacity);
                    }
                }
            }
        }

        return true;
    }

    void set(Tile tile, Item item){
        if(ores.containsKey(item)){
            tile.setOverlay(ores.get(item));
        }else if(oreFloors.containsKey(item)){
            tile.setFloor(oreFloors.get(item));
        }
    }

    boolean isTaken(Block block, int x, int y){
        int offsetx = -(block.size - 1) / 2;
        int offsety = -(block.size - 1) / 2;
        int pad = 1;

        for(int dx = -pad; dx < block.size + pad; dx++){
            for(int dy = -pad; dy < block.size + pad; dy++){
                if(overlaps(dx + offsetx + x, dy + offsety + y)){
                    return true;
                }
            }
        }

        return false;
    }

    boolean overlaps(int x, int y){
        Tile tile = tiles.get(x, y);

        return tile == null || !tile.block().alwaysReplace || world.getDarkness(x, y) > 0;
    }
}
