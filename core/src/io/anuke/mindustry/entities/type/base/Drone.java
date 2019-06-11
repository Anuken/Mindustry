package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.game.EventType.BuildSelectEvent;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.meta.BlockFlag;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Drone extends FlyingUnit implements BuilderTrait{
    protected Item targetItem;
    protected Tile mineTile;
    protected Queue<BuildRequest> placeQueue = new Queue<>();
    protected boolean isBreaking;

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

            if(entity == null){
                setState(repair);
                return;
            }

            if(core == null) return;

            if((entity.progress < 1f || entity.progress > 0f) && entity.tile.block() instanceof BuildBlock){ //building is valid
                if(!isBuilding() && dst(target) < placeDistance * 0.9f){ //within distance, begin placing
                    if(isBreaking){
                        getPlaceQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y));
                    }else{
                        getPlaceQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y, entity.tile.rotation(), entity.cblock));
                    }
                }

                circle(placeDistance * 0.7f);
            }else{ //building isn't valid
                setState(repair);
            }
        }
    },

    repair = new UnitState(){

        public void entered(){
            target = null;
        }

        public void update(){

            retarget(() -> target = Units.findDamagedTile(team, x, y));

            if(target != null){
                if(target.dst(Drone.this) > type.range){
                    circle(type.range * 0.9f);
                }else{
                    getWeapon().update(Drone.this, target.getX(), target.getY());
                }
            }else{
                if(getSpawner() != null){
                    target = getSpawner();
                    circle(type.range * 0.9f);
                }
            }
        }
    },

    mine = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            TileEntity entity = getClosestCore();

            if(entity == null) return;

            if(targetItem == null){
                findItem();
            }

            //core full
            if(targetItem != null && entity.block.acceptStack(targetItem, 1, entity.tile, Drone.this) == 0){
                setState(repair);
                return;
            }

            //if inventory is full, drop it off.
            if(item.amount >= getItemCapacity()){
                setState(drop);
            }else{
                if(targetItem != null && !acceptsItem(targetItem)){
                    setState(drop);
                    return;
                }

                retarget(() -> {
                    if(getMineTile() == null){
                        findItem();
                    }

                    if(targetItem == null) return;

                    target = world.indexer.findClosestOre(x, y, targetItem);
                });

                if(target instanceof Tile){
                    moveTo(type.range / 1.5f);

                    if(dst(target) < type.range && mineTile != target){
                        setMineTile((Tile)target);
                    }

                    if(((Tile)target).block() != Blocks.air){
                        setState(drop);
                    }
                }
            }
        }

        public void exited(){
            setMineTile(null);
        }
    },
    drop = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(item.amount == 0){
                setState(mine);
                return;
            }

            if(item.item.type != ItemType.material){
                item.amount = 0;
                setState(mine);
                return;
            }

            target = getClosestCore();

            if(target == null) return;

            TileEntity tile = (TileEntity)target;

            if(dst(target) < type.range){
                if(tile.tile.block().acceptStack(item.item, item.amount, tile.tile, Drone.this) == item.amount){
                    Call.transferItemTo(item.item, item.amount, x, y, tile.tile);
                    item.amount = 0;
                }

                setState(repair);
            }

            circle(type.range / 1.8f);
        }
    },
    retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(health >= maxHealth()){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.repair)){
                retarget(() -> {
                    Tile repairPoint = Geometry.findClosest(x, y, world.indexer.getAllied(team, BlockFlag.repair));
                    if(repairPoint != null){
                        target = repairPoint;
                    }else if(getSpawner() != null){
                        target = getSpawner();
                    }
                });
            }else{
                circle(40f);
            }
        }
    };

    static{

        Events.on(BuildSelectEvent.class, event -> {
            EntityGroup<BaseUnit> group = unitGroups[event.team.ordinal()];

            if(!(event.tile.entity instanceof BuildEntity)) return;

            for(BaseUnit unit : group.all()){
                if(unit instanceof Drone){
                    Drone drone = (Drone)unit;
                    if(drone.isBuilding()){
                        //stop building if opposite building begins.
                        BuildRequest req = drone.getCurrentRequest();
                        if(req.breaking != event.breaking && req.x == event.tile.x && req.y == event.tile.y){
                            drone.clearBuilding();
                            drone.setState(drone.repair);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean canMine(Item item){
        return type.toMine.contains(item);
    }

    @Override
    public float getBuildPower(Tile tile){
        return type.buildPower;
    }

    @Override
    public float getMinePower(){
        return type.minePower;
    }

    @Override
    public Queue<BuildRequest> getPlaceQueue(){
        return placeQueue;
    }

    @Override
    public Tile getMineTile(){
        return mineTile;
    }

    @Override
    public void setMineTile(Tile tile){
        mineTile = tile;
    }

    @Override
    public void update(){
        super.update();

        if(state.is(repair) && target != null && target.getTeam() != team){
            target = null;
        }

        if(!state.is(build) && timer.get(timerTarget2, 15)){
            for(Player player : playerGroup.all()){
                if(player.getTeam() == team && player.getCurrentRequest() != null){
                    BuildRequest req = player.getCurrentRequest();
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
        }

        updateBuilding();
    }

    @Override
    protected void updateRotation(){
        if(target != null && ((state.is(repair) && target.dst(this) < type.range) || state.is(mine))){
            rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.3f);
        }else{
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.3f);
        }
    }

    @Override
    public void behavior(){
        if(health <= health * type.retreatPercent){
            setState(retreat);
        }
    }

    @Override
    public UnitState getStartState(){
        return repair;
    }

    @Override
    public void drawOver(){
        drawBuilding();
    }

    @Override
    public float drawSize(){
        return isBuilding() ? placeDistance * 2f : 30f;
    }

    protected void findItem(){
        TileEntity entity = getClosestCore();
        if(entity == null){
            return;
        }
        targetItem = Structs.findMin(type.toMine, world.indexer::hasOre, (a, b) -> -Integer.compare(entity.items.get(a), entity.items.get(b)));
    }

    @Override
    public boolean canCreateBlocks(){
        return true;
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.write(data);
        data.writeInt(mineTile == null || !state.is(mine) ? -1 : mineTile.pos());
        data.writeInt(state.is(repair) && target instanceof TileEntity ? ((TileEntity)target).tile.pos() : -1);
        writeBuilding(data);
    }

    @Override
    public void read(DataInput data) throws IOException{
        super.read(data);
        int mined = data.readInt();
        int repairing = data.readInt();

        readBuilding(data);

        if(mined != -1){
            mineTile = world.tile(mined);
        }

        if(repairing != -1){
            Tile tile = world.tile(repairing);
            target = tile.entity;
            state.set(repair);
        }else{
            state.set(retreat);
        }
    }

}
