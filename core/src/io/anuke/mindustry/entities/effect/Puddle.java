package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.SolidEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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

    /**Deserialization use only!*/
    public Puddle(){
    }

    /**Deposists a puddle between tile and source.*/
    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount){
        deposit(tile, source, liquid, amount, 0);
    }

    /**Deposists a puddle at a tile.*/
    public static void deposit(Tile tile, Liquid liquid, float amount){
        deposit(tile, tile, liquid, amount, 0);
    }

    /**Returns the puddle on the specified tile. May return null.*/
    public static Puddle getPuddle(Tile tile){
        return map.get(tile.pos());
    }

    private static void deposit(Tile tile, Tile source, Liquid liquid, float amount, int generation){
        if(tile == null) return;

        if(tile.floor().isLiquid && !canStayOn(liquid, tile.floor().liquidDrop)){
            reactPuddle(tile.floor().liquidDrop, liquid, amount, tile,
                    (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);

            Puddle p = map.get(tile.pos());

            if(generation == 0 && p != null && p.lastRipple <= Timers.time() - 40f){
                Effects.effect(BlockFx.ripple, tile.floor().liquidDrop.color,
                        (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
                p.lastRipple = Timers.time();
            }
            return;
        }

        Puddle p = map.get(tile.pos());
        if(p == null){
            if(Net.client()) return; //not clientside.

            Puddle puddle = Pooling.obtain(Puddle.class, Puddle::new);
            puddle.tile = tile;
            puddle.liquid = liquid;
            puddle.amount = amount;
            puddle.generation = (byte) generation;
            puddle.set((tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
            puddle.add();
            map.put(tile.pos(), puddle);
        }else if(p.liquid == liquid){
            p.accepting = Math.max(amount, p.accepting);

            if(generation == 0  && p.lastRipple <= Timers.time() - 40f && p.amount >= maxLiquid / 2f){
                Effects.effect(BlockFx.ripple, p.liquid.color, (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
                p.lastRipple = Timers.time();
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

    /**Reacts two liquids together at a location.*/
    private static float reactPuddle(Liquid dest, Liquid liquid, float amount, Tile tile, float x, float y){
        if((dest.flammability > 0.3f && liquid.temperature > 0.7f) ||
                (liquid.flammability > 0.3f && dest.temperature > 0.7f)){ //flammable liquid + hot liquid
            Fire.create(tile);
            if(Mathf.chance(0.006 * amount)){
                Call.createBullet(TurretBullets.fireball, x, y, Mathf.random(360f));
            }
        }else if(dest.temperature > 0.7f && liquid.temperature < 0.55f){ //cold liquid poured onto hot puddle
            if(Mathf.chance(0.5f * amount)){
                Effects.effect(EnvironmentFx.steam, x, y);
            }
            return -0.1f * amount;
        }else if(liquid.temperature > 0.7f && dest.temperature < 0.55f){ //hot liquid poured onto cold puddle
            if(Mathf.chance(0.8f * amount)){
                Effects.effect(EnvironmentFx.steam, x, y);
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
    public void getHitbox(Rectangle rectangle){
        rectangle.setCenter(x, y).setSize(tilesize);
    }

    @Override
    public void getHitboxTile(Rectangle rectangle){
        rectangle.setCenter(x, y).setSize(0f);
    }

    @Override
    public void update(){

        //no updating happens clientside
        if(Net.client()){
            amount = Mathf.lerpDelta(amount, targetAmount, 0.15f);
        }else{
            //update code
            float addSpeed = accepting > 0 ? 3f : 0f;

            amount -= Timers.delta() * (1f - liquid.viscosity) / (5f + addSpeed);

            amount += accepting;
            accepting = 0f;

            if(amount >= maxLiquid / 1.5f && generation < maxGeneration){
                float deposited = Math.min((amount - maxLiquid / 1.5f) / 4f, 0.3f) * Timers.delta();
                for(GridPoint2 point : Geometry.d4){
                    Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                    if(other != null && other.block() == Blocks.air && !other.hasCliffs()){
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
            Units.getNearby(rect.setSize(Mathf.clamp(amount / (maxLiquid / 1.5f)) * 10f).setCenter(x, y), unit -> {
                if(unit.isFlying()) return;

                unit.getHitbox(rect2);
                if(!rect.overlaps(rect2)) return;

                unit.applyEffect(liquid.effect, 0.5f);

                if(unit.getVelocity().len() > 0.1){
                    Effects.effect(BlockFx.ripple, liquid.color, unit.x, unit.y);
                }
            });

            if(liquid.temperature > 0.7f && tile.entity != null && Mathf.chance(0.3 * Timers.delta())){
                Fire.create(tile);
            }

            updateTime = 20f;
        }

        updateTime -= Timers.delta();
    }

    @Override
    public void draw(){
        seeds = id;
        boolean onLiquid = tile.floor().isLiquid;
        float f = Mathf.clamp(amount / (maxLiquid / 1.5f));
        float smag = onLiquid ? 0.8f : 0f;
        float sscl = 20f;

        Draw.color(Hue.shift(tmp.set(liquid.color), 2, -0.05f));
        Fill.circle(x + Mathf.sin(Timers.time() + seeds * 532, sscl, smag), y + Mathf.sin(Timers.time() + seeds * 53, sscl, smag), f * 8f);
        Angles.randLenVectors(id, 3, f * 6f, (ex, ey) -> {
            Fill.circle(x + ex + Mathf.sin(Timers.time() + seeds * 532, sscl, smag),
                    y + ey + Mathf.sin(Timers.time() + seeds * 53, sscl, smag), f * 5f);
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
    public void readSave(DataInput stream) throws IOException{
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
        map.remove(tile.pos());
        reset();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeByte(liquid.id);
        data.writeShort((short) (amount * 4));
        data.writeInt(tile.pos());
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        x = data.readFloat();
        y = data.readFloat();
        liquid = content.liquid(data.readByte());
        targetAmount = data.readShort() / 4f;
        tile = world.tile(data.readInt());

        map.put(tile.pos(), this);
    }

    @Override
    public EntityGroup targetGroup(){
        return puddleGroup;
    }
}
