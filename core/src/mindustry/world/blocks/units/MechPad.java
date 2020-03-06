package mindustry.world.blocks.units;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

//TODO remove
public class MechPad extends Block{
    public @NonNull UnitType mech;
    public float buildTime = 60 * 5;

    public MechPad(String name){
        super(name);
        update = true;
        solid = false;
        hasPower = true;
        layer = Layer.overlay;
        flags = EnumSet.of(BlockFlag.mechPad);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.productionTime, buildTime / 60f, StatUnit.seconds);
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void onMechFactoryTap(Playerc player, Tile tile){
        if(player == null || tile == null || !(tile.block() instanceof MechPad) || !checkValidTap(tile, player)) return;

        if(!consValid()) return;
        //player.beginRespawning(entity);
        sameMech = false;
    }

    @Remote(called = Loc.server)
    public static void onMechFactoryDone(){
        if(!(tile.entity instanceof MechFactoryEntity)) return;

        Fx.spawn.at(entity);

        if(player == null) return;
        //Mech mech = ((MechPad)tile.block()).mech;
        //boolean resetSpawner = !sameMech && player.mech == mech;
        //player.mech = !sameMech && player.mech == mech ? UnitTypes.starter : mech;

        Playerc player = player;

        //progress = 0;
        //player.onRespawn(tile);
        //if(resetSpawner) player.lastSpawner = null;
        //player = null;

        //Events.fire(new MechChangeEvent(player, player.mech));
    }

    protected static boolean checkValidTap(Playerc player){
        return false;//!player.dead() && tile.interactable(player.team()) && Math.abs(player.x - x) <= tile.block().size * tilesize &&
        //Math.abs(player.y - y) <= tile.block().size * tilesize && consValid() && player == null;
    }

    @Override
    public void drawSelect(){
        Draw.color(Pal.accent);
        for(int i = 0; i < 4; i++){
            float length = tilesize * size / 2f + 3 + Mathf.absin(Time.time(), 5f, 2f);
            Draw.rect("transfer-arrow", x + Geometry.d4[i].x * length, y + Geometry.d4[i].y * length, (i + 2) * 90);
        }
        Draw.color();
    }

    @Override
    public void tapped(Playerc player){
        if(checkValidTap(tile, player)){
            Call.onMechFactoryTap(player, tile);
        }else if(player.isLocal() && mobile && !player.dead() && consValid() && player == null){
            //deselect on double taps
            //TODO remove
            //player.moveTarget = player.moveTarget == tile.entity ? null : tile.entity;
        }
    }

    @Override
    public void drawLayer(){
        if(player != null){
            //TODO remove
            //RespawnBlock.drawRespawn(tile, heat, progress, time, player, (!sameMech && player.mech == mech ? UnitTypes.starter : mech));
        }
    }

    @Override
    public void updateTile(){
        if(player != null){
            player.set(x, y);
            heat = Mathf.lerpDelta(heat, 1f, 0.1f);
            progress += 1f / buildTime * delta();

            time += 0.5f * delta();

            if(progress >= 1f){
                Call.onMechFactoryDone(tile);
            }
        }else{
            heat = Mathf.lerpDelta(heat, 0f, 0.1f);
        }
    }

    public class MechFactoryEntity extends TileEntity{
        Playerc player;
        boolean sameMech;
        float progress;
        float time;
        float heat;

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(time);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            time = read.f();
            heat = read.f();
        }
    }
}
