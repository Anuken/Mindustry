package mindustry.ai;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.BaseRegistry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class BaseBuilderAI{
    private static final Vec2 axis = new Vec2(), rotator = new Vec2();
    private static final int attempts = 5, coreUnitMultiplier = 2;
    private static final float emptyChance = 0.01f;
    private static final int timerStep = 0, timerSpawn = 1, timerRefreshPath = 2;
    private static final float placeIntervalMin = 12f, placeIntervalMax = 2f;
    private static final int pathStep = 50;
    private static final Seq<Tile> tmpTiles = new Seq<>();

    private static int correct = 0, incorrect = 0;

    private int lastX, lastY, lastW, lastH;
    private boolean triedWalls, foundPath;

    final TeamData data;
    final Interval timer = new Interval(4);

    IntSet path = new IntSet();
    IntSet calcPath = new IntSet();
    @Nullable Tile calcTile;
    boolean calculating, startedCalculating;
    int calcCount = 0;
    int totalCalcs = 0;
    Block wallType;

    public BaseBuilderAI(TeamData data){
        this.data = data;
    }

    public void update(){

        //fill cores.
        if(data.team.cores().size > 0){
            var core = data.team.cores().first();
            for(Item item : content.items()){
                core.items.set(item, core.getMaximumAccepted(item));
            }
        }

        if(wallType == null){
            wallType = BaseGenerator.getDifficultyWall(1, data.team.rules().buildAiTier / 0.8f);
        }

        if(data.team.rules().aiCoreSpawn && timer.get(timerSpawn, 60 * 6f) && data.hasCore()){
            CoreBlock block = (CoreBlock)data.core().block;
            int coreUnits = data.countType(block.unitType);

            //create AI core unit(s)
            if(!state.isEditor() && coreUnits < data.cores.size * coreUnitMultiplier){
                Unit unit = block.unitType.create(data.team);
                unit.set(data.cores.random());
                unit.add();
                Fx.spawn.at(unit);
            }
        }

        //refresh path
        if(!calculating && (timer.get(timerRefreshPath, 3f * Time.toMinutes) || !startedCalculating) && data.hasCore()){
            calculating = true;
            startedCalculating = true;
            calcPath.clear();
        }

        //didn't find tile in time
        if(calculating && calcCount >= world.width() * world.height()){
            calculating = false;
            calcCount = 0;
            calcPath.clear();
            totalCalcs ++;
        }

        //calculate path for units so schematics are not placed on it
        if(calculating){
            if(calcTile == null){
                Vars.spawner.eachGroundSpawn((x, y) -> calcTile = world.tile(x, y));
                if(calcTile == null){
                    calculating = false;
                }
            }else{
                var field = pathfinder.getField(data.team, Pathfinder.costGround, Pathfinder.fieldCore);

                if(field.weights != null){
                    int[] weights = field.weights;
                    for(int i = 0; i < pathStep; i++){
                        int minCost = Integer.MAX_VALUE;
                        int cx = calcTile.x, cy = calcTile.y;
                        boolean foundAny = false;
                        for(Point2 p : Geometry.d4){
                            int nx = cx + p.x, ny = cy + p.y, packed = world.packArray(nx, ny);

                            Tile other = world.tile(nx, ny);
                            if(other != null && weights[packed] < minCost && weights[packed] != -1){
                                minCost = weights[packed];
                                calcTile = other;
                                foundAny = true;
                            }
                        }

                        //didn't find anything, break out of loop, this will trigger a clear later
                        if(!foundAny){
                            calcCount = Integer.MAX_VALUE;
                            break;
                        }

                        calcPath.add(calcTile.pos());
                        for(Point2 p : Geometry.d8){
                            calcPath.add(Point2.pack(p.x + calcTile.x, p.y + calcTile.y));
                        }

                        //found the end.
                        if(calcTile.build instanceof CoreBuild b && b.team != data.team){
                            //clean up calculations and flush results
                            calculating = false;
                            calcCount = 0;
                            path.clear();
                            path.addAll(calcPath);
                            calcPath.clear();
                            calcTile = null;
                            totalCalcs ++;
                            foundPath = true;

                            break;
                        }

                        calcCount ++;
                    }
                }
            }
        }

        //only schedule when there's something to build.
        if(foundPath && data.plans.isEmpty() && timer.get(timerStep, Mathf.lerp(placeIntervalMin, placeIntervalMax, data.team.rules().buildAiTier))){
            //TODO walls are silly, no walls
            //if(!triedWalls){
            //    tryWalls();
            //    triedWalls = true;
            //}

            for(int i = 0; i < attempts; i++){
                int range = 150;

                Position pos = randomPosition();

                //when there are no random positions, do nothing.
                if(pos == null) return;

                Tmp.v1.rnd(Mathf.random(range));
                int wx = (int)(World.toTile(pos.getX()) + Tmp.v1.x), wy = (int)(World.toTile(pos.getY()) + Tmp.v1.y);
                Tile tile = world.tiles.getc(wx, wy);

                //try not to block the spawn point
                if(spawner.getSpawns().contains(t -> t.within(tile, tilesize * 40f))){
                    continue;
                }

                Seq<BasePart> parts = null;

                //pick a completely random base part, and place it a random location
                //((yes, very intelligent))
                if(tile.drop() != null && Vars.bases.forResource(tile.drop()).any()){
                    parts = Vars.bases.forResource(tile.drop());
                }else if(Mathf.chance(emptyChance)){
                    parts = Vars.bases.parts;
                }

                if(parts != null){
                    BasePart part = parts.random();
                    if(tryPlace(part, tile.x, tile.y)){
                        break;
                    }
                }
            }
        }
    }

    /** @return a random position from which to seed building. */
    private Position randomPosition(){
        if(data.hasCore()){
            return data.cores.random();
        }else if(data.team == state.rules.waveTeam){
            return spawner.getSpawns().random();
        }
        return null;
    }

    private boolean tryPlace(BasePart part, int x, int y){
        int rotation = Mathf.range(2);
        axis.set((int)(part.schematic.width / 2f), (int)(part.schematic.height / 2f));
        Schematic result = Schematics.rotate(part.schematic, rotation);
        int rotdeg = rotation*90;
        rotator.set(part.centerX, part.centerY).rotateAround(axis, rotdeg);
        //bottom left schematic corner
        int cx = x - (int)rotator.x;
        int cy = y - (int)rotator.y;

        //check valid placeability
        for(Stile tile : result.tiles){
            int realX = tile.x + cx, realY = tile.y + cy;
            if(!Build.validPlace(tile.block, data.team, realX, realY, tile.rotation)){
                return false;
            }
            Tile wtile = world.tile(realX, realY);

            if(tile.block instanceof PayloadConveyor || tile.block instanceof PayloadBlock){
                //near a building
                for(Point2 point : Edges.getEdges(tile.block.size)){
                    var t = world.build(tile.x + point.x, tile.y + point.y);
                    if(t != null){
                        return false;
                    }
                }
            }

            //may intersect AI path
            tmpTiles.clear();
            if(tile.block.solid && wtile != null && wtile.getLinkedTilesAs(tile.block, tmpTiles).contains(t -> path.contains(t.pos()))){
                return false;
            }
        }

        //make sure at least X% of resource requirements are met
        correct = incorrect = 0;
        boolean anyDrills = false;

        if(part.required instanceof Item){
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){
                    anyDrills = true;

                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {
                        Tile res = world.rawTile(ex, ey);
                        if(res.drop() == part.required){
                            correct ++;
                        }else if(res.drop() != null){
                            incorrect ++;
                        }
                    });
                }
            }
        }

        //fail if not enough fit requirements
        if(anyDrills && (incorrect != 0 || correct == 0)){
            return false;
        }

        //queue it
        for(Stile tile : result.tiles){
            data.plans.add(new BlockPlan(cx + tile.x, cy + tile.y, tile.rotation, tile.block.id, tile.config));
        }

        lastX = cx - 1;
        lastY = cy - 1;
        lastW = result.width + 2;
        lastH = result.height + 2;

        triedWalls = false;

        return true;
    }

    private void tryWalls(){
        Block wall = wallType;
        Building spawnt = state.rules.defaultTeam.core() != null ? state.rules.defaultTeam.core() : data.team.core();
        Tile spawn = spawnt == null ? null : spawnt.tile;

        if(spawn == null) return;

        for(int wx = lastX; wx <= lastX + lastW; wx++){
            outer:
            for(int wy = lastY; wy <= lastY + lastH; wy++){
                Tile tile = world.tile(wx, wy);

                if(tile == null || !tile.block().alwaysReplace) continue;

                boolean any = false;

                for(Point2 p : Geometry.d8){
                    if(Angles.angleDist(Angles.angle(p.x, p.y), spawn.angleTo(tile)) > 70){
                        continue;
                    }

                    Tile o = world.tile(tile.x + p.x, tile.y + p.y);
                    if(o != null && (o.block() instanceof PayloadBlock || o.block() instanceof PayloadConveyor || o.block() instanceof ShockMine)){
                        continue outer;
                    }

                    if(o != null && o.team() == data.team && !(o.block() instanceof Wall)){
                        any = true;
                    }
                }

                tmpTiles.clear();
                if(any && Build.validPlace(wall, data.team, tile.x, tile.y, 0) && !tile.getLinkedTilesAs(wall, tmpTiles).contains(t -> path.contains(t.pos()))){
                    data.plans.add(new BlockPlan(tile.x, tile.y, (short)0, wall.id, null));
                }
            }
        }
    }
}