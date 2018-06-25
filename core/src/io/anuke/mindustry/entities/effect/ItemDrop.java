package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.Pools;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.SolidEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.entities.trait.TimeTrait;
import io.anuke.ucore.entities.trait.VelocityTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class ItemDrop extends SolidEntity implements SaveTrait, SyncTrait, DrawTrait, VelocityTrait, TimeTrait, TargetTrait, Poolable {
    public static int typeID = -1;

    private static final float sinkLifetime = 80f;

    private Interpolator interpolator = new Interpolator();
    private Item item;
    private int amount;

    private Vector2 velocity = new Vector2();
    private float time;
    private float sinktime;

    public static ItemDrop create(Item item, int amount, float x, float y, float angle){
        ItemDrop drop = Pools.obtain(ItemDrop.class);
        drop.item = item;
        drop.amount = amount;
        drop.velocity.set(4f, 0f).rotate(angle);
        drop.set(x, y);
        drop.add();

        return drop;
    }

    @Remote(called = Loc.server, in = In.entities)
    public static void createItemDrop(Item item, int amount, float x, float y, float velocityX, float velocityY){
        create(item, amount, x, y, 0).getVelocity().set(velocityX, velocityY);
    }

    @Remote(called = Loc.server, in = In.entities)
    public static void onPickup(int itemid){
        ItemDrop drop = itemGroup.getByID(itemid);
        if(drop != null){
            Effects.effect(UnitFx.pickup, drop);
        }
        itemGroup.removeByID(itemid);
    }

    /**Internal use only!*/
    public ItemDrop(){
        hitbox.setSize(5f);
        hitboxTile.setSize(5f);
    }

    public Item getItem() {
        return item;
    }

    public int getAmount(){
        return amount;
    }

    @Override
    public boolean isDead() {
        return !isAdded();
    }

    @Override
    public Team getTeam() {
        return Team.none;
    }

    @Override
    public int getTypeID() {
        return typeID;
    }

    @Override
    public float lifetime() {
        return 60*60;
    }

    @Override
    public void time(float time) {
        this.time = time;
    }

    @Override
    public float time() {
        return time;
    }

    @Override
    public Vector2 getVelocity() {
        return velocity;
    }

    @Override
    public boolean collides(SolidTrait other) {
        return other instanceof Player && time > 20f;
    }

    @Override
    public void collision(SolidTrait other, float x, float y) {
        Unit player = (Unit)other;
        if(player.inventory.canAcceptItem(item, 1)){
            int used = Math.min(amount, player.inventory.capacity() - player.inventory.getItem().amount);
            player.inventory.addItem(item, used);
            amount -= used;

            if(amount <= 0) {
                CallEntity.onPickup(getID());
            }
        }
    }

    @Override
    public void draw() {
        float size = itemSize * (1f - sinktime/sinkLifetime) * (1f-Mathf.curve(fin(), 0.98f));

        Tile tile = world.tileWorld(x, y);

        Draw.color(Color.WHITE, tile == null || !tile.floor().isLiquid ? Color.WHITE : tile.floor().liquidColor, sinktime/sinkLifetime);
        Draw.rect(item.region, x, y, size, size);

        int stored = Mathf.clamp(amount / 6, 1, 8);

        for(int i = 0; i < stored; i ++) {
            float px = stored == 1 ? 0 : Mathf.randomSeedRange(i + 1, 4f);
            float py = stored == 1 ? 0 : Mathf.randomSeedRange(i + 2, 4f);
            Draw.rect(item.region, x + px, y + py, size, size);
        }

        Draw.color();
    }

    @Override
    public void update() {
        if(Net.client()) {
            interpolate();
        }else{
            updateVelocity(0.2f);
            updateTime();
        }

        Tile tile = world.tileWorld(x, y);

        if(tile.floor().isLiquid){
            sinktime += Timers.delta();

            if(Mathf.chance(0.04 * Timers.delta())){
                Effects.effect(tile.floor().drownUpdateEffect, tile.floor().liquidColor, x, y);
            }

            if(sinktime >= sinkLifetime){
                CallEntity.onPickup(getID());
            }
        }else{
            sinktime = 0f;
        }
    }

    @Override
    public void removed() {
        Pools.free(this);
    }

    @Override
    public void reset() {
        time = 0f;
        interpolator.reset();
    }

    @Override
    public Interpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public float drawSize() {
        return 10;
    }

    @Override
    public EntityGroup targetGroup() {
        return itemGroup;
    }

    @Override
    public void writeSave(DataOutput data) throws IOException {
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeByte(item.id);
        data.writeShort(amount);
    }

    @Override
    public void readSave(DataInput data) throws IOException {
        x = data.readFloat();
        y = data.readFloat();
        item = Item.getByID(data.readByte());
        amount = data.readShort();
        add();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeByte(item.id);
        data.writeShort(amount);
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        interpolator.read(x, y, data.readFloat(), data.readFloat(), time);
        item = Item.getByID(data.readByte());
        amount = data.readShort();
    }
}
