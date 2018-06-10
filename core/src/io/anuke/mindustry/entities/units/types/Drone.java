package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.EventType.BlockBuildEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.ThreadQueue;

import static io.anuke.mindustry.Vars.unitGroups;
import static io.anuke.mindustry.Vars.world;

public class Drone extends FlyingUnit implements BuilderTrait {
    public static int typeID = -1;

    protected static float healSpeed = 0.1f;
    protected static float discoverRange = 120f;
    protected static boolean initialized;

    protected Tile mineTile;
    protected Queue<BuildRequest> placeQueue = new ThreadQueue<>();

    /**Initialize placement event notifier system.
     * Static initialization is to be avoided, thus, this is done lazily.*/
    private static void initEvents(){
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

    public Drone(UnitType type, Team team) {
        super(type, team);

        if(!initialized){
            initEvents();
            initialized = true;
        }
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
        float rot = rotation;
        super.update();
        rotation = rot;

        if(target != null && state.is(repair)){
            rotation = Mathf.slerpDelta(rot, angleTo(target), 0.3f);
        }else{
            rotation = Mathf.slerpDelta(rot, velocity.angle(), 0.3f);
        }

        x += Mathf.sin(Timers.time() + id * 999, 25f, 0.07f);
        y += Mathf.cos(Timers.time() + id * 999, 25f, 0.07f);

        if(velocity.len() <= 0.2f && !(state.is(repair) && target != null)){
            rotation += Mathf.sin(Timers.time() + id * 99, 10f, 5f);
        }

        updateBuilding(this);
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

    public final UnitState

    build = new UnitState(){

        public void update() {
            BuildEntity entity = (BuildEntity)target;

            if(entity.progress() < 1f && entity.tile.block() instanceof BuildBlock){ //building is valid
                if(!isBuilding() && distanceTo(target) < placeDistance * 0.9f){ //within distance, begin placing
                    getPlaceQueue().addLast(new BuildRequest(entity.tile.x, entity.tile.y, entity.tile.getRotation(), entity.recipe));
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
        public void update() {
            //if inventory is full, drop it off.
            if(inventory.isFull()){
                setState(drop);
            }else{
                //only mines iron for now
                retarget(() -> target = Geometry.findClosest(x, y, world.indexer().getOrePositions(Items.iron)));
            }
        }
    },
    drop = new UnitState() {
        public void update() {

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
