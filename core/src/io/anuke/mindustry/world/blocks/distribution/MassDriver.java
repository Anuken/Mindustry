package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.Pool.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class MassDriver extends Block{
    protected float range;
    protected float rotateSpeed = 0.04f;
    protected float translation = 7f;
    protected int minDistribute = 10;
    protected float knockback = 4f;
    protected float reloadTime = 100f;
    protected Effect shootEffect = Fx.shootBig2;
    protected Effect smokeEffect = Fx.shootBigSmoke2;
    protected Effect recieveEffect = Fx.mineBig;
    protected float shake = 3f;
    protected TextureRegion baseRegion;

    public MassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        layer = Layer.turret;
        hasPower = true;
        outlineIcon = true;
    }

    /*
    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void linkMassDriver(Player player, Tile tile, int position){
        if(!Units.canInteract(player, tile)) return;
        MassDriverEntity entity = tile.entity();
        entity.link = position;
    }*/

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<MassDriverEntity>entity().link = value;
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-base"), Core.atlas.find(name)};
    }

    @Override
    public void load(){
        super.load();

        baseRegion = Core.atlas.find(name + "-base");
    }

    @Override
    public void update(Tile tile){
        MassDriverEntity entity = tile.entity();
        Tile link = world.tile(entity.link);
        boolean hasLink = linkValid(tile);

        //reload regardless of state
        if(entity.reload > 0f){
            entity.reload = Mathf.clamp(entity.reload - entity.delta() / reloadTime * entity.power.satisfaction);
        }

        //cleanup waiting shooters that are not valid
        if(!shooterValid(tile, entity.currentShooter())){
            entity.waitingShooters.remove(entity.currentShooter());
        }

        //switch states
        if(entity.state == DriverState.idle){
            //start accepting when idle and there's space
            if(!entity.waitingShooters.isEmpty() && (itemCapacity - entity.items.total() >= minDistribute)){
                entity.state = DriverState.accepting;
            }else if(hasLink){ //switch to shooting if there's a valid link.
                entity.state = DriverState.shooting;
            }
        }

        //dump when idle or accepting
        if(entity.state == DriverState.idle || entity.state == DriverState.accepting){
            tryDump(tile);
        }

        //skip when there's no power
        if(!entity.cons.valid()){
            return;
        }

        if(entity.state == DriverState.accepting){
            //if there's nothing shooting at this, bail - OR, items full
            if(entity.currentShooter() == null || (itemCapacity - entity.items.total() < minDistribute)){
                entity.state = DriverState.idle;
                return;
            }

            //align to shooter rotation
            entity.rotation = Mathf.slerpDelta(entity.rotation, tile.angleTo(entity.currentShooter()), rotateSpeed * entity.power.satisfaction);
        }else if(entity.state == DriverState.shooting){
            //if there's nothing to shoot at OR someone wants to shoot at this thing, bail
            if(!hasLink || (!entity.waitingShooters.isEmpty() && (itemCapacity - entity.items.total() >= minDistribute))){
                entity.state = DriverState.idle;
                return;
            }

            float targetRotation = tile.angleTo(link);

            if(
                tile.entity.items.total() >= minDistribute && //must shoot minimum amount of items
                link.block().itemCapacity - link.entity.items.total() >= minDistribute //must have minimum amount of space
            ){
                MassDriverEntity other = link.entity();
                other.waitingShooters.add(tile);

                if(entity.reload <= 0.0001f){

                    //align to target location
                    entity.rotation = Mathf.slerpDelta(entity.rotation, targetRotation, rotateSpeed * entity.power.satisfaction);

                    //fire when it's the first in the queue and angles are ready.
                    if(other.currentShooter() == tile &&
                    other.state == DriverState.accepting &&
                    Angles.near(entity.rotation, targetRotation, 2f) && Angles.near(other.rotation, targetRotation + 180f, 2f)){
                        //actually fire
                        fire(tile, link);
                        //remove waiting shooters, it's done firing
                        other.waitingShooters.remove(tile);
                        //set both states to idle
                        entity.state = DriverState.idle;
                        other.state = DriverState.idle;
                    }
                }
            }
        }
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public void drawLayer(Tile tile){
        MassDriverEntity entity = tile.entity();

        Draw.rect(region,
        tile.drawx() + Angles.trnsx(entity.rotation + 180f, entity.reload * knockback),
        tile.drawy() + Angles.trnsy(entity.rotation + 180f, entity.reload * knockback), entity.rotation - 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize, y*tilesize, range, Pal.accent);
    }

    @Override
    public void drawConfigure(Tile tile){
        float sin = Mathf.absin(Time.time(), 6f, 1f);

        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Drawf.circles(tile.drawx(), tile.drawy(), (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

        MassDriverEntity entity = tile.entity();

        if(linkValid(tile)){
            Tile target = world.tile(entity.link);
            Drawf.circles(target.drawx(), target.drawy(), (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
            Drawf.arrow(tile.drawx(), tile.drawy(), target.drawx(), target.drawy(), size * tilesize + sin, 4f + sin);
        }

        Drawf.dashCircle(tile.drawx(), tile.drawy(), range, Pal.accent);
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        if(tile == other) return false;

        MassDriverEntity entity = tile.entity();

        if(entity.link == other.pos()){
            tile.configure(-1);
            return false;
        }else if(other.block() instanceof MassDriver && other.dst(tile) <= range && other.getTeam() == tile.getTeam()){
            tile.configure(other.pos());
            return false;
        }

        return true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        //mass drivers that ouput only cannot accept items
        return tile.entity.items.total() < itemCapacity && linkValid(tile);
    }

    @Override
    public TileEntity newEntity(){
        return new MassDriverEntity();
    }

    protected void fire(Tile tile, Tile target){
        MassDriverEntity entity = tile.entity();
        MassDriverEntity other = target.entity();

        //reset reload, use power.
        entity.reload = 1f;

        DriverBulletData data = Pools.obtain(DriverBulletData.class, DriverBulletData::new);
        data.from = entity;
        data.to = other;
        int totalUsed = 0;
        for(int i = 0; i < content.items().size; i++){
            int maxTransfer = Math.min(entity.items.get(content.item(i)), ((MassDriver)tile.block()).itemCapacity - totalUsed);
            data.items[i] = maxTransfer;
            totalUsed += maxTransfer;
        }
        entity.items.clear();

        float angle = tile.angleTo(target);

        Bullet.create(Bullets.driverBolt, entity, entity.getTeam(),
        tile.drawx() + Angles.trnsx(angle, translation), tile.drawy() + Angles.trnsy(angle, translation),
        angle, 1f, 1f, data);

        Effects.effect(shootEffect, tile.drawx() + Angles.trnsx(angle, translation),
        tile.drawy() + Angles.trnsy(angle, translation), angle);

        Effects.effect(smokeEffect, tile.drawx() + Angles.trnsx(angle, translation),
        tile.drawy() + Angles.trnsy(angle, translation), angle);

        Effects.shake(shake, shake, entity);
    }

    protected void handlePayload(MassDriverEntity entity, Bullet bullet, DriverBulletData data){
        int totalItems = entity.items.total();

        //add all the items possible
        for(int i = 0; i < data.items.length; i++){
            int maxAdd = Math.min(data.items[i], itemCapacity * 2 - totalItems);
            entity.items.add(content.item(i), maxAdd);
            data.items[i] -= maxAdd;
            totalItems += maxAdd;

            if(totalItems >= itemCapacity * 2){
                break;
            }
        }

        Effects.shake(shake, shake, entity);
        Effects.effect(recieveEffect, bullet);

        entity.reload = 1f;
        bullet.remove();
    }

    protected boolean shooterValid(Tile tile, Tile other){
        if(other == null) return true;
        if(!(other.block() instanceof MassDriver)) return false;
        MassDriverEntity entity = other.entity();
        return entity.link == tile.pos() && tile.dst(other) <= range;
    }

    protected boolean linkValid(Tile tile){
        if(tile == null) return false;
        MassDriverEntity entity = tile.entity();
        if(entity == null || entity.link == -1) return false;
        Tile link = world.tile(entity.link);

        return link != null && link.block() instanceof MassDriver && tile.dst(link) <= range;
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
        int link = -1;
        float rotation = 90;
        float reload = 0f;
        DriverState state = DriverState.idle;
        OrderedSet<Tile> waitingShooters = new OrderedSet<>();

        Tile currentShooter(){
            return waitingShooters.isEmpty() ? null : waitingShooters.first();
        }

        public void handlePayload(Bullet bullet, DriverBulletData data){
            ((MassDriver)block).handlePayload(this, bullet, data);
        }

        @Override
        public int config(){
            return link;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(link);
            stream.writeFloat(rotation);
            stream.writeByte((byte)state.ordinal());
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            link = stream.readInt();
            rotation = stream.readFloat();
            state = DriverState.values()[stream.readByte()];
        }
    }

    enum DriverState{
        idle, //nothing is shooting at this mass driver and it does not have any target
        accepting, //currently getting shot at, unload items
        shooting,
        unloading
    }
}
