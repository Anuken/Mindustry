package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected Mech mech = Mechs.starter;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        flags = EnumSet.of(BlockFlag.target, BlockFlag.producer);
        activeSound = Sounds.respawning;
        activeSoundVolume = 1f;
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
    public int getMaximumAccepted(Tile tile, Item item){
        return item.type == ItemType.material ? itemCapacity * state.teams.get(tile.getTeam()).cores.size : 0;
    }

    @Override
    public void onProximityUpdate(Tile tile){
        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other != tile){
                tile.entity.items = other.entity.items;
            }
        }
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    @Override
    public void removed(Tile tile){
        state.teams.get(tile.getTeam()).cores.remove(tile);

        int max = itemCapacity * state.teams.get(tile.getTeam()).cores.size;
        for(Item item : content.items()){
            tile.entity.items.set(item, Math.min(tile.entity.items.get(item), max));
        }
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public void draw(Tile tile){
        CoreEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());

        if(entity.heat > 0){
            Draw.color(Pal.darkMetal);
            Lines.stroke(2f * entity.heat);
            Lines.poly(tile.drawx(), tile.drawy(), 4, 8f * entity.heat);
            Draw.reset();
        }

        if(entity.spawnPlayer != null){
            Unit player = entity.spawnPlayer;

            TextureRegion region = player.getIconRegion();

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.color.set(Pal.accent);
            Shaders.build.time = -entity.time / 10f;

            Draw.shader(Shaders.build, true);
            Draw.rect(region, tile.drawx(), tile.drawy());
            Draw.shader();

            Draw.color(Pal.accent);

            Lines.lineAngleCenter(
                tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize / 3f * size),
                tile.drawy(),
                90,
                size * Vars.tilesize / 2f);

            Draw.reset();
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()){
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
        public Player spawnPlayer;
        float progress;
        float time;
        float heat;

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
