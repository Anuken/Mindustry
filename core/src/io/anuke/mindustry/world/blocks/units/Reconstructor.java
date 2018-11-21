package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

//TODO re-implement properly
public class Reconstructor extends Block{
    protected float departTime = 30f;
    protected float arriveTime = 40f;
    /** Stores the percentage of buffered power to be used upon teleporting. */
    protected float powerPerTeleport = 0.5f;
    protected Effect arriveEffect = Fx.spawn;
    protected TextureRegion openRegion;

    public Reconstructor(String name){
        super(name);
        update = true;
        solidifes = true;
        hasPower = true;
        configurable = true;
        consumes.powerBuffered(30f);
    }

    protected static boolean checkValidTap(Tile tile, ReconstructorEntity entity, Player player){
        return validLink(tile, entity.link) &&
                Math.abs(player.x - tile.drawx()) <= tile.block().size * tilesize / 2f &&
                Math.abs(player.y - tile.drawy()) <= tile.block().size * tilesize / 2f &&
                entity.current == null && entity.power.satisfaction >= ((Reconstructor) tile.block()).powerPerTeleport;
    }

    protected static boolean validLink(Tile tile, int position){
        Tile other = world.tile(position);
        return other != tile && other != null && other.block() instanceof Reconstructor;
    }

    protected static void unlink(ReconstructorEntity entity){
        Tile other = world.tile(entity.link);

        if(other != null && other.block() instanceof Reconstructor){
            ReconstructorEntity oe = other.entity();
            if(oe.link == entity.tile.packedPosition()){
                oe.link = -1;
            }
        }

        entity.link = -1;
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void reconstructPlayer(Player player, Tile tile){
        ReconstructorEntity entity = tile.entity();

        if(!checkValidTap(tile, entity, player) || entity.power.satisfaction < ((Reconstructor) tile.block()).powerPerTeleport)
            return;

        entity.departing = true;
        entity.current = player;
        entity.solid = false;
        entity.power.satisfaction -= Math.min(entity.power.satisfaction, ((Reconstructor) tile.block()).powerPerTeleport);
        entity.updateTime = 1f;
        entity.set(tile.drawx(), tile.drawy());
        player.rotation = 90f;
        player.baseRotation = 90f;
        player.setDead(true);
        // player.setRespawning(true);
        //player.setRespawning();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void linkReconstructor(Player player, Tile tile, Tile other){
        //just in case the client has invalid data
        if(!(tile.entity instanceof ReconstructorEntity) || !(other.entity instanceof ReconstructorEntity)) return;

        ReconstructorEntity entity = tile.entity();
        ReconstructorEntity oe = other.entity();

        //called in main thread to prevent issues
        threads.run(() -> {
            unlink(entity);
            unlink(oe);

            entity.link = other.packedPosition();
            oe.link = tile.packedPosition();
        });
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unlinkReconstructor(Player player, Tile tile, Tile other){
        //just in case the client has invalid data
        if(!(tile.entity instanceof ReconstructorEntity) || !(other.entity instanceof ReconstructorEntity)) return;

        ReconstructorEntity entity = tile.entity();
        ReconstructorEntity oe = other.entity();

        //called in main thread to prevent issues
        threads.run(() -> {
            unlink(entity);
            unlink(oe);
        });
    }

    @Override
    public void load(){
        super.load();
        openRegion = Draw.region(name + "-open");
    }

    @Override
    public boolean isSolidFor(Tile tile){
        ReconstructorEntity entity = tile.entity();

        return entity.solid;
    }

    @Override
    public void drawConfigure(Tile tile){
        super.drawConfigure(tile);

        ReconstructorEntity entity = tile.entity();

        if(validLink(tile, entity.link)){
            Tile target = world.tile(entity.link);

            Draw.color(Palette.place);
            Lines.square(target.drawx(), target.drawy(),
                    target.block().size * tilesize / 2f + 1f);
            Draw.reset();
        }

        Draw.color(Palette.accent);
        Draw.color();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        if(tile == other) return false;

        ReconstructorEntity entity = tile.entity();

        if(entity.link == other.packedPosition()){
            Call.unlinkReconstructor(null, tile, other);
            return false;
        }else if(other.block() instanceof Reconstructor){
            Call.linkReconstructor(null, tile, other);
            return false;
        }

        return true;
    }

    @Override
    public boolean shouldShowConfigure(Tile tile, Player player){
        ReconstructorEntity entity = tile.entity();
        return !checkValidTap(tile, entity, player);
    }

    @Override
    public boolean shouldHideConfigure(Tile tile, Player player){
        ReconstructorEntity entity = tile.entity();
        return checkValidTap(tile, entity, player);
    }

    @Override
    public void draw(Tile tile){
        ReconstructorEntity entity = tile.entity();

        if(entity.solid){
            Draw.rect(region, tile.drawx(), tile.drawy());
        }else{
            Draw.rect(openRegion, tile.drawx(), tile.drawy());
        }

        if(entity.current != null){
            float progress = entity.departing ? entity.updateTime : (1f - entity.updateTime);

            //Player player = entity.current;

            TextureRegion region = entity.current.getIconRegion();

            Shaders.build.region = region;
            Shaders.build.progress = progress;
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
                    size * Vars.tilesize / 2f);

            Draw.reset();
        }
    }

    @Override
    public void update(Tile tile){
        ReconstructorEntity entity = tile.entity();

        boolean stayOpen = false;

        if(entity.current != null){
            entity.time += Timers.delta();

            entity.solid = true;

            if(entity.departing){
                //force respawn if there's suddenly nothing to link to
                if(!validLink(tile, entity.link)){
                    //entity.current.setRespawning(false);
                    return;
                }

                ReconstructorEntity other = world.tile(entity.link).entity();

                entity.updateTime -= Timers.delta() / departTime;
                if(entity.updateTime <= 0f){
                    //no power? death.
                    if(other.power.satisfaction < powerPerTeleport){
                        entity.current.setDead(true);
                        //entity.current.setRespawning(false);
                        entity.current = null;
                        return;
                    }
                    other.power.satisfaction -= Math.min(other.power.satisfaction, powerPerTeleport);
                    other.current = entity.current;
                    other.departing = false;
                    other.current.set(other.x, other.y);
                    other.updateTime = 1f;
                    entity.current = null;
                }
            }else{ //else, arriving
                entity.updateTime -= Timers.delta() / arriveTime;

                if(entity.updateTime <= 0f){
                    entity.solid = false;
                    entity.current.setDead(false);

                    Effects.effect(arriveEffect, entity.current);

                    entity.current = null;
                }
            }

        }else{

            if(validLink(tile, entity.link)){
                Tile other = world.tile(entity.link);
                if(other.entity.power.satisfaction >= powerPerTeleport && Units.anyEntities(tile, 4f, unit -> unit.getTeam() == entity.getTeam() && unit instanceof Player) &&
                        entity.power.satisfaction >= powerPerTeleport){
                    entity.solid = false;
                    stayOpen = true;
                }
            }

            if(!stayOpen && !entity.solid && !Units.anyEntities(tile)){
                entity.solid = true;
            }
        }
    }

    @Override
    public void tapped(Tile tile, Player player){
        ReconstructorEntity entity = tile.entity();

        if(!checkValidTap(tile, entity, player)) return;

        Call.reconstructPlayer(player, tile);
    }

    @Override
    public void transformLinks(Tile tile, int oldWidth, int oldHeight, int newWidth, int newHeight, int shiftX, int shiftY){
        super.transformLinks(tile, oldWidth, oldHeight, newWidth, newHeight, shiftX, shiftY);

        ReconstructorEntity entity = tile.entity();
        entity.link = world.transform(entity.link, oldWidth, oldHeight, newWidth, shiftX, shiftY);
    }

    @Override
    public TileEntity newEntity(){
        return new ReconstructorEntity();
    }

    public class ReconstructorEntity extends TileEntity implements SpawnerTrait{
        Unit current;
        float updateTime;
        float time;
        int link;
        boolean solid = true, departing;

        @Override
        public void updateSpawning(Unit unit){

        }

        @Override
        public float getSpawnProgress(){
            return 0;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeInt(link);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            link = stream.readInt();
        }
    }
}
