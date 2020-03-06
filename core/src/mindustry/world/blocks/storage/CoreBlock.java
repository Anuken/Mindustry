package mindustry.world.blocks.storage;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    public UnitType mech = UnitTypes.starter;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        flags = EnumSet.of(BlockFlag.core, BlockFlag.producer);
        activeSound = Sounds.respawning;
        activeSoundVolume = 1f;
        layer = Layer.overlay;
    }

    @Remote(called = Loc.server)
    public static void onUnitRespawn(Tilec tile, Playerc player){
        if(player == null || tile == null) return;

        //TODO really fix
        Fx.spawn.at(tile);
        //progress = 0;
        //spawnPlayer = player;
        //TODO fix
        //spawnPlayer.onRespawn(tile);
        //spawnPlayer.applyImpulse(0, 8f);
        //spawnPlayer = null;
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
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    public class CoreEntity extends TileEntity{
       // protected Playerc spawnPlayer;
        //protected float progress;
        protected float time;
        protected float heat;
        protected int storageCapacity;

        @Override
        public void drawLight(){
            renderer.lights.add(x, y, 30f * size, Pal.accent, 0.5f + Mathf.absin(20f, 0.1f));
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

            storageCapacity = itemCapacity + proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
            proximity.each(this::isContainer, t -> {
                t.items(items);
                ((StorageBlockEntity)t).linkedCore = tile;
            });

            for(Tilec other : state.teams.cores(team)){
                if(other.tile() == tile) continue;
                storageCapacity += other.block().itemCapacity + other.proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
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
            return tile instanceof StorageBlockEntity;
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

            proximity.each(e -> isContainer(e) && e.items() == items, t -> {
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
        public void drawLayer(){
            if(heat > 0.001f){
                //TODO implement
                //RespawnBlock.drawRespawn(tile, heat, progress, time, spawnPlayer, mech);
            }
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

        @Override
        public void updateTile(){
            //TODO implement
        /*
        if(spawnPlayer != null){
            if(!spawnPlayer.dead() || !spawnPlayer.isAdded()){
                spawnPlayer = null;
                return;
            }

            spawnPlayer.set(x, y);
            heat = Mathf.lerpDelta(heat, 1f, 0.1f);
            time += delta();
            progress += 1f / state.rules.respawnTime * delta();

            if(progress >= 1f){
                Call.onUnitRespawn(tile, spawnPlayer);
            }
        }else{
            heat = Mathf.lerpDelta(heat, 0f, 0.1f);
        }*/
        }

        @Override
        public boolean shouldActiveSound(){
            //TODO
            return false;//spawnPlayer != null;
        }
    }
}
