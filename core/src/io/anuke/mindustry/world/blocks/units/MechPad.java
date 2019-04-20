package io.anuke.mindustry.world.blocks.units;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.*;

import static io.anuke.mindustry.Vars.mobile;
import static io.anuke.mindustry.Vars.tilesize;

public class MechPad extends Block{
    protected Mech mech;
    protected float buildTime = 60 * 5;
    protected float requiredSatisfaction = 0.999f;

    protected TextureRegion openRegion;

    public MechPad(String name){
        super(name);
        update = true;
        solid = false;
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.productionTime, buildTime / 60f, StatUnit.seconds);
    }

    @Override
    public void init(){
        super.init();
    }

    @Override
    public boolean shouldConsume(Tile tile){
        return false;
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void onMechFactoryTap(Player player, Tile tile){
        if(player == null || !(tile.block() instanceof MechPad) || !checkValidTap(tile, player)) return;

        MechFactoryEntity entity = tile.entity();
        MechPad pad = (MechPad)tile.block();

        if(entity.power.satisfaction < pad.requiredSatisfaction) return;

        entity.power.satisfaction -= Math.min(entity.power.satisfaction, pad.requiredSatisfaction);
        player.beginRespawning(entity);
    }

    @Remote(called = Loc.server)
    public static void onMechFactoryDone(Tile tile){
        if(!(tile.entity instanceof MechFactoryEntity)) return;

        MechFactoryEntity entity = tile.entity();

        Effects.effect(Fx.spawn, entity);

        if(entity.player == null) return;

        Mech result = ((MechPad)tile.block()).mech;

        if(entity.player.mech == result){
            Mech target = (entity.player.isMobile ? Mechs.starterMobile : Mechs.starterDesktop);
            if(entity.player.mech == target){
                entity.player.mech = (entity.player.isMobile ? Mechs.starterDesktop : Mechs.starterMobile);
            }else{
                entity.player.mech = target;
            }
        }else{
            entity.player.mech = result;
        }

        entity.progress = 0;
        entity.player.heal();
        entity.player.endRespawning();
        entity.player.setDead(false);
        entity.player.clearItem();
        entity.player = null;
    }

    protected static boolean checkValidTap(Tile tile, Player player){
        MechFactoryEntity entity = tile.entity();
        return Math.abs(player.x - tile.drawx()) <= tile.block().size * tilesize / 2f &&
        Math.abs(player.y - tile.drawy()) <= tile.block().size * tilesize / 2f && entity.cons.valid() && entity.player == null;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Pal.accent);
        for(int i = 0; i < 4; i++){
            float length = tilesize * size / 2f + 3 + Mathf.absin(Time.time(), 5f, 2f);
            Draw.rect("transfer-arrow", tile.drawx() + Geometry.d4[i].x * length, tile.drawy() + Geometry.d4[i].y * length, (i + 2) * 90);
        }
        Draw.color();
    }

    @Override
    public void tapped(Tile tile, Player player){
        MechFactoryEntity entity = tile.entity();

        if(checkValidTap(tile, player)){
            Call.onMechFactoryTap(player, tile);
        }else if(player.isLocal && mobile && !player.isDead() && (entity.power.satisfaction >= requiredSatisfaction) && entity.player == null){
            player.moveTarget = tile.entity;
        }
    }

    @Override
    public void load(){
        super.load();
        openRegion = Core.atlas.find(name + "-open");
    }

    @Override
    public void draw(Tile tile){
        MechFactoryEntity entity = tile.entity();

        Draw.rect(Core.atlas.find(name), tile.drawx(), tile.drawy());

        if(entity.player != null){
            TextureRegion region = mech.iconRegion;

            if(entity.player.mech == mech){
                region = (entity.player.mech == Mechs.starterDesktop ? Mechs.starterMobile : Mechs.starterDesktop).iconRegion;
            }

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.time = -entity.time / 5f;
            Shaders.build.color.set(Pal.accent);

            Draw.shader(Shaders.build);
            Draw.rect(region, tile.drawx(), tile.drawy());
            Draw.shader();

            Draw.color(Pal.accent);

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

        if(entity.player != null){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.progress += 1f / buildTime * entity.delta();

            entity.time += 0.5f * entity.delta();

            if(entity.progress >= 1f){
                Call.onMechFactoryDone(tile);
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new MechFactoryEntity();
    }

    public class MechFactoryEntity extends TileEntity implements SpawnerTrait{
        Player player;
        float progress;
        float time;
        float heat;

        @Override
        public void updateSpawning(Player unit){
            if(player == null){
                progress = 0f;
                player = unit;

                player.rotation = 90f;
                player.baseRotation = 90f;
                player.setNet(x, y);
                player.beginRespawning(this);
            }
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(progress);
            stream.writeFloat(time);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            progress = stream.readFloat();
            time = stream.readFloat();
            heat = stream.readFloat();
        }
    }
}
