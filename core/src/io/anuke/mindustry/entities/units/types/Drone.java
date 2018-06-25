package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.EventType.BlockBuildEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.meta.BlockFlag;
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

import static io.anuke.mindustry.Vars.*;

public class Drone extends FlyingUnit implements BuilderTrait {
    public static int typeID = -1;

    protected static ObjectSet<Item> toMine;
    protected static float healSpeed = 0.1f;
    protected static float discoverRange = 120f;
    protected static boolean initialized;

    protected Item targetItem;
    protected Tile mineTile;
    protected Queue<BuildRequest> placeQueue = new ThreadQueue<>();

    /**Initialize placement event notifier system.
     * Static initialization is to be avoided, thus, this is done lazily.*/
    private static void initEvents(){
        if(initialized) return;

        toMine = ObjectSet.with(Items.lead, Items.tungsten);

        Events.on(BlockBuildEvent.class, (team, tile) -> {
            EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];

            if(!(tile.entity instanceof BuildEntity)) return;
            BuildEntity entity = tile.entity();

            for(BaseUnit unit : group.all()){
                if(unit instanceof Drone){
                    ((Drone) unit).notifyPlaced(entity);
                }
            }
        });
    }

    {
        initEvents();
    }

    public Drone(UnitType type, Team team) {
        super(type, team);
    }

    public Drone(){
    }

    private void notifyPlaced(BuildEntity entity){
        float timeToBuild = entity.recipe.cost;
        float dist = Math.min(entity.distanceTo(x, y) - placeDistance, 0);

        if(dist / type.maxVelocity < timeToBuild * 0.9f){
            target = entity;
            setState(build);
        }
    }

    @Override
    public int getTypeID() {
        return typeID;
    }

    @Override
    public float getBuildPower(Tile tile) {
        return 0.3f;
    }

    @Override
    public Queue<BuildRequest> getPlaceQueue() {
        return placeQueue;
    }

    @Override
    public Tile getMineTile() {
        return mineTile;
    }

    @Override
    public void setMineTile(Tile tile) {
        this.mineTile = tile;
    }

    @Override
    public void update() {
        super.update();

        x += Mathf.sin(Timers.time() + id * 999, 25f, 0.07f);
        y += Mathf.cos(Timers.time() + id * 999, 25f, 0.07f);

        updateBuilding(this);
    }

    @Override
    protected void updateRotation() {
        if(target != null && (state.is(repair) || state.is(mine))){
            rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.3f);
        }else{
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.3f);
        }

        if(velocity.len() <= 0.2f && !(state.is(repair) && target != null)){
            rotation += Mathf.sin(Timers.time() + id * 99, 10f, 5f);
        }
    }

    @Override
    public void behavior() {
        if(health <= health * type.retreatPercent &&
                Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }
    }

    @Override
    public UnitState getStartState() {
        return repair;
    }

    @Override
    public void drawOver() {
        trail.draw(Palette.lighterOrange, Palette.lightishOrange, 3f);

        if(target instanceof TileEntity && state.is(repair)){
            float len = 5f;
            Draw.color(Color.BLACK, Color.WHITE, 0.95f + Mathf.absin(Timers.time(), 0.8f, 0.05f));
            Shapes.laser("beam", "beam-end",
                    x + Angles.trnsx(rotation, len),
                    y + Angles.trnsy(rotation, len),
                    target.getX(), target.getY());
            Draw.color();
        }

        drawBuilding(this);
    }

    @Override
    public float drawSize() {
        return isBuilding() ? placeDistance*2f : 30f;
    }

    @Override
    public float getAmmoFraction() {
        return inventory.getItem().amount / (float)type.itemCapacity;
    }

    protected void findItem(){
        TileEntity entity = getClosestCore();
        if(entity == null){
            return;
        }
        targetItem = Mathf.findMin(toMine, (a, b) -> -Integer.compare(entity.items.getItem(a), entity.items.getItem(b)));
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

    public final UnitState

    build = new UnitState(){
        public void entered() {
            target = null;
        }

        public void update() {
            BuildEntity entity = (BuildEntity)target;
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
                    if(!core.items.hasItem(stack.item, stack.amount) && toMine.contains(stack.item)){
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
        public void entered() {
            target = null;
        }

        public void update() {
            if(target != null && (((TileEntity)target).health >= ((TileEntity)target).tile.block().health
                    || target.distanceTo(Drone.this) > discoverRange)){
                target = null;
            }

            if (target == null) {
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
                entity.health += healSpeed * Timers.delta();
                entity.health = Mathf.clamp(entity.health, 0, entity.tile.block().health);
            }
        }
    },
    mine = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            TileEntity entity = getClosestCore();

            if(entity == null) return;

            if(targetItem == null) {
                findItem();
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

                if(target instanceof Tile) {
                    moveTo(type.range/1.5f);

                    if (distanceTo(target) < type.range) {
                        setMineTile((Tile)target);
                    }
                }
            }
        }

        public void exited() {
            setMineTile(null);
        }
    },
    pickup = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            ItemDrop item = (ItemDrop)target;

            if(inventory.isFull() || !inventory.canAcceptItem(item.getItem(), 1)){
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
    drop = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            if(inventory.isEmpty()){
                setState(mine);
                return;
            }

            target = getClosestCore();

            if(target == null) return;

            TileEntity tile = (TileEntity)target;

            if(distanceTo(target) < type.range
                    && tile.tile.block().acceptStack(inventory.getItem().item, inventory.getItem().amount, tile.tile, Drone.this) == inventory.getItem().amount){
                CallEntity.transferItemTo(inventory.getItem().item, inventory.getItem().amount, x, y, tile.tile);
                inventory.clearItem();
                setState(repair);
            }

            circle(type.range/1.8f);
        }
    },
    retreat = new UnitState() {
        public void entered() {
            target = null;
        }

        public void update() {
            if(health >= health){
                state.set(attack);
            }else if(!targetHasFlag(BlockFlag.repair)){
                if(timer.get(timerTarget, 20)) {
                    Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, BlockFlag.repair));
                    if (target != null) Drone.this.target = target.entity;
                }
            }else{
                circle(40f);
            }
        }
    };

}
