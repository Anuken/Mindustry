package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.ai.BaseRegistry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BaseGenerator{
    private static final Vec2 axis = new Vec2(), rotator = new Vec2();

    private static final int range = 160;

    private Tiles tiles;
    private Seq<Tile> cores;

    public static Block getDifficultyWall(int size, float difficulty){
        Seq<Block> wallsSmall = content.blocks().select(b -> b instanceof Wall && b.size == size
            && !b.insulated && b.buildVisibility == BuildVisibility.shown
            && !(b instanceof Door)
            && !(Structs.contains(b.requirements, i -> state.rules.hiddenBuildItems.contains(i.item))));
        wallsSmall.sort(b -> b.buildCost);
        return wallsSmall.getFrac(difficulty * 0.91f);
    }

    public void generate(Tiles tiles, Seq<Tile> cores, Tile spawn, Team team, Sector sector, float difficulty){
        this.tiles = tiles;
        this.cores = cores;

        //don't generate bases when there are no loaded schematics
        if(bases.cores.isEmpty()) return;

        Mathf.rand.setSeed(sector.id);

        float bracketRange = 0.17f;
        float baseChance = Mathf.lerp(0.7f, 2.1f, difficulty);
        int wallAngle = 70; //180 for full coverage
        double resourceChance = 0.5 * baseChance;
        double nonResourceChance = 0.0005 * baseChance;
        BasePart coreschem = bases.cores.getFrac(difficulty);
        int passes = difficulty < 0.4 ? 1 : difficulty < 0.8 ? 2 : 3;

        Block wall = getDifficultyWall(1, difficulty), wallLarge = getDifficultyWall(2, difficulty);

        for(Tile tile : cores){
            tile.clearOverlay();
            Schematics.placeLoadout(coreschem.schematic, tile.x, tile.y, team, false);

            //fill core with every type of item (even non-material)
            Building entity = tile.build;
            for(Item item : content.items()){
                entity.items.add(item, entity.block.itemCapacity);
            }
        }

        for(int i = 0; i < passes; i++){
            //random schematics
            pass(tile -> {
                if(!tile.block().alwaysReplace) return;

                if(((tile.overlay().asFloor().itemDrop != null || (tile.drop() != null && Mathf.chance(nonResourceChance)))
                || (tile.floor().liquidDrop != null && Mathf.chance(nonResourceChance * 2))) && Mathf.chance(resourceChance)){
                    Seq<BasePart> parts = bases.forResource(tile.drop() != null ? tile.drop() : tile.floor().liquidDrop);
                    if(!parts.isEmpty()){
                        tryPlace(parts.getFrac(difficulty + Mathf.range(bracketRange)), tile.x, tile.y, team);
                    }
                }else if(Mathf.chance(nonResourceChance)){
                    tryPlace(bases.parts.getFrac(difficulty + Mathf.range(bracketRange)), tile.x, tile.y, team);
                }
            });
        }

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

                    for(Point2 p : Geometry.d4){
                        Tile o = tiles.get(tile.x + p.x, tile.y + p.y);

                        //do not block payloads
                        if(o != null && (o.block() instanceof PayloadConveyor || o.block() instanceof PayloadBlock)){
                            return;
                        }
                    }

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

        //clear path for ground units
        for(Tile tile : cores){
            Astar.pathfind(tile, spawn, t -> t.team() == state.rules.waveTeam && !t.within(tile, 25f * 8) ? 100000 : t.floor().hasSurface() ? 1 : 10, t -> !t.block().isStatic()).each(t -> {
                if(!t.within(tile, 25f * 8)){
                    if(t.team() == state.rules.waveTeam){
                        t.setBlock(Blocks.air);
                    }

                    for(Point2 p : Geometry.d8){
                        Tile other = t.nearby(p);
                        if(other != null && other.team() == state.rules.waveTeam){
                            other.setBlock(Blocks.air);
                        }
                    }
                }
            });
        }
    }

    public void postGenerate(){
        if(tiles == null) return;

        for(Tile tile : tiles){
            if(tile.isCenter() && tile.block() instanceof PowerNode && tile.team() == state.rules.waveTeam){
                tile.build.configureAny(new Point2[0]);
                tile.build.placed();
            }
        }
    }

    void pass(Cons<Tile> cons){
        Tile core = cores.first();
        core.circle(range, (x, y) -> cons.get(tiles.getn(x, y)));
    }

    /**
     * Tries to place a base part at a certain location with a certain team.
     * @return success state
     * */
    public static boolean tryPlace(BasePart part, int x, int y, Team team){
        return tryPlace(part, x, y, team, null);
    }

    /**
     * Tries to place a base part at a certain location with a certain team.
     * @return success state
     * */
    public static boolean tryPlace(BasePart part, int x, int y, Team team, @Nullable Intc2 posc){
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

            if(posc != null){
                posc.get(realX, realY);
            }
        }

        if(part.required instanceof Item item){
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){

                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {

                        if(world.tiles.getn(ex, ey).floor().hasSurface()){
                            set(world.tiles.getn(ex, ey), item);
                        }

                        Tile rand = world.tiles.getc(ex + Mathf.range(1), ey + Mathf.range(1));
                        if(rand.floor().hasSurface()){
                            //random ores nearby to make it look more natural
                            set(rand, item);
                        }
                    });
                }
            }
        }

        Schematics.place(result, cx + result.width/2, cy + result.height/2, team);

        //fill drills with items after placing
        if(part.required instanceof Item item){
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

    static void set(Tile tile, Item item){
        if(bases.ores.containsKey(item)){
            tile.setOverlay(bases.ores.get(item));
        }else if(bases.oreFloors.containsKey(item)){
            tile.setFloor(bases.oreFloors.get(item));
        }
    }

    static boolean isTaken(Block block, int x, int y){
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

    static boolean overlaps(int x, int y){
        Tile tile = world.tiles.get(x, y);

        return tile == null || !tile.block().alwaysReplace || world.getDarkness(x, y) > 0;
    }
}
