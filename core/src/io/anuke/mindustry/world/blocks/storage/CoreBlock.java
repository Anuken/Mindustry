package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.mindustry.entities.Effects;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected TextureRegion topRegion;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        flags = EnumSet.of(BlockFlag.target);
    }

    @Remote(called = Loc.server)
    public static void onUnitRespawn(Tile tile, Player player){
        if(player == null || tile.entity == null) return;

        CoreEntity entity = tile.entity();
        Effects.effect(Fx.spawn, entity);
        entity.progress = 0;
        entity.currentUnit.onRespawn(tile);
        entity.currentUnit = player;
        entity.currentUnit.heal();
        entity.currentUnit.rotation = 90f;
        entity.currentUnit.applyImpulse(0, 8f);
        entity.currentUnit.setNet(tile.drawx(), tile.drawy());
        entity.currentUnit.add();
        entity.currentUnit = null;

        player.endRespawning();
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity * state.teams.get(tile.getTeam()).cores.size;
    }

    @Override
    public void onProximityUpdate(Tile tile) {
        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other != tile){
                tile.entity.items = other.entity.items;
            }
        }
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public boolean canBreak(Tile tile){
        return state.teams.get(tile.getTeam()).cores.size > 1;
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
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        CoreEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());

        Draw.alpha(entity.heat);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.color();

        if(entity.currentUnit != null){
            Unit player = entity.currentUnit;

            TextureRegion region = player.getIconRegion();

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.color.set(Palette.accent);
            Shaders.build.time = -entity.time / 10f;

            Draw.shader(Shaders.build, true);
            Draw.rect(region, tile.drawx(), tile.drawy());
            Draw.shader();

            Draw.color(Palette.accent);

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
        if(Net.server() || !Net.active()) super.handleItem(item, tile, source);
    }

    @Override
    public void update(Tile tile){
        CoreEntity entity = tile.entity();

        if(entity.currentUnit != null){
            if(!entity.currentUnit.isDead() || !entity.currentUnit.isAdded()){
                entity.currentUnit = null;
                return;
            }

            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += entity.delta();
            entity.progress += 1f / state.rules.respawnTime * entity.delta();

            if(entity.progress >= 1f){
                Call.onUnitRespawn(tile, entity.currentUnit);
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new CoreEntity();
    }

    public class CoreEntity extends TileEntity implements SpawnerTrait{
        public Player currentUnit;
        float progress;
        float time;
        float heat;

        @Override
        public void updateSpawning(Player unit){
            if(!netServer.isWaitingForPlayers() && currentUnit == null){
                currentUnit = unit;
                progress = 0f;
                unit.set(tile.drawx(), tile.drawy());
            }
        }
    }
}
