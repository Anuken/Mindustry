package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected float droneRespawnDuration = 60 * 6;
    protected UnitType droneType = UnitTypes.drone;

    protected TextureRegion openRegion;
    protected TextureRegion topRegion;

    public CoreBlock(String name){
        super(name);

        solid = false;
        solidifes = true;
        update = true;
        unbreakable = true;
        size = 3;
        hasItems = true;
        itemCapacity = 2000;
        viewRange = 200f;
        flags = EnumSet.of(BlockFlag.resupplyPoint, BlockFlag.target);
    }

    @Remote(called = Loc.server)
    public static void onUnitRespawn(Tile tile, Unit player){
        if(player == null) return;

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
    }

    @Remote(called = Loc.server)
    public static void setCoreSolid(Tile tile, boolean solid){
        CoreEntity entity = tile.entity();
        entity.solid = solid;
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
    public float handleDamage(Tile tile, float amount){
        return debug ? 0 : amount;
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
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && source.getTeam() == tile.getTeam()){
            return Math.min(itemCapacity - tile.entity.items.get(item), amount);
        }else{
            return 0;
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.get(item) < itemCapacity && item.type == ItemType.material;
    }

    @Override
    public void onDestroyed(Tile tile){
        //TODO more dramatic effects
        super.onDestroyed(tile);

        state.teams.get(tile.getTeam()).cores.removeValue(tile, true);
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

        EntityPhysics.getNearby(bulletGroup, tile.drawx(), tile.drawy(), state.mode.enemyCoreShieldRadius*2f, e -> {
            if(e.distanceTo(tile) > state.mode.enemyCoreShieldRadius) return;
            Bullet bullet = (Bullet)e;
            if(bullet.getOwner() instanceof Player && bullet.getTeam() != tile.getTeam()){
                Effects.effect(BulletFx.absorb, bullet);
                entity.shieldHeat = 1f;
                bullet.supressCollision();
                bullet.remove();
            }
        });

        if(entity.currentUnit != null){
            if(!entity.currentUnit.isDead()){
                entity.currentUnit = null;
                return;
            }
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += Timers.delta();
            entity.progress += 1f / (entity.currentUnit instanceof Player ? state.mode.respawnTime : droneRespawnDuration) * Timers.delta();

            //instant build for fast testing.
            if(debug){
                entity.progress = 1f;
            }

            if(entity.progress >= 1f){
                Call.onUnitRespawn(tile, entity.currentUnit);
            }
        }else{
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

                if(!found){
                    BaseUnit unit = droneType.create(tile.getTeam());
                    unit.setSpawner(tile);
                    unit.setDead(true);
                    unit.add();

                    useContent(droneType);

                    entity.droneID = unit.id;
                }
            }

            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public TileEntity getEntity(){
        return new CoreEntity();
    }

    public class CoreEntity extends TileEntity implements SpawnerTrait{
        public Unit currentUnit;
        public float shieldHeat;
        int droneID = -1;
        boolean solid = true;
        float warmup;
        float progress;
        float time;
        float heat;

        @Override
        public void updateSpawning(Unit unit){
            if(currentUnit == null){
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
        public void write(DataOutputStream stream) throws IOException{
            stream.writeBoolean(solid);
            stream.writeInt(droneID);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            solid = stream.readBoolean();
            droneID = stream.readInt();
        }
    }
}
