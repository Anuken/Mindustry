package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class MechFactory extends Block{
    protected Mech mech;
    protected float buildTime = 60*5;

    protected TextureRegion openRegion;

    public MechFactory(String name){
        super(name);
        update = true;
        consumesTap = true;
        solidifes = true;
    }

    @Override
    public boolean isSolidFor(Tile tile) {
        MechFactoryEntity entity = tile.entity();
        return !entity.open;
    }

    @Override
    public void tapped(Tile tile, Player player) {

        if(checkValidTap(tile, player)){
            CallBlocks.onMechFactoryBegin(player, tile);
        }
    }

    @Override
    public void load() {
        super.load();
        openRegion = Draw.region(name + "-open");
    }

    @Override
    public void draw(Tile tile) {
        MechFactoryEntity entity = tile.entity();

        Draw.rect(entity.open ? openRegion : Draw.region(name), tile.drawx(), tile.drawy());

        if(entity.player != null) {
            TextureRegion region = mech.iconRegion;

            if(entity.player.mech == mech){
                region = (entity.player.isMobile ? Mechs.starterMobile : Mechs.starterDesktop).iconRegion;
            }

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.time = -entity.time / 4f;
            Shaders.build.color.set(Palette.accent);

            Graphics.shader(Shaders.build, false);
            Shaders.build.apply();
            Draw.rect(region, tile.drawx(), tile.drawy());
            Graphics.shader();

            Draw.color(Palette.accent);

            Lines.lineAngleCenter(
                    tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize / 3f * size),
                    tile.drawy(),
                    90,
                    size * Vars.tilesize / 2f + 1f);

            Draw.reset();
        }
    }

    @Override
    public void update(Tile tile) {
        MechFactoryEntity entity = tile.entity();

        if(entity.open){
            if(!Units.anyEntities(tile)){
                entity.open = false;
            }else{
                entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
            }
        }

        if(entity.player != null){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.progress += 1f / buildTime;

            entity.time += entity.heat;

            if(entity.progress >= 1f){
                CallBlocks.onMechFactoryDone(tile);
            }
        }else{
            if(Units.anyEntities(tile, 4f, unit -> unit.getTeam() == entity.getTeam() && unit instanceof Player)){
                entity.open = true;
            }

            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity getEntity() {
        return new MechFactoryEntity();
    }

    @Remote(targets = Loc.both, called = Loc.server, in = In.blocks, forward = true)
    public static void onMechFactoryBegin(Player player, Tile tile){
        if(!checkValidTap(tile, player)) return;

        MechFactoryEntity entity = tile.entity();
        entity.progress = 0f;
        entity.player = player;

        player.rotation = 90f;
        player.baseRotation = 90f;
        player.set(entity.x, entity.y);
        player.setDead(true);
        player.setRespawning(true);
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onMechFactoryDone(Tile tile){
        MechFactoryEntity entity = tile.entity();

        Effects.effect(Fx.spawn, entity);
        Mech result = ((MechFactory)tile.block()).mech;

        if(entity.player.mech == result){
            entity.player.mech = (entity.player.isMobile ? Mechs.starterMobile : Mechs.starterDesktop);
        }else{
            entity.player.mech = result;
        }

        entity.progress = 0;
        entity.player.heal();
        entity.open = true;
        entity.player.setDead(false);
        entity.player.inventory.clear();
        entity.player = null;
    }

    protected static boolean checkValidTap(Tile tile, Player player){
        MechFactoryEntity entity = tile.entity();
        return Math.abs(player.x - tile.drawx()) <= tile.block().size * tilesize / 2f &&
                Math.abs(player.y - tile.drawy()) <= tile.block().size * tilesize / 2f && entity.player == null;
    }

    public class MechFactoryEntity extends TileEntity{
        public Player player;
        public float progress;
        public float time;
        public float heat;
        public boolean open;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(progress);
            stream.writeFloat(time);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            progress = stream.readFloat();
            time = stream.readFloat();
            heat = stream.readFloat();
        }
    }
}
