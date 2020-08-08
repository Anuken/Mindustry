package mindustry.entities.comp;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.Queue;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.BuildBlock.*;

import java.util.*;

import static mindustry.Vars.*;

@Component
abstract class BuilderComp implements Unitc{
    static final Vec2[] vecs = new Vec2[]{new Vec2(), new Vec2(), new Vec2(), new Vec2()};

    @Import float x, y, rotation;

    @SyncLocal Queue<BuildPlan> plans = new Queue<>();
    transient boolean updateBuilding = true;

    @Override
    public void controller(UnitController next){
        //reset building state so AI controlled units will always start off building
        updateBuilding = true;
    }

    @Override
    public void update(){
        if(!updateBuilding) return;

        float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : buildingRange;

        boolean infinite = state.rules.infiniteResources || team().rules().infiniteResources;

        Iterator<BuildPlan> it = plans.iterator();
        while(it.hasNext()){
            BuildPlan req = it.next();
            Tile tile = world.tile(req.x, req.y);
            if(tile == null || (req.breaking && tile.block() == Blocks.air) || (!req.breaking && ((tile.build != null && tile.build.rotation == req.rotation) || !req.block.rotate) && tile.block() == req.block)){
                it.remove();
            }
        }

        Building core = core();

        //nothing to build.
        if(buildPlan() == null) return;

        //find the next build request
        if(plans.size > 1){
            int total = 0;
            BuildPlan req;
            while((dst((req = buildPlan()).tile()) > finalPlaceDst || shouldSkip(req, core)) && total < plans.size){
                plans.removeFirst();
                plans.addLast(req);
                total++;
            }
        }

        BuildPlan current = buildPlan();

        if(!within(current.tile(), finalPlaceDst)) return;

        Tile tile = world.tile(current.x, current.y);

        if(within(tile, finalPlaceDst)){
            rotation = Mathf.slerpDelta(rotation, angleTo(tile), 0.4f);
        }

        if(!(tile.block() instanceof BuildBlock)){
            if(!current.initialized && !current.breaking && Build.validPlace(current.block, team(), current.x, current.y, current.rotation)){
                boolean hasAll = infinite || !Structs.contains(current.block.requirements, i -> core != null && !core.items.has(i.item));

                if(hasAll){
                    Build.beginPlace(current.block, team(), current.x, current.y, current.rotation);
                }else{
                    current.stuck = true;
                }
            }else if(!current.initialized && current.breaking && Build.validBreak(team(), current.x, current.y)){
                Build.beginBreak(team(), current.x, current.y);
            }else{
                plans.removeFirst();
                return;
            }
        }else if(tile.team() != team()){
            plans.removeFirst();
            return;
        }

        if(tile.build instanceof BuildEntity && !current.initialized){
            Core.app.post(() -> Events.fire(new BuildSelectEvent(tile, team(), (Builderc)this, current.breaking)));
            current.initialized = true;
        }

        //if there is no core to build with or no build entity, stop building!
        if((core == null && !infinite) || !(tile.build instanceof BuildEntity)){
            return;
        }

        //otherwise, update it.
        BuildEntity entity = tile.bc();

        if(current.breaking){
            entity.deconstruct(base(), core, 1f / entity.buildCost * Time.delta * type().buildSpeed * state.rules.buildSpeedMultiplier);
        }else{
            if(entity.construct(base(), core, 1f / entity.buildCost * Time.delta * type().buildSpeed * state.rules.buildSpeedMultiplier, current.hasConfig)){
                if(current.hasConfig){
                    Call.tileConfig(null, tile.build, current.config);
                }
            }
        }

        current.stuck = Mathf.equal(current.progress, entity.progress);
        current.progress = entity.progress;
    }


    /** Draw all current build requests. Does not draw the beam effect, only the positions. */
    void drawBuildRequests(){

        for(BuildPlan request : plans){
            if(request.progress > 0.01f || (buildPlan() == request && request.initialized && (dst(request.x * tilesize, request.y * tilesize) <= buildingRange || state.isEditor()))) continue;

            request.animScale = 1f;
            if(request.breaking){
                control.input.drawBreaking(request);
            }else{
                request.block.drawRequest(request, control.input.allRequests(),
                Build.validPlace(request.block, team(), request.x, request.y, request.rotation) || control.input.requestMatches(request));
            }
        }

        Draw.reset();
    }

    /** @return whether this request should be skipped, in favor of the next one. */
    boolean shouldSkip(BuildPlan request, @Nullable Building core){
        //requests that you have at least *started* are considered
        if(state.rules.infiniteResources || team().rules().infiniteResources || request.breaking || core == null) return false;
        //TODO these are bad criteria
        return (request.stuck && !core.items.has(request.block.requirements)) || (Structs.contains(request.block.requirements, i -> !core.items.has(i.item)) && !request.initialized);
    }

    void removeBuild(int x, int y, boolean breaking){
        //remove matching request
        int idx = plans.indexOf(req -> req.breaking == breaking && req.x == x && req.y == y);
        if(idx != -1){
            plans.removeIndex(idx);
        }
    }

    /** Return whether this builder's place queue contains items. */
    boolean isBuilding(){
        return plans.size != 0;
    }

    /** Clears the placement queue. */
    void clearBuilding(){
        plans.clear();
    }

    /** Add another build requests to the tail of the queue, if it doesn't exist there yet. */
    void addBuild(BuildPlan place){
        addBuild(place, true);
    }

    /** Add another build requests to the queue, if it doesn't exist there yet. */
    void addBuild(BuildPlan place, boolean tail){
        BuildPlan replace = null;
        for(BuildPlan request : plans){
            if(request.x == place.x && request.y == place.y){
                replace = request;
                break;
            }
        }
        if(replace != null){
            plans.remove(replace);
        }
        Tile tile = world.tile(place.x, place.y);
        if(tile != null && tile.build instanceof BuildEntity){
            place.progress = tile.<BuildEntity>bc().progress;
        }
        if(tail){
            plans.addLast(place);
        }else{
            plans.addFirst(place);
        }
    }

    boolean activelyBuilding(){
        return isBuilding() && updateBuilding;
    }

    /** Return the build request currently active, or the one at the top of the queue.*/
    @Nullable
    BuildPlan buildPlan(){
        return plans.size == 0 ? null : plans.first();
    }

    @Override
    public void draw(){
        if(!isBuilding() || !updateBuilding) return;

        //TODO check correctness
        Draw.z(Layer.flyingUnit);

        BuildPlan plan = buildPlan();
        Tile tile = world.tile(plan.x, plan.y);

        if(dst(tile) > buildingRange && !state.isEditor()){
            return;
        }

        int size = plan.breaking ? tile.block().size : plan.block.size;
        float tx = plan.drawx(), ty = plan.drawy();

        Lines.stroke(1f, Pal.accent);
        float focusLen = 3.8f + Mathf.absin(Time.time(), 1.1f, 0.6f);
        float px = x + Angles.trnsx(rotation, focusLen);
        float py = y + Angles.trnsy(rotation, focusLen);

        float sz = Vars.tilesize * size / 2f;
        float ang = angleTo(tx, ty);

        vecs[0].set(tx - sz, ty - sz);
        vecs[1].set(tx + sz, ty - sz);
        vecs[2].set(tx - sz, ty + sz);
        vecs[3].set(tx + sz, ty + sz);

        Arrays.sort(vecs, Structs.comparingFloat(vec -> -Angles.angleDist(angleTo(vec), ang)));

        float x1 = vecs[0].x, y1 = vecs[0].y,
        x3 = vecs[1].x, y3 = vecs[1].y;

        Draw.alpha(1f);

        Lines.line(px, py, x1, y1);
        Lines.line(px, py, x3, y3);

        Fill.circle(px, py, 1.6f + Mathf.absin(Time.time(), 0.8f, 1.5f));

        Draw.color();
    }
}
