package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePowerExact;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.mobile;
import static io.anuke.mindustry.Vars.tilesize;

public class MechFactory extends Block{
    protected Mech mech;
    protected float buildTime = 60 * 5;

    protected TextureRegion openRegion;

    public MechFactory(String name){
        super(name);
        update = true;
        solidifes = true;
        hasPower = true;
    }

    @Override
    public void init(){
        consumes.add(new ConsumePowerExact(powerCapacity * 0.8f));
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(BlockStat.powerUse);
    }

    @Override
    public boolean shouldConsume(Tile tile){
        return false;
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void onMechFactoryTap(Player player, Tile tile){
        if(player == null || !checkValidTap(tile, player)) return;

        MechFactoryEntity entity = tile.entity();
        entity.power.amount = 0f;
        player.beginRespawning(entity);
    }

    @Remote(called = Loc.server)
    public static void onMechFactoryDone(Tile tile){
        MechFactoryEntity entity = tile.entity();

        Effects.effect(Fx.spawn, entity);

        if(entity.player == null) return;

        Mech result = ((MechFactory) tile.block()).mech;

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
                Math.abs(player.y - tile.drawy()) <= tile.block().size * tilesize / 2f && entity.cons.valid() && entity.player == null;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        for(int i = 0; i < 4; i ++){
            float length = tilesize * size/2f + 3 + Mathf.absin(Timers.time(), 5f, 2f);
            Draw.rect("transfer-arrow", tile.drawx() + Geometry.d4[i].x * length, tile.drawy() + Geometry.d4[i].y * length, (i+2) * 90);
        }
        Draw.color();
    }

    @Override
    public boolean isSolidFor(Tile tile){
        MechFactoryEntity entity = tile.entity();
        return !entity.open;
    }

    @Override
    public void tapped(Tile tile, Player player){
        if(mobile && !mech.flying) return;

        if(checkValidTap(tile, player)){
            Call.onMechFactoryTap(player, tile);
        }else if(player.isLocal && mobile){
            player.moveTarget = tile.entity;
        }
    }

    @Override
    public void load(){
        super.load();
        openRegion = Draw.region(name + "-open");
    }

    @Override
    public void draw(Tile tile){
        MechFactoryEntity entity = tile.entity();

        Draw.rect(Draw.region(name), tile.drawx(), tile.drawy(), entity.open ? 180f : 0f);

        if(entity.player != null){
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
    public void update(Tile tile){
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

            entity.time += 0.5f;

            if(entity.progress >= 1f){
                Call.onMechFactoryDone(tile);
            }
        }else{
            if(entity.cons.valid() && Units.anyEntities(tile, 4f, unit -> unit.getTeam() == entity.getTeam() && unit instanceof Player)){
                entity.open = true;
            }

            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity getEntity(){
        return new MechFactoryEntity();
    }

    public class MechFactoryEntity extends TileEntity implements SpawnerTrait{
        Player player;
        float progress;
        float time;
        float heat;
        boolean open;

        @Override
        public void updateSpawning(Unit unit){
            if(!(unit instanceof Player))
                throw new IllegalArgumentException("Mech factories only accept player respawners.");

            if(player == null){
                progress = 0f;
                player = (Player) unit;

                player.rotation = 90f;
                player.baseRotation = 90f;
                player.set(x, y);
                player.beginRespawning(this);
            }
        }

        @Override
        public float getSpawnProgress(){
            return progress;
        }

        @Override
        public void write(DataOutputStream stream) throws IOException{
            stream.writeFloat(progress);
            stream.writeFloat(time);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            progress = stream.readFloat();
            time = stream.readFloat();
            heat = stream.readFloat();
        }
    }
}
