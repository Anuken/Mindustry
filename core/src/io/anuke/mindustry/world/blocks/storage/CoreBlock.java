package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.maps.TutorialSector;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected float droneRespawnDuration = 60 * 6;
    protected UnitType droneType = UnitTypes.spirit;

    protected TextureRegion openRegion;
    protected TextureRegion topRegion;

    public CoreBlock(String name){
        super(name);

        solid = false;
        solidifes = true;
        update = true;
        size = 3;
        hasItems = true;
        itemCapacity = 2000;
        viewRange = 200f;
        flags = EnumSet.of(BlockFlag.resupplyPoint, BlockFlag.target);
    }

    @Remote(called = Loc.server)
    public static void onUnitRespawn(Tile tile, Unit player){
        if(player == null || tile.entity == null) return;

        CoreEntity entity = tile.entity();
        Effects.effect(Fx.spawn, entity);
        entity.solid = false;
        entity.progress = 0;
        entity.currentUnit = player;
        entity.currentUnit.heal();
        entity.currentUnit.rotation = 90f;
        entity.currentUnit.setNet(tile.drawx(), tile.drawy());
        entity.currentUnit.add();
        entity.currentUnit = null;

        if(player instanceof Player){
            ((Player) player).endRespawning();
        }
    }

    @Remote(called = Loc.server)
    public static void setCoreSolid(Tile tile, boolean solid){
        if(tile == null) return;
        CoreEntity entity = tile.entity();
        if(entity != null) entity.solid = solid;
    }

    @Override
    public void onProximityUpdate(Tile tile) {
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public boolean canBreak(Tile tile){
        return state.teams.get(tile.getTeam()).cores.size > 1;
    }

    @Override
    public void removed(Tile tile){
        state.teams.get(tile.getTeam()).cores.remove(tile);
    }

    @Override
    public void placed(Tile tile){
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove(BarType.inventory);
    }

    @Override
    public void load(){
        super.load();

        openRegion = Draw.region(name + "-open");
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        CoreEntity entity = tile.entity();

        Draw.rect(entity.solid ? Draw.region(name) : openRegion, tile.drawx(), tile.drawy());

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
    public boolean isSolidFor(Tile tile){
        CoreEntity entity = tile.entity();

        return entity.solid;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()) super.handleItem(item, tile, source);
    }

    @Override
    public void update(Tile tile){
        CoreEntity entity = tile.entity();

        if(!entity.solid && !Units.anyEntities(tile)){
            Call.setCoreSolid(tile, true);
        }

        if(entity.currentUnit != null){
            if(!entity.currentUnit.isDead()){
                entity.currentUnit = null;
                return;
            }
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += entity.delta();
            entity.progress += 1f / (entity.currentUnit instanceof Player ? state.mode.respawnTime : droneRespawnDuration) * entity.delta();

            if(entity.progress >= 1f){
                Call.onUnitRespawn(tile, entity.currentUnit);
            }
        }else if(!netServer.isWaitingForPlayers()){
            entity.warmup += Timers.delta();

            if(entity.solid && entity.warmup > 60f && unitGroups[tile.getTeamID()].getByID(entity.droneID) == null && !Net.client()){

                boolean found = false;
                for(BaseUnit unit : unitGroups[tile.getTeamID()].all()){
                    if(unit.getType().id == droneType.id){
                        entity.droneID = unit.id;
                        found = true;
                        break;
                    }
                }

                if(!found && !TutorialSector.supressDrone()){
                    BaseUnit unit = droneType.create(tile.getTeam());
                    unit.setSpawner(tile);
                    unit.setDead(true);
                    unit.add();

                    useContent(tile, droneType);

                    entity.droneID = unit.id;
                }
            }

            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new CoreEntity();
    }

    public class CoreEntity extends StorageEntity implements SpawnerTrait{
        public Unit currentUnit;
        int droneID = -1;
        boolean solid = true;
        float warmup;
        float progress;
        float time;
        float heat;

        @Override
        public void updateSpawning(Unit unit){
            if(!netServer.isWaitingForPlayers() && currentUnit == null){
                currentUnit = unit;
                progress = 0f;
                unit.set(tile.drawx(), tile.drawy());
            }
        }

        @Override
        public float getSpawnProgress(){
            return progress;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeBoolean(solid);
            stream.writeInt(droneID);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            solid = stream.readBoolean();
            droneID = stream.readInt();
        }
    }
}
