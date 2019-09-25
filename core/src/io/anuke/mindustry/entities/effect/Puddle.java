package io.anuke.mindustry.entities.effect;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.collection.IntMap;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.pooling.Pool.Poolable;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.SolidEntity;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.TypeID;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Puddle extends SolidEntity implements SaveTrait, Poolable, DrawTrait, SyncTrait{
    private static final IntMap<Puddle> map = new IntMap<>();
    private static final float maxLiquid = 70f;
    private static final int maxGeneration = 2;
    private static final Color tmp = new Color();
    private static final Rectangle rect = new Rectangle();
    private static final Rectangle rect2 = new Rectangle();
    private static int seeds;

    private int loadedPosition = -1;

    private float updateTime;
    private float lastRipple;
    private Tile tile;
    private Liquid liquid;
    private float amount, targetAmount;
    private float accepting;
    private byte generation;

    /** Deserialization use only! */
    public Puddle(){
    }

    /** Deposists a puddle between tile and source. */
    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount){
        deposit(tile, source, liquid, amount, 0);
    }

    /** Deposists a puddle at a tile. */
    public static void deposit(Tile tile, Liquid liquid, float amount){
        deposit(tile, tile, liquid, amount, 0);
    }

    /** Returns the puddle on the specified tile. May return null. */
    public static Puddle getPuddle(Tile tile){
        return map.get(tile.pos());
    }

    private static void deposit(Tile tile, Tile source, Liquid liquid, float amount, int generation){
        if(tile == null) return;

        if(tile.floor().isLiquid && !canStayOn(liquid, tile.floor().liquidDrop)){
            reactPuddle(tile.floor().liquidDrop, liquid, amount, tile,
            (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);

            Puddle p = map.get(tile.pos());

            if(generation == 0 && p != null && p.lastRipple <= Time.time() - 40f){
                Effects.effect(Fx.ripple, tile.floor().liquidDrop.color,
                (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
                p.lastRipple = Time.time();
            }
            return;
        }

        Puddle p = map.get(tile.pos());
        if(p == null){
            if(net.client()) return; //not clientside.

            Puddle puddle = Pools.obtain(Puddle.class, Puddle::new);
            puddle.tile = tile;
            puddle.liquid = liquid;
            puddle.amount = amount;
            puddle.generation = (byte)generation;
            puddle.set((tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
            puddle.add();
            map.put(tile.pos(), puddle);
        }else if(p.liquid == liquid){
            p.accepting = Math.max(amount, p.accepting);

            if(generation == 0 && p.lastRipple <= Time.time() - 40f && p.amount >= maxLiquid / 2f){
                Effects.effect(Fx.ripple, p.liquid.color, (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
                p.lastRipple = Time.time();
            }
        }else{
            p.amount += reactPuddle(p.liquid, liquid, amount, p.tile, p.x, p.y);
        }
    }

    /**
     * Returns whether the first liquid can 'stay' on the second one.
     * Currently, the only place where this can happen is oil on water.
     */
    private static boolean canStayOn(Liquid liquid, Liquid other){
        return liquid == Liquids.oil && other == Liquids.water;
    }

    /** Reacts two liquids together at a location. */
    private static float reactPuddle(Liquid dest, Liquid liquid, float amount, Tile tile, float x, float y){
        if((dest.flammability > 0.3f && liquid.temperature > 0.7f) ||
        (liquid.flammability > 0.3f && dest.temperature > 0.7f)){ //flammable liquid + hot liquid
            Fire.create(tile);
            if(Mathf.chance(0.006 * amount)){
                Call.createBullet(Bullets.fireball, x, y, Mathf.random(360f));
            }
        }else if(dest.temperature > 0.7f && liquid.temperature < 0.55f){ //cold liquid poured onto hot puddle
            if(Mathf.chance(0.5f * amount)){
                Effects.effect(Fx.steam, x, y);
            }
            return -0.1f * amount;
        }else if(liquid.temperature > 0.7f && dest.temperature < 0.55f){ //hot liquid poured onto cold puddle
            if(Mathf.chance(0.8f * amount)){
                Effects.effect(Fx.steam, x, y);
            }
            return -0.4f * amount;
        }
        return 0f;
    }

    @Remote(called = Loc.server)
    public static void onPuddleRemoved(int puddleid){
        puddleGroup.removeByID(puddleid);
    }

    public float getFlammability(){
        return liquid.flammability * amount;
    }

    @Override
    public TypeID getTypeID(){
        return TypeIDs.puddle;
    }

    @Override
    public byte version(){
        return 0;
    }

    @Override
    public void hitbox(Rectangle rectangle){
        rectangle.setCenter(x, y).setSize(tilesize);
    }

    @Override
    public void hitboxTile(Rectangle rectangle){
        rectangle.setCenter(x, y).setSize(0f);
    }

    @Override
    public void update(){

        //no updating happens clientside
        if(net.client()){
            amount = Mathf.lerpDelta(amount, targetAmount, 0.15f);
        }else{
            //update code
            float addSpeed = accepting > 0 ? 3f : 0f;

            amount -= Time.delta() * (1f - liquid.viscosity) / (5f + addSpeed);

            amount += accepting;
            accepting = 0f;

            if(amount >= maxLiquid / 1.5f && generation < maxGeneration){
                float deposited = Math.min((amount - maxLiquid / 1.5f) / 4f, 0.3f) * Time.delta();
                for(Point2 point : Geometry.d4){
                    Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                    if(other != null && other.block() == Blocks.air){
                        deposit(other, tile, liquid, deposited, generation + 1);
                        amount -= deposited / 2f; //tweak to speed up/slow down puddle propagation
                    }
                }
            }

            amount = Mathf.clamp(amount, 0, maxLiquid);

            if(amount <= 0f){
                Call.onPuddleRemoved(getID());
            }
        }

        //effects-only code
        if(amount >= maxLiquid / 2f && updateTime <= 0f){
            Units.nearby(rect.setSize(Mathf.clamp(amount / (maxLiquid / 1.5f)) * 10f).setCenter(x, y), unit -> {
                if(unit.isFlying()) return;

                unit.hitbox(rect2);
                if(!rect.overlaps(rect2)) return;

                unit.applyEffect(liquid.effect, 60 * 2);

                if(unit.velocity().len() > 0.1){
                    Effects.effect(Fx.ripple, liquid.color, unit.x, unit.y);
                }
            });

            if(liquid.temperature > 0.7f && (tile.link().entity != null) && Mathf.chance(0.3 * Time.delta())){
                Fire.create(tile);
            }

            updateTime = 20f;
        }

        updateTime -= Time.delta();
    }

    @Override
    public void draw(){
        seeds = id;
        boolean onLiquid = tile.floor().isLiquid;
        float f = Mathf.clamp(amount / (maxLiquid / 1.5f));
        float smag = onLiquid ? 0.8f : 0f;
        float sscl = 20f;

        Draw.color(tmp.set(liquid.color).shiftValue(-0.05f));
        Fill.circle(x + Mathf.sin(Time.time() + seeds * 532, sscl, smag), y + Mathf.sin(Time.time() + seeds * 53, sscl, smag), f * 8f);
        Angles.randLenVectors(id, 3, f * 6f, (ex, ey) -> {
            Fill.circle(x + ex + Mathf.sin(Time.time() + seeds * 532, sscl, smag),
            y + ey + Mathf.sin(Time.time() + seeds * 53, sscl, smag), f * 5f);
            seeds++;
        });
        Draw.color();
    }

    @Override
    public float drawSize(){
        return 20;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeInt(tile.pos());
        stream.writeFloat(x);
        stream.writeFloat(y);
        stream.writeByte(liquid.id);
        stream.writeFloat(amount);
        stream.writeByte(generation);
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
        this.loadedPosition = stream.readInt();
        this.x = stream.readFloat();
        this.y = stream.readFloat();
        this.liquid = content.liquid(stream.readByte());
        this.amount = stream.readFloat();
        this.generation = stream.readByte();
        add();
    }

    @Override
    public void reset(){
        loadedPosition = -1;
        tile = null;
        liquid = null;
        amount = 0;
        generation = 0;
        accepting = 0;
    }

    @Override
    public void added(){
        if(loadedPosition != -1){
            map.put(loadedPosition, this);
            tile = world.tile(loadedPosition);
        }
    }

    @Override
    public void removed(){
        if(tile != null){
            map.remove(tile.pos());
        }
        reset();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeByte(liquid.id);
        data.writeShort((short)(amount * 4));
        data.writeInt(tile.pos());
    }

    @Override
    public void read(DataInput data) throws IOException{
        x = data.readFloat();
        y = data.readFloat();
        liquid = content.liquid(data.readByte());
        targetAmount = data.readShort() / 4f;
        int pos = data.readInt();
        tile = world.tile(pos);

        map.put(pos, this);
    }

    @Override
    public EntityGroup targetGroup(){
        return puddleGroup;
    }
}
