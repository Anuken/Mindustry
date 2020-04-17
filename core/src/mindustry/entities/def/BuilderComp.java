package mindustry.entities.def;

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
abstract class BuilderComp implements Unitc, DrawLayerFlyingc{
    static final Vec2[] vecs = new Vec2[]{new Vec2(), new Vec2(), new Vec2(), new Vec2()};

    @Import float x, y, rotation;

    Queue<BuildRequest> requests = new Queue<>();
    transient float buildSpeed = 1f;
    //boolean building;

    @Override
    public void update(){
        float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : buildingRange;

        Iterator<BuildRequest> it = requests.iterator();
        while(it.hasNext()){
            BuildRequest req = it.next();
            Tile tile = world.tile(req.x, req.y);
            if(tile == null || (req.breaking && tile.block() == Blocks.air) || (!req.breaking && (tile.rotation() == req.rotation || !req.block.rotate) && tile.block() == req.block)){
                it.remove();
            }
        }

        Tilec core = closestCore();

        //nothing to build.
        if(buildRequest() == null) return;

        //find the next build request
        if(requests.size > 1){
            int total = 0;
            BuildRequest req;
            while((dst((req = buildRequest()).tile()) > finalPlaceDst || shouldSkip(req, core)) && total < requests.size){
                requests.removeFirst();
                requests.addLast(req);
                total++;
            }
        }

        BuildRequest current = buildRequest();

        if(dst(current.tile()) > finalPlaceDst) return;

        Tile tile = world.tile(current.x, current.y);

        if(dst(tile) <= finalPlaceDst){
            rotation = Mathf.slerpDelta(rotation, angleTo(tile), 0.4f);
        }

        if(!(tile.block() instanceof BuildBlock)){
            if(!current.initialized && !current.breaking && Build.validPlace(team(), current.x, current.y, current.block, current.rotation)){
                boolean hasAll = !Structs.contains(current.block.requirements, i -> !core.items().has(i.item));

                if(hasAll || state.rules.infiniteResources){
                    Build.beginPlace(team(), current.x, current.y, current.block, current.rotation);
                }else{
                    current.stuck = true;
                }
            }else if(!current.initialized && current.breaking && Build.validBreak(team(), current.x, current.y)){
                Build.beginBreak(team(), current.x, current.y);
            }else{
                requests.removeFirst();
                return;
            }
        }else if(tile.team() != team()){
            requests.removeFirst();
            return;
        }

        if(tile.entity instanceof BuildEntity && !current.initialized){
            Core.app.post(() -> Events.fire(new BuildSelectEvent(tile, team(), (Builderc)this, current.breaking)));
            current.initialized = true;
        }

        //if there is no core to build with or no build entity, stop building!
        if((core == null && !state.rules.infiniteResources) || !(tile.entity instanceof BuildEntity)){
            return;
        }

        //otherwise, update it.
        BuildEntity entity = tile.ent();

        if(current.breaking){
            entity.deconstruct(this, core, 1f / entity.buildCost * Time.delta() * buildSpeed * state.rules.buildSpeedMultiplier);
        }else{
            if(entity.construct(this, core, 1f / entity.buildCost * Time.delta() * buildSpeed * state.rules.buildSpeedMultiplier, current.hasConfig)){
                if(current.hasConfig){
                    Call.onTileConfig(null, tile.entity, current.config);
                }
            }
        }

        current.stuck = Mathf.equal(current.progress, entity.progress);
        current.progress = entity.progress;
    }


    /** Draw all current build requests. Does not draw the beam effect, only the positions. */
    void drawBuildRequests(){

        for(BuildRequest request : requests){
            if(request.progress > 0.01f || (buildRequest() == request && request.initialized && (dst(request.x * tilesize, request.y * tilesize) <= buildingRange || state.isEditor()))) continue;

            request.animScale = 1f;
            if(request.breaking){
                control.input.drawBreaking(request);
            }else{
                request.block.drawRequest(request, control.input.allRequests(),
                Build.validPlace(team(), request.x, request.y, request.block, request.rotation) || control.input.requestMatches(request));
            }
        }

        Draw.reset();
    }

    /** @return whether this request should be skipped, in favor of the next one. */
    boolean shouldSkip(BuildRequest request, @Nullable Tilec core){
        //requests that you have at least *started* are considered
        if(state.rules.infiniteResources || request.breaking || core == null) return false;
        //TODO these are bad criteria
        return (request.stuck && !core.items().has(request.block.requirements)) || (Structs.contains(request.block.requirements, i -> !core.items().has(i.item)) && !request.initialized);
    }

    void removeBuild(int x, int y, boolean breaking){
        //remove matching request
        int idx = requests.indexOf(req -> req.breaking == breaking && req.x == x && req.y == y);
        if(idx != -1){
            requests.removeIndex(idx);
        }
    }

    /** Return whether this builder's place queue contains items. */
    boolean isBuilding(){
        return requests.size != 0;
    }

    /** Clears the placement queue. */
    void clearBuilding(){
        requests.clear();
    }

    /** Add another build requests to the tail of the queue, if it doesn't exist there yet. */
    void addBuild(BuildRequest place){
        addBuild(place, true);
    }

    /** Add another build requests to the queue, if it doesn't exist there yet. */
    void addBuild(BuildRequest place, boolean tail){
        BuildRequest replace = null;
        for(BuildRequest request : requests){
            if(request.x == place.x && request.y == place.y){
                replace = request;
                break;
            }
        }
        if(replace != null){
            requests.remove(replace);
        }
        Tile tile = world.tile(place.x, place.y);
        if(tile != null && tile.entity instanceof BuildEntity){
            place.progress = tile.<BuildEntity>ent().progress;
        }
        if(tail){
            requests.addLast(place);
        }else{
            requests.addFirst(place);
        }
    }

    /** Return the build requests currently active, or the one at the top of the queue.*/
    @Nullable BuildRequest buildRequest(){
        return requests.size == 0 ? null : requests.first();
    }

    @Override
    public void drawFlying(){
        if(!isBuilding()) return;
        BuildRequest request = buildRequest();
        Tile tile = world.tile(request.x, request.y);

        if(dst(tile) > buildingRange && !state.isEditor()){
            return;
        }

        int size = request.breaking ? tile.block().size : request.block.size;
        float tx = request.drawx(), ty = request.drawy();

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
