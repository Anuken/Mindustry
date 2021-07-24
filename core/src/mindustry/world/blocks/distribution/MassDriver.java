package mindustry.world.blocks.distribution;

import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MassDriver extends Block{
    public float range;
    public float rotateSpeed = 5f;
    public float translation = 7f;
    public int minDistribute = 10;
    public float knockback = 4f;
    public float reloadTime = 100f;
    public MassDriverBolt bullet = new MassDriverBolt();
    public float bulletSpeed = 5.5f;
    public float bulletLifetime = 200f;
    public Effect shootEffect = Fx.shootBig2;
    public Effect smokeEffect = Fx.shootBigSmoke2;
    public Effect receiveEffect = Fx.mineBig;
    public Sound shootSound = Sounds.shootBig;
    public float shake = 3f;
    public @Load("@-base") TextureRegion baseRegion;

    public MassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        hasPower = true;
        outlineIcon = true;
        sync = true;

        //point2 is relative
        config(Point2.class, (MassDriverBuild tile, Point2 point) -> tile.link = Point2.pack(point.x + tile.tileX(), point.y + tile.tileY()));
        config(Integer.class, (MassDriverBuild tile, Integer point) -> tile.link = point);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / reloadTime, StatUnit.perSecond);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize, y * tilesize, range, Pal.accent);

        //check if a mass driver is selected while placing this driver
        if(!control.input.frag.config.isShown()) return;
        Building selected = control.input.frag.config.getSelectedTile();
        if(selected == null || selected.block != this || !selected.within(x * tilesize, y * tilesize, range)) return;

        //if so, draw a dotted line towards it while it is in range
        float sin = Mathf.absin(Time.time, 6f, 1f);
        Tmp.v1.set(x * tilesize + offset, y * tilesize + offset).sub(selected.x, selected.y).limit((size / 2f + 1) * tilesize + sin + 0.5f);
        float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
            x1 = selected.x + Tmp.v1.x, y1 = selected.y + Tmp.v1.y;
        int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

        Lines.stroke(4f, Pal.gray);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Lines.stroke(2f, Pal.placing);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Draw.reset();
    }

    public static class DriverBulletData implements Poolable{
        public MassDriverBuild from, to;
        public int[] items = new int[content.items().size];

        @Override
        public void reset(){
            from = null;
            to = null;
        }
    }

    public class MassDriverBuild extends Building{
        public int link = -1;
        public float rotation = 90;
        public float reload = 0f;
        public DriverState state = DriverState.idle;
        //TODO use queue? this array usually holds about 3 shooters max anyway
        public OrderedSet<Building> waitingShooters = new OrderedSet<>();

        public Building currentShooter(){
            return waitingShooters.isEmpty() ? null : waitingShooters.first();
        }

        @Override
        public void updateTile(){
            Building link = world.build(this.link);
            boolean hasLink = linkValid();

            if(hasLink){
                this.link = link.pos();
            }

            //reload regardless of state
            if(reload > 0f){
                reload = Mathf.clamp(reload - edelta() / reloadTime);
            }

            var current = currentShooter();

            //cleanup waiting shooters that are not valid
            if(current != null && !shooterValid(current)){
                waitingShooters.remove(current);
            }

            //switch states
            if(state == DriverState.idle){
                //start accepting when idle and there's space
                if(!waitingShooters.isEmpty() && (itemCapacity - items.total() >= minDistribute)){
                    state = DriverState.accepting;
                }else if(hasLink){ //switch to shooting if there's a valid link.
                    state = DriverState.shooting;
                }
            }

            //dump when idle or accepting
            if(state == DriverState.idle || state == DriverState.accepting){
                dumpAccumulate();
            }

            //skip when there's no power
            if(!consValid()){
                return;
            }

            if(state == DriverState.accepting){
                //if there's nothing shooting at this, bail - OR, items full
                if(currentShooter() == null || (itemCapacity - items.total() < minDistribute)){
                    state = DriverState.idle;
                    return;
                }

                //align to shooter rotation
                rotation = Angles.moveToward(rotation, tile.angleTo(currentShooter()), rotateSpeed * efficiency());
            }else if(state == DriverState.shooting){
                //if there's nothing to shoot at OR someone wants to shoot at this thing, bail
                if(!hasLink || (!waitingShooters.isEmpty() && (itemCapacity - items.total() >= minDistribute))){
                    state = DriverState.idle;
                    return;
                }

                float targetRotation = tile.angleTo(link);

                if(
                items.total() >= minDistribute && //must shoot minimum amount of items
                link.block.itemCapacity - link.items.total() >= minDistribute //must have minimum amount of space
                ){
                    MassDriverBuild other = (MassDriverBuild)link;
                    other.waitingShooters.add(this);

                    if(reload <= 0.0001f){

                        //align to target location
                        rotation = Angles.moveToward(rotation, targetRotation, rotateSpeed * efficiency());

                        //fire when it's the first in the queue and angles are ready.
                        if(other.currentShooter() == this &&
                        other.state == DriverState.accepting &&
                        Angles.near(rotation, targetRotation, 2f) && Angles.near(other.rotation, targetRotation + 180f, 2f)){
                            //actually fire
                            fire(other);
                            float timeToArrive = Math.min(bulletLifetime, dst(other) / bulletSpeed);
                            Time.run(timeToArrive, () -> {
                                //remove waiting shooters, it's done firing
                                other.waitingShooters.remove(this);
                                other.state = DriverState.idle;
                            });
                            //driver is immediately idle
                            state = DriverState.idle;
                        }
                    }
                }
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(1f - reload / reloadTime);
            return super.sense(sensor);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);

            Draw.z(Layer.turret);

            Drawf.shadow(region,
            x + Angles.trnsx(rotation + 180f, reload * knockback) - (size / 2),
            y + Angles.trnsy(rotation + 180f, reload * knockback) - (size / 2), rotation - 90);
            Draw.rect(region,
            x + Angles.trnsx(rotation + 180f, reload * knockback),
            y + Angles.trnsy(rotation + 180f, reload * knockback), rotation - 90);
        }

        @Override
        public void drawConfigure(){
            float sin = Mathf.absin(Time.time, 6f, 1f);

            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Drawf.circles(x, y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

            for(var shooter : waitingShooters){
                Drawf.circles(shooter.x, shooter.y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(shooter.x, shooter.y, x, y, size * tilesize + sin, 4f + sin, Pal.place);
            }

            if(linkValid()){
                Building target = world.build(link);
                Drawf.circles(target.x, target.y, (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(x, y, target.x, target.y, size * tilesize + sin, 4f + sin);
            }

            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                configure(-1);
                return false;
            }

            if(link == other.pos()){
                configure(-1);
                return false;
            }else if(other.block == block && other.dst(tile) <= range && other.team == team){
                configure(other.pos());
                return false;
            }

            return true;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            //mass drivers that output only cannot accept items
            return items.total() < itemCapacity && linkValid();
        }

        protected void fire(MassDriverBuild target){
            //reset reload, use power.
            reload = 1f;

            DriverBulletData data = Pools.obtain(DriverBulletData.class, DriverBulletData::new);
            data.from = this;
            data.to = target;
            int totalUsed = 0;
            for(int i = 0; i < content.items().size; i++){
                int maxTransfer = Math.min(items.get(content.item(i)), tile.block().itemCapacity - totalUsed);
                data.items[i] = maxTransfer;
                totalUsed += maxTransfer;
                items.remove(content.item(i), maxTransfer);
            }

            float angle = tile.angleTo(target);

            bullet.create(this, team,
                x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation),
                angle, -1f, bulletSpeed, bulletLifetime, data);

            shootEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle);
            smokeEffect.at(x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation), angle);

            Effect.shake(shake, shake, this);
            
            shootSound.at(tile, Mathf.random(0.9f, 1.1f));
        }

        public void handlePayload(Bullet bullet, DriverBulletData data){
            int totalItems = items.total();

            //add all the items possible
            for(int i = 0; i < data.items.length; i++){
                int maxAdd = Math.min(data.items[i], itemCapacity * 2 - totalItems);
                items.add(content.item(i), maxAdd);
                data.items[i] -= maxAdd;
                totalItems += maxAdd;

                if(totalItems >= itemCapacity * 2){
                    break;
                }
            }

            Effect.shake(shake, shake, this);
            receiveEffect.at(bullet);

            reload = 1f;
            bullet.remove();
        }

        protected boolean shooterValid(Building other){
            return other instanceof MassDriverBuild entity && other.isValid() && other.consValid() && entity.block == block && entity.link == pos() && within(other, range);
        }

        protected boolean linkValid(){
            if(link == -1) return false;
            return world.build(this.link) instanceof MassDriverBuild other && other.block == block && other.team == team && within(other, range);
        }

        @Override
        public Point2 config(){
            if(tile == null) return null;
            return Point2.unpack(link).sub(tile.x, tile.y);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(link);
            write.f(rotation);
            write.b((byte)state.ordinal());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            link = read.i();
            rotation = read.f();
            state = DriverState.all[read.b()];
        }
    }

    public enum DriverState{
        idle, //nothing is shooting at this mass driver and it does not have any target
        accepting, //currently getting shot at, unload items
        shooting;

        public static final DriverState[] all = values();
    }
}
