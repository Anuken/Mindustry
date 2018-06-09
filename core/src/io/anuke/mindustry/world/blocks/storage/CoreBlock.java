package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.debug;
import static io.anuke.mindustry.Vars.state;

public class CoreBlock extends StorageBlock {
    private static Rectangle rect = new Rectangle();

    protected int timerSupply = timers ++;

    protected float supplyRadius = 50f;
    protected float supplyInterval = 5f;

    public CoreBlock(String name) {
        super(name);

        solid = false;
        solidifes = true;
        update = true;
        unbreakable = true;
        size = 3;
        hasItems = true;
        itemCapacity = 1000;
        flags = EnumSet.of(BlockFlag.resupplyPoint, BlockFlag.target);
    }

    @Override
    public void draw(Tile tile) {
        CoreEntity entity = tile.entity();

        Draw.rect(entity.solid ? name : name + "-open", tile.drawx(), tile.drawy());

        Draw.alpha(entity.heat);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
        Draw.color();

        if(entity.currentPlayer != null) {
            Player player = entity.currentPlayer;

            TextureRegion region = Draw.region(player.mech.name);

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.color.set(Palette.accent);
            Shaders.build.time = -entity.time / 10f;

            Graphics.shader(Shaders.build, false);
            Shaders.build.apply();
            Draw.rect(region, tile.drawx(), tile.drawy());
            Graphics.shader();

            Draw.color(Palette.accent);

            Lines.lineAngleCenter(
                    tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize / 3f * size),
                    tile.drawy(),
                    90,
                    size * Vars.tilesize /2f);

            Draw.reset();
        }
    }

    @Override
    public boolean isSolidFor(Tile tile) {
        CoreEntity entity = tile.entity();

        return entity.solid;
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && source.getTeam() == tile.getTeam()){
            return Math.min(itemCapacity - tile.entity.items.getItem(item), amount);
        }else{
            return 0;
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.items.items[item.id]< itemCapacity && item.type == ItemType.material;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), supplyRadius);
        Draw.color();
    }

    @Override
    public void onDestroyed(Tile tile){
        //TODO more dramatic effects
        super.onDestroyed(tile);

        if(state.teams.has(tile.getTeam())){
            state.teams.get(tile.getTeam()).cores.removeValue(tile, true);
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()) super.handleItem(item, tile, source);
    }

    @Override
    public void update(Tile tile) {
        CoreEntity entity = tile.entity();

        if(!entity.solid && !Units.anyEntities(tile)){
            CallBlocks.setCoreSolid(tile, true);
        }

        if(entity.currentPlayer != null){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += Timers.delta();
            entity.progress += 1f / Vars.respawnduration;

            //instant build for fast testing.
            if(debug){
           //     entity.progress = 1f;
            }

            if(entity.progress >= 1f){
                Effects.effect(Fx.spawn, entity);
                CallBlocks.setCoreSolid(tile, false);
                entity.progress = 0;
                entity.currentPlayer.heal();
                entity.currentPlayer.rotation = 90f;
                entity.currentPlayer.baseRotation = 90f;
                entity.currentPlayer.set(tile.drawx(), tile.drawy());
                entity.currentPlayer.add();
                entity.currentPlayer = null;
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }

        if(entity.solid && tile.entity.timer.get(timerSupply, supplyInterval)){
            rect.setSize(supplyRadius*2).setCenter(tile.drawx(), tile.drawy());

            Units.getNearby(tile.getTeam(), rect, unit -> {
                if(unit.isDead() || unit.distanceTo(tile.drawx(), tile.drawy()) > supplyRadius) return;

                for(int i = 0; i < tile.entity.items.items.length; i ++){
                    Item item = Item.getByID(i);
                    if(tile.entity.items.items[i] > 0 && unit.acceptsAmmo(item)){
                        tile.entity.items.items[i] --;
                        unit.addAmmo(item);
                        CallEntity.transferAmmo(item, tile.drawx(), tile.drawy(), unit);
                        return;
                    }
                }
            });
        }
    }

    @Override
    public TileEntity getEntity() {
        return new CoreEntity();
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void setCoreSolid(Tile tile, boolean solid){
        CoreEntity entity = tile.entity();
        entity.solid = solid;
    }

    public class CoreEntity extends TileEntity{
        Player currentPlayer;
        boolean solid = true;
        float progress;
        float time;
        float heat;

        public boolean trySetPlayer(Player player){
            if(currentPlayer != null) return false;
            player.set(tile.drawx(), tile.drawy());
            currentPlayer = player;
            progress = 0f;
            return true;
        }

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeBoolean(solid);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            solid = stream.readBoolean();
        }
    }
}
