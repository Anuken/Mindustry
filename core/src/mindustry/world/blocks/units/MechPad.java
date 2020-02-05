package mindustry.world.blocks.units;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
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
    public @NonNull UnitDef mech;
    public float buildTime = 60 * 5;

    public MechPad(String name){
        super(name);
        update = true;
        solid = false;
        hasPower = true;
        layer = Layer.overlay;
        flags = EnumSet.of(BlockFlag.mechPad);
        entityType = MechFactoryEntity::new;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.productionTime, buildTime / 60f, StatUnit.seconds);
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void onMechFactoryTap(Playerc player, Tile tile){
        if(player == null || tile == null || !(tile.block() instanceof MechPad) || !checkValidTap(tile, player)) return;

        MechFactoryEntity entity = tile.ent();

        if(!entity.consValid()) return;
        //player.beginRespawning(entity);
        entity.sameMech = false;
    }

    @Remote(called = Loc.server)
    public static void onMechFactoryDone(Tile tile){
        if(!(tile.entity instanceof MechFactoryEntity)) return;

        MechFactoryEntity entity = tile.ent();

        Fx.spawn.at(entity);

        if(entity.player == null) return;
        //Mech mech = ((MechPad)tile.block()).mech;
        //boolean resetSpawner = !entity.sameMech && entity.player.mech == mech;
        //entity.player.mech = !entity.sameMech && entity.player.mech == mech ? UnitTypes.starter : mech;

        Playerc player = entity.player;

        //entity.progress = 0;
        //entity.player.onRespawn(tile);
        //if(resetSpawner) entity.player.lastSpawner = null;
        //entity.player = null;

        //Events.fire(new MechChangeEvent(player, player.mech));
    }

    protected static boolean checkValidTap(Tile tile, Playerc player){
        MechFactoryEntity entity = tile.ent();
        return false;//!player.dead() && tile.interactable(player.team()) && Math.abs(player.x - tile.drawx()) <= tile.block().size * tilesize &&
        //Math.abs(player.y - tile.drawy()) <= tile.block().size * tilesize && entity.consValid() && entity.player == null;
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
    public void tapped(Tile tile, Playerc player){
        MechFactoryEntity entity = tile.ent();

        if(checkValidTap(tile, player)){
            Call.onMechFactoryTap(player, tile);
        }else if(player.isLocal() && mobile && !player.dead() && entity.consValid() && entity.player == null){
            //deselect on double taps
            //TODO remove
            //player.moveTarget = player.moveTarget == tile.entity ? null : tile.entity;
        }
    }

    @Override
    public void drawLayer(Tile tile){
        MechFactoryEntity entity = tile.ent();

        if(entity.player != null){
            //TODO remove
            //RespawnBlock.drawRespawn(tile, entity.heat, entity.progress, entity.time, entity.player, (!entity.sameMech && entity.player.mech == mech ? UnitTypes.starter : mech));
        }
    }

    @Override
    public void update(Tile tile){
        MechFactoryEntity entity = tile.ent();

        if(entity.player != null){
            entity.player.set(tile.drawx(), tile.drawy());
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

    public class MechFactoryEntity extends TileEntity{
        Playerc player;
        boolean sameMech;
        float progress;
        float time;
        float heat;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(progress);
            stream.writeFloat(time);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            super.read(stream);
            progress = stream.readFloat();
            time = stream.readFloat();
            heat = stream.readFloat();
        }
    }
}
