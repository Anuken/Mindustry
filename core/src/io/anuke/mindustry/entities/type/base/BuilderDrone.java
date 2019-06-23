package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.IntIntMap;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.game.EventType.BuildSelectEvent;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.gen.BrokenBlock;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class BuilderDrone extends BaseDrone implements BuilderTrait{
    private static final StaticReset reset = new StaticReset();
    private static final IntIntMap totals = new IntIntMap();

    protected Queue<BuildRequest> placeQueue = new Queue<>();
    protected boolean isBreaking;
    protected Player playerTarget;

    public final UnitState

    build = new UnitState(){

        public void entered(){
            if(!(target instanceof BuildEntity)){
                target = null;
            }
        }

        public void update(){
            BuildEntity entity = (BuildEntity)target;
            TileEntity core = getClosestCore();

            if(isBuilding() && entity == null && isRebuild()){
                target = world.tile(buildRequest().x, buildRequest().y);
                circle(placeDistance * 0.7f);
                target = null;

                BuildRequest request = buildRequest();

                if(world.tile(request.x, request.y).entity instanceof BuildEntity){
                    target = world.tile(request.x, request.y).entity;
                }
            }else if(entity != null && core != null && (entity.progress < 1f || entity.progress > 0f) && entity.tile.block() instanceof BuildBlock){ //building is valid
                if(!isBuilding() && dst(target) < placeDistance * 0.9f){ //within distance, begin placing
                    if(isBreaking){
                        buildQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y));
                    }else{
                        buildQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y, entity.tile.rotation(), entity.cblock));
                    }
                }

                circle(placeDistance * 0.7f);
                velocity.scl(0.74f);
            }else{ //else, building isn't valid, follow a player
                target = null;

                if(playerTarget == null || playerTarget.getTeam() != team || !playerTarget.isValid()){
                    playerTarget = null;

                    if(retarget()){
                        float minDst = Float.POSITIVE_INFINITY;
                        int minDrones = Integer.MAX_VALUE;

                        //find player with min amount of drones
                        for(Player player : playerGroup.all()){
                            if(player.getTeam() == team){
                                int drones = getDrones(player);
                                float dst = dst2(player);

                                if(playerTarget == null || drones < minDrones || (drones == minDrones && dst < minDst)){
                                    minDrones = drones;
                                    minDst = dst;
                                    playerTarget = player;
                                }
                            }
                        }
                    }

                    if(getSpawner() != null){
                        target = getSpawner();
                        circle(40f);
                        target = null;
                    }
                }else{
                    incDrones(playerTarget);
                    TargetTrait prev = target;
                    target = playerTarget;
                    float dst = 90f + (id % 4)*30;
                    float tdst = dst(target);
                    float scale = (Mathf.lerp(1f, 0.77f, 1f - Mathf.clamp((tdst - dst) / dst)));
                    circle(dst);
                    velocity.scl(scale);
                    target = prev;
                }
            }
        }
    };

    public BuilderDrone(){
        if(reset.check()){
            Events.on(BuildSelectEvent.class, event -> {
                EntityGroup<BaseUnit> group = unitGroups[event.team.ordinal()];

                if(!(event.tile.entity instanceof BuildEntity)) return;

                for(BaseUnit unit : group.all()){
                    if(unit instanceof BuilderDrone){
                        BuilderDrone drone = (BuilderDrone)unit;
                        if(drone.isBuilding()){
                            //stop building if opposite building begins.
                            BuildRequest req = drone.buildRequest();
                            if(req.breaking != event.breaking && req.x == event.tile.x && req.y == event.tile.y){
                                drone.clearBuilding();
                                drone.target = null;
                            }
                        }
                    }
                }
            });
        }
    }

    int getDrones(Player player){
        return Pack.leftShort(totals.get(player.id, 0));
    }

    void incDrones(Player player){
        int num = totals.get(player.id, 0);
        int amount = Pack.leftShort(num), frame = Pack.rightShort(num);
        short curFrame = (short)(Core.graphics.getFrameId() % Short.MAX_VALUE);

        if(frame != curFrame){
            totals.put(player.id, Pack.shortInt((short)1, curFrame));
        }else{
            totals.put(player.id, Pack.shortInt((short)(amount + 1), curFrame));
        }
    }

    boolean isRebuild(){
        //disabled until further notice, reason being that it's too annoying when playing enemies and too broken for ally use
        return false; //Vars.state.rules.enemyCheat && team == waveTeam;
    }

    @Override
    public float getBuildPower(Tile tile){
        return type.buildPower;
    }

    @Override
    public Queue<BuildRequest> buildQueue(){
        return placeQueue;
    }

    @Override
    public void update(){
        super.update();

        if(!isBuilding() && timer.get(timerTarget2, 15)){
            for(Player player : playerGroup.all()){
                if(player.getTeam() == team && player.buildRequest() != null){
                    BuildRequest req = player.buildRequest();
                    Tile tile = world.tile(req.x, req.y);
                    if(tile != null && tile.entity instanceof BuildEntity){
                        BuildEntity b = tile.entity();
                        float dist = Math.min(b.dst(x, y) - placeDistance, 0);
                        if(dist / type.maxVelocity < b.buildCost * 0.9f){
                            target = b;
                            this.isBreaking = req.breaking;
                            setState(build);
                            break;
                        }
                    }
                }
            }

            if(isRebuild() && !isBuilding()){
                TeamData data = Vars.state.teams.get(team);
                if(!data.brokenBlocks.isEmpty()){
                    long block = data.brokenBlocks.removeLast();

                    placeQueue.addFirst(new BuildRequest(BrokenBlock.x(block), BrokenBlock.y(block), BrokenBlock.rotation(block), content.block(BrokenBlock.block(block))));
                    setState(build);
                }
            }
        }

        updateBuilding();
    }

    @Override
    public boolean shouldRotate(){
        return isBuilding();
    }

    @Override
    public UnitState getStartState(){
        return build;
    }

    @Override
    public void drawOver(){
        drawBuilding();
    }

    @Override
    public float drawSize(){
        return isBuilding() ? placeDistance * 2f : 30f;
    }

    @Override
    public boolean canCreateBlocks(){
        return true;
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.write(data);
        writeBuilding(data);
    }

    @Override
    public void read(DataInput data) throws IOException{
        super.read(data);
        readBuilding(data);
    }
}
