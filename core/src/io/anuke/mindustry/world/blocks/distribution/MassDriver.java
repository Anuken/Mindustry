package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

// TODO Adapt whole class to new power system
public class MassDriver extends Block{
    protected float range;
    protected float rotateSpeed = 0.04f;
    protected float translation = 7f;
    protected int minDistribute = 10;
    protected float knockback = 4f;
    protected float reloadTime = 100f;
    protected Effect shootEffect = ShootFx.shootBig2;
    protected Effect smokeEffect = ShootFx.shootBigSmoke2;
    protected Effect recieveEffect = BlockFx.mineBig;
    protected float shake = 3f;
    protected final static float powerPercentageUsed = 0.8f;
    protected TextureRegion turretRegion;

    public MassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        layer = Layer.turret;
        hasPower = true;
        consumes.powerBuffered(30f);
        consumes.require(ConsumePower.class);
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void linkMassDriver(Player player, Tile tile, int position){
        MassDriverEntity entity = tile.entity();

        //called in main thread to prevent issues
        threads.run(() -> entity.link = position);
    }

    @Remote(called = Loc.server)
    public static void onMassDriverFire(Tile tile, Tile target){
        //just in case the client has invalid data
        if(!(tile.entity instanceof MassDriverEntity) || !(target.entity instanceof MassDriverEntity)) return;

        MassDriver driver = (MassDriver) tile.block();

        MassDriverEntity entity = tile.entity();
        MassDriverEntity other = target.entity();

        entity.reload = 1f;

        entity.power.satisfaction -= Math.min(entity.power.satisfaction, powerPercentageUsed);

        DriverBulletData data = Pooling.obtain(DriverBulletData.class, DriverBulletData::new);
        data.from = entity;
        data.to = other;
        int totalUsed = 0;
        for(int i = 0; i < content.items().size; i++){
            int maxTransfer = Math.min(entity.items.get(content.item(i)), ((MassDriver) tile.block()).itemCapacity - totalUsed);
            data.items[i] = maxTransfer;
            totalUsed += maxTransfer;
        }
        entity.items.clear();

        float angle = tile.angleTo(target);

        other.isRecieving = true;
        Bullet.create(TurretBullets.driverBolt, entity, entity.getTeam(),
                tile.drawx() + Angles.trnsx(angle, driver.translation), tile.drawy() + Angles.trnsy(angle, driver.translation),
                angle, 1f, 1f, data);

        Effects.effect(driver.shootEffect, tile.drawx() + Angles.trnsx(angle, driver.translation),
                tile.drawy() + Angles.trnsy(angle, driver.translation), angle);

        Effects.effect(driver.smokeEffect, tile.drawx() + Angles.trnsx(angle, driver.translation),
                tile.drawy() + Angles.trnsy(angle, driver.translation), angle);

        Effects.shake(driver.shake, driver.shake, entity);
    }

    @Override
    public TextureRegion[] getBlockIcon(){
        if(blockIcon == null){
            blockIcon = new TextureRegion[]{region, turretRegion};
        }
        return super.getBlockIcon();
    }

    @Override
    public void load(){
        super.load();

        turretRegion = Draw.region(name + "-turret");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerShot, consumes.get(ConsumePower.class).powerCapacity * powerPercentageUsed, StatUnit.powerUnits);
    }

    @Override
    public void init(){
        super.init();

        viewRange = range;
    }

    @Override
    public void update(Tile tile){
        MassDriverEntity entity = tile.entity();

        Tile link = world.tile(entity.link);

        if(entity.isUnloading){
            tryDump(tile);
            if(entity.items.total() <= 0){
                entity.isUnloading = false;
            }
        }

        if(entity.reload > 0f){
            entity.reload = Mathf.clamp(entity.reload - entity.delta() / reloadTime);
        }

        //unload when dest is full
        if(!linkValid(tile) || (link.entity.items.total() >= itemCapacity) && entity.items.total() > 0){
            entity.isUnloading = true;
        }

        if(!entity.isRecieving){

            if(entity.waiting.size > 0){ //accepting takes priority over shooting
                Tile waiter = entity.waiting.first();

                entity.rotation = Mathf.slerpDelta(entity.rotation, tile.angleTo(waiter), rotateSpeed);
            }else if(tile.entity.items.total() >= minDistribute &&
                    linkValid(tile) && //only fire when at least at 80% power capacity
                    tile.entity.power.satisfaction > powerPercentageUsed &&
                    link.block().itemCapacity - link.entity.items.total() >= minDistribute && entity.reload <= 0.0001f){

                MassDriverEntity other = link.entity();
                other.waiting.add(tile);

                float target = tile.angleTo(link);

                entity.rotation = Mathf.slerpDelta(entity.rotation, target, rotateSpeed);

                if(Mathf.angNear(entity.rotation, target, 1f) &&
                        Mathf.angNear(other.rotation, target + 180f, 1f)){
                    Call.onMassDriverFire(tile, link);
                }
            }
        }

        entity.waiting.clear();
    }

    @Override
    public void drawLayer(Tile tile){
        MassDriverEntity entity = tile.entity();

        Draw.rect(turretRegion,
                tile.drawx() + Angles.trnsx(entity.rotation + 180f, entity.reload * knockback),
                tile.drawy() + Angles.trnsy(entity.rotation + 180f, entity.reload * knockback),
                entity.rotation - 90);
    }

    @Override
    public void drawConfigure(Tile tile){
        float sin = Mathf.absin(Timers.time(), 6f, 1f);

        Draw.color(Palette.accent);
        Lines.stroke(1f);
        Lines.poly(tile.drawx(), tile.drawy(), 20, (tile.block().size/2f+1) * tilesize + sin);

        MassDriverEntity entity = tile.entity();

        if(linkValid(tile)){
            Tile target = world.tile(entity.link);

            Draw.color(Palette.place);
            Lines.poly(target.drawx(), target.drawy(), 20, (target.block().size/2f+1) * tilesize + sin);
            Draw.reset();
        }

        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), range);
        Draw.color();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        if(tile == other) return false;

        MassDriverEntity entity = tile.entity();

        if(entity.link == other.packedPosition()){
            Call.linkMassDriver(null, tile, -1);
            return false;
        }else if(other.block() instanceof MassDriver && other.distanceTo(tile) <= range){
            Call.linkMassDriver(null, tile, other.packedPosition());
            return false;
        }

        return true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public void transformLinks(Tile tile, int oldWidth, int oldHeight, int newWidth, int newHeight, int shiftX, int shiftY){
        super.transformLinks(tile, oldWidth, oldHeight, newWidth, newHeight, shiftX, shiftY);

        MassDriverEntity entity = tile.entity();
        entity.link = world.transform(entity.link, oldWidth, oldHeight, newWidth, shiftX, shiftY);
    }

    @Override
    public TileEntity newEntity(){
        return new MassDriverEntity();
    }

    protected boolean linkValid(Tile tile){
        MassDriverEntity entity = tile.entity();
        if(entity == null || entity.link == -1) return false;
        Tile link = world.tile(entity.link);

        return link != null && link.block() instanceof MassDriver && tile.distanceTo(link) <= range;
    }

    public static class DriverBulletData implements Poolable{
        public MassDriverEntity from, to;
        public int[] items = new int[content.items().size];

        @Override
        public void reset(){
            from = null;
            to = null;
        }
    }

    public class MassDriverEntity extends TileEntity{
        public int link = -1;
        public float rotation = 90;
        //set of tiles that currently want to distribute to this tile
        public ObjectSet<Tile> waiting = new ObjectSet<>();
        //whether this mass driver is waiting for a bullet to hit it and deliver items
        public boolean isRecieving;
        //whether this driver just recieved some items and is now unloading
        public boolean isUnloading = true;

        public float reload = 0f;

        public void handlePayload(Bullet bullet, DriverBulletData data){
            int totalItems = items.total();

            //add all the items possible
            for(int i = 0; i < data.items.length; i++){
                int maxAdd = Math.min(data.items[i], itemCapacity*2 - totalItems);
                items.add(content.item(i), maxAdd);
                data.items[i] -= maxAdd;
                totalItems += maxAdd;

                if(totalItems >= itemCapacity*2){
                    break;
                }
            }

            //drop all items remaining on the ground
            for(int i = 0; i < data.items.length; i++){
                int amountDropped = Mathf.random(0, data.items[i]);
                if(amountDropped > 0){
                    float angle = Mathf.range(180f);
                    Effects.effect(EnvironmentFx.dropItem, Color.WHITE, bullet.x, bullet.y, angle, content.item(i));
                }
            }

            reload = 1f;
            Effects.shake(shake, shake, this);
            Effects.effect(recieveEffect, bullet);

            isRecieving = false;
            bullet.remove();

            if(!linkValid(tile)){
                isUnloading = true;
            }
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeInt(link);
            stream.writeFloat(rotation);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            link = stream.readInt();
            rotation = stream.readFloat();
        }
    }
}
