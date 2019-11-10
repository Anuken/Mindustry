package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;
import io.anuke.mindustry.world.modules.*;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected Mech mech = Mechs.starter;

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
    public static void onUnitRespawn(Tile tile, Player player){
        if(player == null || tile.entity == null) return;

        CoreEntity entity = tile.entity();
        Effects.effect(Fx.spawn, entity);
        entity.progress = 0;
        entity.spawnPlayer = player;
        entity.spawnPlayer.onRespawn(tile);
        entity.spawnPlayer.applyImpulse(0, 8f);
        entity.spawnPlayer = null;
    }

    @Override
    public void setStats(){
        super.setStats();

        bars.add("capacity", e ->
            new Bar(
                () -> Core.bundle.format("bar.capacity", ui.formatAmount(((CoreEntity)e).storageCapacity)),
                () -> Pal.items,
                () -> e.items.total() / (float)(((CoreEntity)e).storageCapacity * content.items().count(i -> i.type == ItemType.material))
            ));
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        CoreEntity entity = tile.entity();
        return item.type == ItemType.material ? entity.storageCapacity : 0;
    }

    @Override
    public void onProximityUpdate(Tile tile){
        CoreEntity entity = tile.entity();

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other != tile){
                entity.items = other.entity.items;
            }
        }
        state.teams.get(tile.getTeam()).cores.add(tile);

        entity.storageCapacity = itemCapacity + entity.proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
        entity.proximity().each(this::isContainer, t -> {
            t.entity.items = entity.items;
            t.<StorageBlockEntity>entity().linkedCore = tile;
        });

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other == tile) continue;
            entity.storageCapacity += other.block().itemCapacity + other.entity.proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
        }

        if(!world.isGenerating()){
            for(Item item : content.items()){
                entity.items.set(item, Math.min(entity.items.get(item), entity.storageCapacity));
            }
        }

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            CoreEntity oe = other.entity();
            oe.storageCapacity = entity.storageCapacity;
        }
    }

    @Override
    public void drawSelect(Tile tile){
        Lines.stroke(1f, Pal.accent);
        Cons<Tile> outline = t -> {
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float offset = -Math.max(t.block().size - 1, 0) / 2f * tilesize;
                Draw.rect("block-select", t.drawx() + offset * p.x, t.drawy() + offset * p.y, i * 90);
            }
        };
        if(tile.entity.proximity().contains(e -> isContainer(e) && e.entity.items == tile.entity.items)){
            outline.get(tile);
        }
        tile.entity.proximity().each(e -> isContainer(e) && e.entity.items == tile.entity.items, outline);
        Draw.reset();
    }


    public boolean isContainer(Tile tile){
        return tile.entity instanceof StorageBlockEntity;
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    @Override
    public void removed(Tile tile){
        int total = tile.entity.proximity().count(e -> e.entity.items == tile.entity.items);
        float fract = 1f / total / state.teams.get(tile.getTeam()).cores.size;

        tile.entity.proximity().each(e -> isContainer(e) && e.entity.items == tile.entity.items, t -> {
            StorageBlockEntity ent = (StorageBlockEntity)t.entity;
            ent.linkedCore = null;
            ent.items = new ItemModule();
            for(Item item : content.items()){
                ent.items.set(item, (int)(fract * tile.entity.items.get(item)));
            }
        });

        state.teams.get(tile.getTeam()).cores.remove(tile);

        int max = itemCapacity * state.teams.get(tile.getTeam()).cores.size;
        for(Item item : content.items()){
            tile.entity.items.set(item, Math.min(tile.entity.items.get(item), max));
        }

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            other.block().onProximityUpdate(other);
        }
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public void drawLayer(Tile tile){
        CoreEntity entity = tile.entity();

        if(entity.heat > 0.001f){
            RespawnBlock.drawRespawn(tile, entity.heat, entity.progress, entity.time, entity.spawnPlayer, mech);
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(net.server() || !net.active()){
            super.handleItem(item, tile, source);
            if(state.rules.tutorial){
                Events.fire(new CoreItemDeliverEvent());
            }
        }
    }

    @Override
    public void update(Tile tile){
        CoreEntity entity = tile.entity();

        if(entity.spawnPlayer != null){
            if(!entity.spawnPlayer.isDead() || !entity.spawnPlayer.isAdded()){
                entity.spawnPlayer = null;
                return;
            }

            entity.spawnPlayer.set(tile.drawx(), tile.drawy());
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += entity.delta();
            entity.progress += 1f / state.rules.respawnTime * entity.delta();

            if(entity.progress >= 1f){
                Call.onUnitRespawn(tile, entity.spawnPlayer);
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public boolean shouldActiveSound(Tile tile){
        CoreEntity entity = tile.entity();

        return entity.spawnPlayer != null;
    }

    @Override
    public TileEntity newEntity(){
        return new CoreEntity();
    }

    public class CoreEntity extends TileEntity implements SpawnerTrait{
        protected Player spawnPlayer;
        protected float progress;
        protected float time;
        protected float heat;
        protected int storageCapacity;

        @Override
        public boolean hasUnit(Unit unit){
            return unit == spawnPlayer;
        }

        @Override
        public void updateSpawning(Player player){
            if(!netServer.isWaitingForPlayers() && spawnPlayer == null){
                spawnPlayer = player;
                progress = 0f;
                player.mech = mech;
                player.beginRespawning(this);
            }
        }
    }
}
