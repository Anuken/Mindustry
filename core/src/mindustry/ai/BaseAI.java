package mindustry.ai;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.BaseRegistry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class BaseAI{
    private static final Vec2 axis = new Vec2(), rotator = new Vec2();
    private static final float correctPercent = 0.5f;
    private static final float step = 5;
    private static final int attempts = 5;
    private static final float emptyChance = 0.01f;
    private static final int timerStep = 0, timerSpawn = 1;

    private static int correct = 0, incorrect = 0;

    private int lastX, lastY, lastW, lastH;
    private boolean triedWalls;

    TeamData data;
    Interval timer = new Interval(4);

    public BaseAI(TeamData data){
        this.data = data;
    }

    public void update(){
        if(timer.get(timerSpawn, 60) && data.hasCore()){
            CoreBlock block = (CoreBlock)data.core().block;

            //create AI core unit
            if(!Groups.unit.contains(u -> u.team() == data.team && u.type() == block.unitType)){
                Unit unit = block.unitType.create(data.team);
                unit.set(data.core());
                unit.add();
                Fx.spawn.at(unit);
            }
        }

        //only schedule when there's something to build.
        if(data.blocks.isEmpty() && timer.get(timerStep, step)){
            if(!triedWalls){
                tryWalls();
                triedWalls = true;
            }

            for(int i = 0; i < attempts; i++){
                int range = 150;

                Position pos = randomPosition();
                //when there are no random positions, do nothing.
                if(pos == null) return;

                Tmp.v1.rnd(Mathf.random(range));
                int wx = (int)(world.toTile(pos.getX()) + Tmp.v1.x), wy = (int)(world.toTile(pos.getY()) + Tmp.v1.y);
                Tile tile = world.tiles.getc(wx, wy);

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

        //chekc valid placeability
        for(Stile tile : result.tiles){
            int realX = tile.x + cx, realY = tile.y + cy;
            if(!Build.validPlace(tile.block, data.team, realX, realY, tile.rotation)){
                return false;
            }
        }

        //make sure at least X% of resource requirements are met
        correct = incorrect = 0;

        if(part.required instanceof Item){
            for(Stile tile : result.tiles){
                if(tile.block instanceof Drill){

                    tile.block.iterateTaken(tile.x + cx, tile.y + cy, (ex, ey) -> {
                        Tile res = world.rawTile(ex, ey);
                        if(res.drop() == part.required){
                            correct ++;
                        }else{
                            incorrect ++;
                        }
                    });
                }
            }
        }

        //fail if not enough fit requirements
        if((float)correct / incorrect < correctPercent){
            return false;
        }

        //queue it
        for(Stile tile : result.tiles){
            data.blocks.add(new BlockPlan(cx + tile.x, cy + tile.y, tile.rotation, tile.block.id, tile.config));
        }

        lastX = cx - 1;
        lastY = cy - 1;
        lastW = result.width + 2;
        lastH = result.height + 2;

        triedWalls = false;

        return true;
    }

    private void tryWalls(){
        Block wall = Blocks.copperWall;
        Tile spawn = state.rules.defaultTeam.core() != null ? state.rules.defaultTeam.core().tile : data.team.core().tile;

        for(int wx = lastX; wx <= lastX + lastW; wx++){
            for(int wy = lastY; wy <= lastY + lastH; wy++){
                Tile tile = world.tile(wx, wy);

                if(tile == null || !tile.block().alwaysReplace) continue;

                boolean any = false;

                for(Point2 p : Geometry.d8){
                    if(Angles.angleDist(Angles.angle(p.x, p.y), spawn.angleTo(tile)) > 70){
                        continue;
                    }

                    Tile o = world.tile(tile.x + p.x, tile.y + p.y);
                    if(o != null && o.team() == data.team && !(o.block() instanceof Wall)){
                        any = true;
                        break;
                    }
                }

                if(any && Build.validPlace(wall, data.team, tile.x, tile.y, 0)){
                    data.blocks.add(new BlockPlan(tile.x, tile.y, (short)0, wall.id, null));
                }
            }
        }
    }
}
