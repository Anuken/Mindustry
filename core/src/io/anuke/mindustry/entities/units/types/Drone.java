package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.game.EventType.BlockBuildEvent;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.ThreadQueue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Drone extends FlyingUnit implements BuilderTrait{
    protected static float discoverRange = 120f;
    protected static boolean initialized;
    protected static int timerRepairEffect = timerIndex++;

    protected Item targetItem;
    protected Tile mineTile;
    protected Queue<BuildRequest> placeQueue = new ThreadQueue<>();

    public final UnitState

    build = new UnitState(){

        public void entered(){
            if(!(target instanceof BuildEntity)){
                target = null;
            }
        }

        public void update(){
            BuildEntity entity = (BuildEntity) target;
            TileEntity core = getClosestCore();

            if(entity == null){
                setState(repair);
                return;
            }

            if(core == null) return;

            if(entity.progress() < 1f && entity.tile.block() instanceof BuildBlock){ //building is valid
                if(!isBuilding() && distanceTo(target) < placeDistance * 0.9f){ //within distance, begin placing
                    getPlaceQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y, entity.tile.getRotation(), entity.recipe));
                }

                //if it's missing requirements, try and mine them
                for(ItemStack stack : entity.recipe.requirements){
                    if(!core.items.has(stack.item, stack.amount) && type.toMine.contains(stack.item)){
                        targetItem = stack.item;
                        getPlaceQueue().clear();
                        setState(mine);
                        return;
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
            if(target != null && (((TileEntity) target).health >= ((TileEntity) target).tile.block().health
                    || target.distanceTo(Drone.this) > discoverRange)){
                target = null;
            }

            if(target == null){
                retarget(() -> {
                    target = Units.findAllyTile(team, x, y, discoverRange,
                            tile -> tile.entity != null && tile.entity.health + 0.0001f < tile.block().health);

                    if(target == null){
                        setState(mine);
                    }
                });
            }else if(target.distanceTo(Drone.this) > type.range){
                circle(type.range);
            }else{
                TileEntity entity = (TileEntity) target;
                entity.healBy(type.healSpeed * entity.tile.block().health / 100f * Timers.delta());

                if(timer.get(timerRepairEffect, 30)){
                    Effects.effect(BlockFx.healBlockFull, Palette.heal, entity.x, entity.y, entity.tile.block().size);
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
            if(targetItem != null && entity.tile.block().acceptStack(targetItem, 1, entity.tile, Drone.this) == 0){
                setState(repair);
                return;
            }

            //if inventory is full, drop it off.
            if(inventory.isFull()){
                setState(drop);
            }else{
                if(targetItem != null && !inventory.canAcceptItem(targetItem)){
                    setState(drop);
                    return;
                }

                retarget(() -> {
                    if(findItemDrop()){
                        return;
                    }

                    if(getMineTile() == null){
                        findItem();
                    }

                    if(targetItem == null) return;

                    target = world.indexer().findClosestOre(x, y, targetItem);
                });

                if(target instanceof Tile){
                    moveTo(type.range / 1.5f);

                    if(distanceTo(target) < type.range && mineTile != target){
                        setMineTile((Tile) target);
                    }

                    if(((Tile) target).block() != Blocks.air){
                        setState(drop);
                    }
                }
            }
        }

        public void exited(){
            setMineTile(null);
        }
    },
    pickup = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            ItemDrop item = (ItemDrop) target;

            if(item == null || inventory.isFull() || item.getItem().type != ItemType.material || !inventory.canAcceptItem(item.getItem(), 1)){
                setState(drop);
                return;
            }

            if(distanceTo(item) < 4){
                item.collision(Drone.this, x, y);
            }

            //item has been picked up
            if(item.getAmount() == 0){
                if(!findItemDrop()){
                    setState(drop);
                }
            }

            moveTo(0f);
        }
    },
    drop = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(inventory.isEmpty()){
                setState(mine);
                return;
            }

            if(inventory.getItem().item.type != ItemType.material){
                inventory.clearItem();
                setState(mine);
                return;
            }

            target = getClosestCore();

            if(target == null) return;

            TileEntity tile = (TileEntity) target;

            if(distanceTo(target) < type.range){
                if(tile.tile.block().acceptStack(inventory.getItem().item, inventory.getItem().amount, tile.tile, Drone.this) == inventory.getItem().amount){
                    Call.transferItemTo(inventory.getItem().item, inventory.getItem().amount, x, y, tile.tile);
                    inventory.clearItem();
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
                if(timer.get(timerTarget, 20)){
                    Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair));
                    if(target != null) Drone.this.target = target.entity;
                }
            }else{
                circle(40f);
            }
        }
    };

    {
        initEvents();
    }

    /**
     * Initialize placement event notifier system.
     * Static initialization is to be avoided, thus, this is done lazily.
     */
    private static void initEvents(){
        if(initialized) return;

        Events.on(BlockBuildEvent.class, event -> {
            EntityGroup<BaseUnit> group = unitGroups[event.team.ordinal()];

            if(!(event.tile.entity instanceof BuildEntity)) return;
            BuildEntity entity = event.tile.entity();

            for(BaseUnit unit : group.all()){
                if(unit instanceof Drone){
                    ((Drone) unit).notifyPlaced(entity);
                }
            }
        });

        initialized = true;
    }

    private void notifyPlaced(BuildEntity entity){
        float timeToBuild = entity.recipe.cost;
        float dist = Math.min(entity.distanceTo(x, y) - placeDistance, 0);

        if(dist / type.maxVelocity < timeToBuild * 0.9f){
            //Call.onDroneBeginBuild(this, entity.tile, entity.recipe);
            target = entity;
            setState(build);
        }
    }

    @Override
    public void onCommand(UnitCommand command){
        //no
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

        if(Net.client() && state.is(repair) && target instanceof TileEntity){
            TileEntity entity = (TileEntity) target;
            entity.health += type.healSpeed * Timers.delta();
            entity.health = Mathf.clamp(entity.health, 0, entity.tile.block().health);
        }

        updateBuilding(this);
    }

    @Override
    protected void updateRotation(){
        if(target != null && (state.is(repair) || state.is(mine))){
            rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.3f);
        }else{
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.3f);
        }
    }

    @Override
    public void behavior(){
        if(health <= health * type.retreatPercent &&
                Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }
    }

    @Override
    public UnitState getStartState(){
        return repair;
    }

    @Override
    public void drawOver(){
        trail.draw(Palette.lightTrail, 3f);

        TargetTrait entity = target;

        if(entity instanceof TileEntity && state.is(repair)){
            float len = 5f;
            Draw.color(Color.BLACK, Color.WHITE, 0.95f + Mathf.absin(Timers.time(), 0.8f, 0.05f));
            Shapes.laser("beam", "beam-end",
                    x + Angles.trnsx(rotation, len),
                    y + Angles.trnsy(rotation, len),
                    entity.getX(), entity.getY());
            Draw.color();
        }

        drawBuilding(this);
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
        targetItem = Mathf.findMin(type.toMine, (a, b) -> -Integer.compare(entity.items.get(a), entity.items.get(b)));
    }

    protected boolean findItemDrop(){
        TileEntity core = getClosestCore();

        if(core == null) return false;

        //find nearby dropped items to pick up if applicable
        ItemDrop drop = EntityPhysics.getClosest(itemGroup, x, y, 60f,
                item -> core.tile.block().acceptStack(item.getItem(), item.getAmount(), core.tile, Drone.this) == item.getAmount() &&
                        inventory.canAcceptItem(item.getItem(), 1));
        if(drop != null){
            setState(pickup);
            target = drop;
            return true;
        }
        return false;
    }

    @Override
    public boolean canCreateBlocks(){
        return false;
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.write(data);
        data.writeInt(mineTile == null || !state.is(mine) ? -1 : mineTile.packedPosition());
        data.writeInt(state.is(repair) && target instanceof TileEntity ? ((TileEntity)target).tile.packedPosition() : -1);
        writeBuilding(data);
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        super.read(data, time);
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
