package mindustry.world.blocks.storage;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    public UnitType unitType = UnitTypes.alpha;

    public final int timerResupply = timers++;

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
        flags = EnumSet.of(BlockFlag.core, BlockFlag.producer, BlockFlag.unitModifier);
        unitCapModifier = 10;
        activeSound = Sounds.respawning;
        activeSoundVolume = 1f;
    }

    @Remote(called = Loc.server)
    public static void onPlayerSpawn(Tile tile, Playerc player){
        if(player == null || tile == null) return;

        CoreEntity entity = tile.ent();
        CoreBlock block = (CoreBlock)tile.block();
        Fx.spawn.at(entity);

        if(!net.client()){
            Unitc unit = block.unitType.create(tile.team());
            unit.set(entity);
            unit.rotation(90f);
            unit.impulse(0f, 3f);
            unit.controller(player);
            unit.spawnedByCore(true);
            unit.add();
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        bars.add("capacity", e ->
            new Bar(
                () -> Core.bundle.format("bar.capacity", ui.formatAmount(((CoreEntity)e).storageCapacity)),
                () -> Pal.items,
                () -> e.items().total() / (float)(((CoreEntity)e).storageCapacity * content.items().count(i -> i.type == ItemType.material))
            ));

        bars.add("units", e ->
            new Bar(
                () -> Core.bundle.format("bar.units", teamIndex.count(e.team()), Units.getCap(e.team())),
                () -> Pal.power,
                () -> (float)teamIndex.count(e.team()) / Units.getCap(e.team())
            ));
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    public class CoreEntity extends TileEntity implements ControlBlock{
        public int storageCapacity;
        //note that this unit is never actually used for control; the possession handler makes the player respawn when this unit is controlled
        public @NonNull BlockUnitc unit = Nulls.blockUnit;

        @Override
        public void created(){
            unit = (BlockUnitc)UnitTypes.block.create(team);
            unit.tile(this);
        }

        @Override
        public Unitc unit(){
            return unit;
        }

        public void requestSpawn(Playerc player){
            Call.onPlayerSpawn(tile, player);
        }

        @Override
        public void updateTile(){

            //resupply nearby units
            if(items.has(resupplyItem) && timer(timerResupply, resupplyRate) && ResupplyPoint.resupply(this, resupplyRange, ammoAmount, resupplyItem.color)){
                items.remove(resupplyItem, 1);
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 30f * size, Pal.accent, 0.5f + Mathf.absin(20f, 0.1f));
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return item.type == ItemType.material ? storageCapacity : 0;
        }

        @Override
        public void onProximityUpdate(){
            for(Tilec other : state.teams.cores(team)){
                if(other.tile() != tile){
                    items(other.items());
                }
            }
            state.teams.registerCore(this);

            storageCapacity = itemCapacity + proximity().sum(e -> isContainer(e) && owns(e) ? e.block().itemCapacity : 0);
            proximity.each(e -> isContainer(e) && owns(e), t -> {
                t.items(items);
                ((StorageBlockEntity)t).linkedCore = this;
            });

            for(Tilec other : state.teams.cores(team)){
                if(other.tile() == tile) continue;
                storageCapacity += other.block().itemCapacity + other.proximity().sum(e -> isContainer(e) && owns(other, e) ? e.block().itemCapacity : 0);
            }

            if(!world.isGenerating()){
                for(Item item : content.items()){
                    items.set(item, Math.min(items.get(item), storageCapacity));
                }
            }

            for(CoreEntity other : state.teams.cores(team)){
                other.storageCapacity = storageCapacity;
            }
        }

        @Override
        public void drawSelect(){
            Lines.stroke(1f, Pal.accent);
            Cons<Tilec> outline = t -> {
                for(int i = 0; i < 4; i++){
                    Point2 p = Geometry.d8edge[i];
                    float offset = -Math.max(t.block().size - 1, 0) / 2f * tilesize;
                    Draw.rect("block-select", t.x() + offset * p.x, t.y() + offset * p.y, i * 90);
                }
            };
            if(proximity.contains(e -> isContainer(e) && e.items() == items)){
                outline.get(this);
            }
            proximity.each(e -> isContainer(e) && e.items() == items, outline);
            Draw.reset();
        }


        public boolean isContainer(Tilec tile){
            return tile instanceof StorageBlockEntity && (((StorageBlockEntity)tile).linkedCore == this || ((StorageBlockEntity)tile).linkedCore == null);
        }

        public boolean owns(Tilec tile){
            return tile instanceof StorageBlockEntity && (((StorageBlockEntity)tile).linkedCore == this || ((StorageBlockEntity)tile).linkedCore == null);
        }

        public boolean owns(Tilec core, Tilec tile){
            return tile instanceof StorageBlockEntity && (((StorageBlockEntity)tile).linkedCore == core || ((StorageBlockEntity)tile).linkedCore == null);
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
            int total = proximity.count(e -> e.items() != null && e.items() == items);
            float fract = 1f / total / state.teams.cores(team).size;

            proximity.each(e -> isContainer(e) && e.items() == items && owns(e), t -> {
                StorageBlockEntity ent = (StorageBlockEntity)t;
                ent.linkedCore = null;
                ent.items(new ItemModule());
                for(Item item : content.items()){
                    ent.items().set(item, (int)(fract * items.get(item)));
                }
            });

            state.teams.unregisterCore(this);

            int max = itemCapacity * state.teams.cores(team).size;
            for(Item item : content.items()){
                items.set(item, Math.min(items.get(item), max));
            }

            for(CoreEntity other : state.teams.cores(team)){
                other.onProximityUpdate();
            }
        }

        @Override
        public void placed(){
            super.placed();
            state.teams.registerCore(this);
        }

        @Override
        public void handleItem(Tilec source, Item item){
            if(net.server() || !net.active()){
                super.handleItem(source, item);
                if(state.rules.tutorial){
                    Events.fire(new CoreItemDeliverEvent());
                }
            }
        }
    }
}
