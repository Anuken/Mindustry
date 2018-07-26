package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class MassDriver extends Block{
    protected float range;
    protected float rotateSpeed = 0.04f;
    protected float translation = 7f;
    protected int minDistribute = 10;
    protected float knockback = 4f;
    protected float reloadTime = 80f;
    protected Effect shootEffect = ShootFx.shootBig2;
    protected Effect smokeEffect = ShootFx.shootBigSmoke2;
    protected Effect recieveEffect = BlockFx.smeltsmoke;
    protected float shake = 3f;

    public MassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        itemCapacity = 50;
        layer = Layer.turret;
        hasPower = true;
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

        DriverBulletData data = Pooling.obtain(DriverBulletData.class);
        data.from = entity;
        data.to = other;
        for(int i = 0; i < Item.all().size; i++){
            data.items[i] = entity.items.get(Item.getByID(i));
        }
        entity.items.clear();

        float angle = tile.angleTo(target);

        other.isRecieving = true;
        Bullet.create(TurretBullets.driverBolt, entity, entity.getTeam(),
                tile.drawx() + Angles.trnsx(angle, driver.translation), tile.drawy() + Angles.trnsy(angle, driver.translation),
                angle, 1f, data);

        Effects.effect(driver.shootEffect, tile.drawx() + Angles.trnsx(angle, driver.translation),
                tile.drawy() + Angles.trnsy(angle, driver.translation), angle);

        Effects.effect(driver.smokeEffect, tile.drawx() + Angles.trnsx(angle, driver.translation),
                tile.drawy() + Angles.trnsy(angle, driver.translation), angle);

        Effects.shake(driver.shake, driver.shake, entity);
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
            entity.reload = Mathf.clamp(entity.reload - Timers.delta() / reloadTime);
        }

        if(!entity.isRecieving){

            if(entity.waiting.size > 0){ //accepting takes priority over shooting
                Tile waiter = entity.waiting.first();

                entity.rotation = Mathf.slerpDelta(entity.rotation, tile.angleTo(waiter), rotateSpeed);
            }else if(tile.entity.items.total() >= minDistribute &&
                    linkValid(tile) && //only fire when at least at half-capacity and power
                    tile.entity.power.amount >= powerCapacity &&
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

        Draw.rect(name + "-turret",
                tile.drawx() + Angles.trnsx(entity.rotation + 180f, entity.reload * knockback),
                tile.drawy() + Angles.trnsy(entity.rotation + 180f, entity.reload * knockback),
                entity.rotation - 90);
    }

    @Override
    public void drawConfigure(Tile tile){
        super.drawConfigure(tile);

        MassDriverEntity entity = tile.entity();

        if(linkValid(tile)){
            Tile target = world.tile(entity.link);

            Draw.color(Palette.place);
            Lines.square(target.drawx(), target.drawy(),
                    target.block().size * tilesize / 2f + 1f);
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
    public TileEntity getEntity(){
        return new MassDriverEntity();
    }

    protected boolean linkValid(Tile tile){
        MassDriverEntity entity = tile.entity();
        if(entity.link == -1) return false;
        Tile link = world.tile(entity.link);

        return link != null && link.block() instanceof MassDriver && tile.distanceTo(link) <= range;
    }

    public static class DriverBulletData implements Poolable{
        public MassDriverEntity from, to;
        public int[] items = new int[Item.all().size];

        @Override
        public void reset(){
            from = null;
            to = null;
            ;
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
        public boolean isUnloading;

        public float reload = 0f;

        public void handlePayload(Bullet bullet, DriverBulletData data){
            int totalItems = items.total();

            //add all the items possible
            for(int i = 0; i < data.items.length; i++){
                int maxAdd = Math.min(data.items[i], itemCapacity - totalItems);
                items.add(Item.getByID(i), maxAdd);
                data.items[i] -= maxAdd;
                totalItems += maxAdd;

                if(totalItems >= itemCapacity){
                    break;
                }
            }

            //drop all items remaining on the ground
            for(int i = 0; i < data.items.length; i++){
                int amountDropped = Mathf.random(0, data.items[i]);
                if(amountDropped > 0){
                    float angle = Mathf.range(180f);
                    float vs = Mathf.random(0f, 4f);
                    ItemDrop.create(Item.getByID(i), amountDropped, bullet.x, bullet.y, Angles.trnsx(angle, vs), Angles.trnsy(angle, vs));
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
        public void write(DataOutputStream stream) throws IOException{
            stream.writeInt(link);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            link = stream.readInt();
        }
    }
}
