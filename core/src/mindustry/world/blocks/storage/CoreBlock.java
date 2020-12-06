package mindustry.world.blocks.storage;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    //hacky way to pass item modules between methods
    private static ItemModule nextItems;

    public UnitType unitType = UnitTypes.alpha;

    public final int timerResupply = timers++;

    public int launchRange = 1;

    public int ammoAmount = 5;
    public float resupplyRate = 10f;
    public float resupplyRange = 60f;
    public Item resupplyItem = Items.copper;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        priority = TargetPriority.core;
        flags = EnumSet.of(BlockFlag.core, BlockFlag.unitModifier);
        unitCapModifier = 10;
        loopSound = Sounds.respawning;
        loopSoundVolume = 1f;
        group = BlockGroup.none;
    }

    @Remote(called = Loc.server)
    public static void playerSpawn(Tile tile, Player player){
        if(player == null || tile == null) return;

        CoreBuild entity = tile.bc();
        CoreBlock block = (CoreBlock)tile.block();
        Fx.spawn.at(entity);

        player.set(entity);

        if(!net.client()){
            Unit unit = block.unitType.create(tile.team());
            unit.set(entity);
            unit.rotation(90f);
            unit.impulse(0f, 3f);
            unit.controller(player);
            unit.spawnedByCore(true);
            unit.add();
        }

        if(state.isCampaign() && player == Vars.player){
            block.unitType.unlock();
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.buildTime);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("capacity", (CoreBuild e) -> new Bar(
            () -> Core.bundle.format("bar.capacity", UI.formatAmount(e.storageCapacity)),
            () -> Pal.items,
            () -> e.items.total() / ((float)e.storageCapacity * content.items().count(i -> i.unlockedNow()))
        ));
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    @Override
    public boolean canReplace(Block other){
        //coreblocks can upgrade smaller cores
        return super.canReplace(other) || (other instanceof CoreBlock && size > other.size);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team){
        if(tile == null) return false;
        CoreBuild core = team.core();
        //must have all requirements
        if(core == null || (!state.rules.infiniteResources && !core.items.has(requirements))) return false;
        return tile.block() instanceof CoreBlock && size > tile.block().size;
    }

    @Override
    public void placeBegan(Tile tile, Block previous){
        //finish placement immediately when a block is replaced.
        if(previous instanceof CoreBlock){
            tile.setBlock(this, tile.team());
            Fx.placeBlock.at(tile, tile.block().size);
            Fx.upgradeCore.at(tile, tile.block().size);

            //set up the correct items
            if(nextItems != null){
                //force-set the total items
                if(tile.team().core() != null){
                    tile.team().core().items.set(nextItems);
                }

                nextItems = null;
            }
        }
    }

    @Override
    public void beforePlaceBegan(Tile tile, Block previous){
        if(tile.build instanceof CoreBuild){
            //right before placing, create a "destination" item array which is all the previous items minus core requirements
            ItemModule items = tile.build.items.copy();
            if(!state.rules.infiniteResources){
                items.remove(ItemStack.mult(requirements, state.rules.buildCostMultiplier));
            }

            nextItems = items;
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(world.tile(x, y) == null) return;

        if(!canPlaceOn(world.tile(x, y), player.team())){

            drawPlaceText(Core.bundle.get((player.team().core() != null && player.team().core().items.has(requirements, state.rules.buildCostMultiplier)) || state.rules.infiniteResources ?
                "bar.corereq" :
                "bar.noresources"
            ), x, y, valid);

        }
    }

    public class CoreBuild extends Building implements ControlBlock{
        public int storageCapacity;
        //note that this unit is never actually used for control; the possession handler makes the player respawn when this unit is controlled
        public BlockUnitc unit = Nulls.blockUnit;
        public boolean noEffect = false;

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity) return storageCapacity;
            return super.sense(sensor);
        }

        @Override
        public void created(){
            unit = (BlockUnitc)UnitTypes.block.create(team);
            unit.tile(this);
        }

        @Override
        public Unit unit(){
            return (Unit)unit;
        }

        public void requestSpawn(Player player){
            Call.playerSpawn(tile, player);
        }

        @Override
        public void updateTile(){

            //resupply nearby units
            if(items.has(resupplyItem) && timer(timerResupply, resupplyRate) && ResupplyPoint.resupply(this, resupplyRange, ammoAmount, resupplyItem.color)){
                items.remove(resupplyItem, 1);
            }
        }

        @Override
        public boolean canPickup(){
            //cores can never be picked up
            return false;
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();

            //add a spawn to the map for future reference - waves should be disabled, so it shouldn't matter
            if(state.isCampaign() && team == state.rules.waveTeam && team.cores().size <= 1){
                //do not recache
                tile.setOverlayQuiet(Blocks.spawn);

                if(!spawner.getSpawns().contains(tile)){
                    spawner.getSpawns().add(tile);
                }
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 30f + 20f * size, Pal.accent, 0.65f + Mathf.absin(20f, 0.1f));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return incinerate() ? storageCapacity * 2 : storageCapacity;
        }

        @Override
        public void onProximityUpdate(){
            for(Building other : state.teams.cores(team)){
                if(other.tile() != tile){
                    this.items = other.items;
                }
            }
            state.teams.registerCore(this);

            storageCapacity = itemCapacity + proximity().sum(e -> owns(e) ? e.block.itemCapacity : 0);
            proximity.each(e -> owns(e), t -> {
                t.items = items;
                ((StorageBuild)t).linkedCore = this;
            });

            for(Building other : state.teams.cores(team)){
                if(other.tile() == tile) continue;
                storageCapacity += other.block.itemCapacity + other.proximity().sum(e -> owns(e) && owns(other, e) ? e.block.itemCapacity : 0);
            }

            //Team.sharded.core().items.set(Items.surgeAlloy, 12000)
            if(!world.isGenerating()){
                for(Item item : content.items()){
                    items.set(item, Math.min(items.get(item), storageCapacity));
                }
            }

            for(CoreBuild other : state.teams.cores(team)){
                other.storageCapacity = storageCapacity;
            }
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            int realAmount = Math.min(amount, storageCapacity - items.get(item));
            super.handleStack(item, realAmount, source);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                state.rules.sector.info.handleCoreItem(item, amount);

                if(realAmount == 0){
                    Fx.coreBurn.at(x, y);
                }
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                state.rules.sector.info.handleCoreItem(item, -result);
            }

            return result;
        }

        @Override
        public void drawSelect(){
            Lines.stroke(1f, Pal.accent);
            Cons<Building> outline = t -> {
                for(int i = 0; i < 4; i++){
                    Point2 p = Geometry.d8edge[i];
                    float offset = -Math.max(t.block.size - 1, 0) / 2f * tilesize;
                    Draw.rect("block-select", t.x + offset * p.x, t.y + offset * p.y, i * 90);
                }
            };
            if(proximity.contains(e -> owns(e) && e.items == items)){
                outline.get(this);
            }
            proximity.each(e -> owns(e) && e.items == items, outline);
            Draw.reset();
        }

        public boolean owns(Building tile){
            return owns(this, tile);
        }

        public boolean owns(Building core, Building tile){
            return tile instanceof StorageBuild b && (b.linkedCore == core || b.linkedCore == null);
        }

        public boolean incinerate(){
            return state.isCampaign();
        }

        @Override
        public float handleDamage(float amount){
            if(player != null && team == player.team()){
                Events.fire(Trigger.teamCoreDamage);
            }
            return amount;
        }

        @Override
        public void onRemoved(){
            int total = proximity.count(e -> e.items != null && e.items == items);
            float fract = 1f / total / state.teams.cores(team).size;

            proximity.each(e -> owns(e) && e.items == items && owns(e), t -> {
                StorageBuild ent = (StorageBuild)t;
                ent.linkedCore = null;
                ent.items = new ItemModule();
                for(Item item : content.items()){
                    ent.items.set(item, (int)(fract * items.get(item)));
                }
            });

            state.teams.unregisterCore(this);

            int max = itemCapacity * state.teams.cores(team).size;
            for(Item item : content.items()){
                items.set(item, Math.min(items.get(item), max));
            }

            for(CoreBuild other : state.teams.cores(team)){
                other.onProximityUpdate();
            }
        }

        @Override
        public void placed(){
            super.placed();
            state.teams.registerCore(this);
        }

        @Override
        public void itemTaken(Item item){
            if(state.isCampaign() && team == state.rules.defaultTeam){
                //update item taken amount
                state.rules.sector.info.handleCoreItem(item, -1);
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            if(net.server() || !net.active()){
                if(team == state.rules.defaultTeam && state.isCampaign()){
                    state.rules.sector.info.handleCoreItem(item, 1);
                }

                if(items.get(item) >= storageCapacity){
                    //create item incineration effect at random intervals
                    if(!noEffect){
                        incinerateEffect(this, source);
                    }
                    noEffect = false;
                }else{
                    super.handleItem(source, item);
                }
            }
        }
    }
}
