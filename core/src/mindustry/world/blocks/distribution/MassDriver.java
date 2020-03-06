package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MassDriver extends Block{
    public float range;
    public float rotateSpeed = 0.04f;
    public float translation = 7f;
    public int minDistribute = 10;
    public float knockback = 4f;
    public float reloadTime = 100f;
    public Effect shootEffect = Fx.shootBig2;
    public Effect smokeEffect = Fx.shootBigSmoke2;
    public Effect recieveEffect = Fx.mineBig;
    public float shake = 3f;
    public TextureRegion baseRegion;

    public MassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasItems = true;
        layer = Layer.turret;
        hasPower = true;
        outlineIcon = true;
    //point2 is relative
        config(Point2.class, (tile, point) -> tile.<MassDriverEntity>ent().link = Point2.pack(point.x + tile.x, point.y + tile.y));
        config(Integer.class, (tile, point) -> tile.<MassDriverEntity>ent().link = point);
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
    public void updateTile(){
        Tile link = world.tile(link);
        boolean hasLink = linkValid(tile);

        //reload regardless of state
        if(reload > 0f){
            reload = Mathf.clamp(reload - delta() / reloadTime * efficiency());
        }

        //cleanup waiting shooters that are not valid
        if(!shooterValid(tile, currentShooter())){
            waitingShooters.remove(currentShooter());
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
            tryDump(tile);
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
            rotation = Mathf.slerpDelta(rotation, tile.angleTo(currentShooter()), rotateSpeed * efficiency());
        }else if(state == DriverState.shooting){
            //if there's nothing to shoot at OR someone wants to shoot at this thing, bail
            if(!hasLink || (!waitingShooters.isEmpty() && (itemCapacity - items.total() >= minDistribute))){
                state = DriverState.idle;
                return;
            }

            float targetRotation = tile.angleTo(link);

            if(
                tile.items.total() >= minDistribute && //must shoot minimum amount of items
                link.block().itemCapacity - link.items.total() >= minDistribute //must have minimum amount of space
            ){
                MassDriverEntity other = link.ent();
                other.waitingShooters.add(tile);

                if(reload <= 0.0001f){

                    //align to target location
                    rotation = Mathf.slerpDelta(rotation, targetRotation, rotateSpeed * efficiency());

                    //fire when it's the first in the queue and angles are ready.
                    if(other.currentShooter() == tile &&
                    other.state == DriverState.accepting &&
                    Angles.near(rotation, targetRotation, 2f) && Angles.near(other.rotation, targetRotation + 180f, 2f)){
                        //actually fire
                        fire(tile, link);
                        //remove waiting shooters, it's done firing
                        other.waitingShooters.remove(tile);
                        //set both states to idle
                        state = DriverState.idle;
                        other.state = DriverState.idle;
                    }
                }
            }
        }
    }

    @Override
    public void draw(){
        Draw.rect(baseRegion, x, y);
    }

    @Override
    public void drawLayer(){
        Draw.rect(region,
        x + Angles.trnsx(rotation + 180f, reload * knockback),
        y + Angles.trnsy(rotation + 180f, reload * knockback), rotation - 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize, y*tilesize, range, Pal.accent);

        //check if a mass driver is selected while placing this driver
        if(!control.input.frag.config.isShown()) return;
        Tile selected = control.input.frag.config.getSelectedTile();
        if(selected == null || !(selected.block() instanceof MassDriver) || !(selected.dst(x * tilesize, y * tilesize) <= range)) return;

        //if so, draw a dotted line towards it while it is in range
        float sin = Mathf.absin(Time.time(), 6f, 1f);
        Tmp.v1.set(x * tilesize + offset(), y * tilesize + offset()).sub(selected.drawx(), selected.drawy()).limit((size / 2f + 1) * tilesize + sin + 0.5f);
        float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
            x1 = selected.drawx() + Tmp.v1.x, y1 = selected.drawy() + Tmp.v1.y;
        int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

        Lines.stroke(4f, Pal.gray);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Lines.stroke(2f, Pal.placing);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Draw.reset();
    }

    @Override
    public void drawConfigure(){
        float sin = Mathf.absin(Time.time(), 6f, 1f);

        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Drawf.circles(x, y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

        for(Tile shooter : waitingShooters){
            Drawf.circles(shooter.drawx(), shooter.drawy(), (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
            Drawf.arrow(shooter.drawx(), shooter.drawy(), x, y, size * tilesize + sin, 4f + sin, Pal.place);
        }

        if(linkValid(tile)){
            Tile target = world.tile(link);
            Drawf.circles(target.drawx(), target.drawy(), (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
            Drawf.arrow(x, y, target.drawx(), target.drawy(), size * tilesize + sin, 4f + sin);
        }

        Drawf.dashCircle(x, y, range, Pal.accent);
    }

    @Override
    public boolean onConfigureTileTapped(Tile other){
        if(tile == other) return false;

        if(link == other.pos()){
            tile.configure(-1);
            return false;
        }else if(other.block() instanceof MassDriver && other.dst(tile) <= range && other.team() == team){
            tile.configure(other.pos());
            return false;
        }

        return true;
    }

    @Override
    public boolean acceptItem(Tilec source, Item item){
        //mass drivers that ouput only cannot accept items
        return tile.items.total() < itemCapacity && linkValid(tile);
    }

    protected void fire(Tile target){
        MassDriverEntity other = target.ent();

        //reset reload, use power.
        reload = 1f;

        DriverBulletData data = Pools.obtain(DriverBulletData.class, DriverBulletData::new);
        data.from = entity;
        data.to = other;
        int totalUsed = 0;
        for(int i = 0; i < content.items().size; i++){
            int maxTransfer = Math.min(items.get(content.item(i)), ((MassDriver)tile.block()).itemCapacity - totalUsed);
            data.items[i] = maxTransfer;
            totalUsed += maxTransfer;
            items.remove(content.item(i), maxTransfer);
        }

        float angle = tile.angleTo(target);

        Bullets.driverBolt.create(entity, team(),
        x + Angles.trnsx(angle, translation), y + Angles.trnsy(angle, translation),
        angle, -1f, 1f, 1f, data);

        shootEffect.at(x + Angles.trnsx(angle, translation),
        y + Angles.trnsy(angle, translation), angle);

        smokeEffect.at(x + Angles.trnsx(angle, translation),
        y + Angles.trnsy(angle, translation), angle);

        Effects.shake(shake, shake, entity);
    }

    protected void handlePayload(MassDriverEntity entity, Bulletc bullet, DriverBulletData data){
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

        Effects.shake(shake, shake, entity);
        recieveEffect.at(bullet);

        reload = 1f;
        bullet.remove();
    }

    protected boolean shooterValid(Tile other){

        if(other == null) return true;
        if(!(other.block() instanceof MassDriver)) return false;
        MassDriverEntity entity = other.ent();
        return link == tile.pos() && tile.dst(other) <= range;
    }

    protected boolean linkValid(){
        if(tile == null) return false;
        if(entity == null || link == -1) return false;
        Tile link = world.tile(link);

        return link != null && link.block() instanceof MassDriver && link.team() == team && tile.dst(link) <= range;
    }

    public class DriverBulletData implements Poolable{
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

        public void handlePayload(Bulletc bullet, DriverBulletData data){
            ((MassDriver)block()).handlePayload(this, bullet, data);
        }

        @Override
        public Point2 config(){
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
            state = DriverState.values()[read.b()];
        }
    }

    enum DriverState{
        idle, //nothing is shooting at this mass driver and it does not have any target
        accepting, //currently getting shot at, unload items
        shooting,
        unloading
    }
}
