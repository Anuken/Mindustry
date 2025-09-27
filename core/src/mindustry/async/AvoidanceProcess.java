package mindustry.async;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;

import java.util.*;

import static mindustry.Vars.*;

public class AvoidanceProcess implements AsyncProcess{
    /** cached world size */
    static int wwidth, wheight;

    @Nullable int[] buffer1, buffer2;
    volatile boolean swap;

    IntSeq requests = new IntSeq();

    @Nullable int[] avoidance;
    boolean modified;
    boolean active;

    public @Nullable int[] getAvoidance(){
        if(!active){
            //lazily initialize and begin processing after this first request
            buffer1 = new int[wwidth * wheight];
            buffer2 = new int[wwidth * wheight];
            active = true;
        }
        return avoidance;
    }

    @Override
    public void init(){
        wwidth = Vars.world.width();
        wheight = Vars.world.height();
    }

    @Override
    public void reset(){
        buffer1 = buffer2 = avoidance = null;
        swap = false;
        modified = false;
        active = false;
        requests.clear();
    }

    @Override
    public void begin(){
        if(!active) return;

        requests.clear();

        avoidance = !swap ? buffer1 : buffer2;

        for(var team : state.teams.present){
            //only do avoidance if it's relevant to the team
            if(team.team.isAI() && !team.team.rules().rtsAi){
                for(var unit : team.units){
                    if(unit.collisionLayer() == PhysicsProcess.layerGround){
                        //scaling is oversized 2x because units need to avoid things that are at their origin tile
                        float scaling = 2f;
                        requests.add(Point2.pack(unit.tileX(), unit.tileY()), Float.floatToRawIntBits(unit.hitSize * unitCollisionRadiusScale / tilesize * scaling), unit.id);
                    }
                }
            }
        }
    }

    @Override
    public void process(){
        //double buffering; one buffer is always valid (not being updated)
        var buffer = swap ? buffer1 : buffer2;
        swap = !swap;

        if(buffer == null) return;
        //technically, this is wrong, and will lead to flickering avoidance when all units are gone, but this doesn't matter because it's not being queried either way
        if(modified){
            Arrays.fill(buffer, 0);
        }

        modified = requests.size > 0;

        int total = requests.size;
        int[] items = requests.items;
        for(int i = 0; i < total; i += 3){
            int point = items[i], id = items[i + 2];
            int rx = Point2.x(point), ry = Point2.y(point);
            float rad = Float.intBitsToFloat(items[i + 1]);
            float rad2 = rad * rad;

            int r = Math.max(1, Mathf.ceil(rad));

            for(int dx = -r; dx <= r; dx++){
                for(int dy = -r; dy <= r; dy++){
                    int x = dx + rx, y = dy + ry;
                    if(x >= 0 && y >= 0 && x < wwidth && y < wheight && (dx*dx + dy*dy) <= rad2){
                        buffer[x + y * wwidth] = Math.max(buffer[x + y * wwidth], Integer.MAX_VALUE - id);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldProcess(){
        return active;
    }
}
