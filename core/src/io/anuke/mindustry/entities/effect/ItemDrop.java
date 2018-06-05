package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.net.Interpolator;
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

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.itemGroup;
import static io.anuke.mindustry.Vars.itemSize;
import static io.anuke.mindustry.Vars.world;

public class ItemDrop extends SolidEntity implements SyncTrait, DrawTrait, VelocityTrait, TimeTrait, Poolable {
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

    /**Internal use only!*/
    public ItemDrop(){
        hitbox.setSize(5f);
        hitboxTile.setSize(5f);
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
        Player player = (Player)other;
        if(player.inventory.canAcceptItem(item, amount)){
            player.inventory.addItem(item, amount);
            remove();
        }
    }

    @Override
    public void draw() {
        float size = itemSize * (1f - sinktime/sinkLifetime);

        Tile tile = world.tileWorld(x, y);

        Draw.color(Color.WHITE, tile == null || !tile.floor().liquid ? Color.WHITE : tile.floor().liquidColor, sinktime/sinkLifetime);
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
        updateVelocity(0.2f);
        updateTime();

        Tile tile = world.tileWorld(x, y);

        if(tile.floor().liquid){
            sinktime += Timers.delta();

            if(Mathf.chance(0.04 * Timers.delta())){
                Effects.effect(tile.floor().drownUpdateEffect, tile.floor().liquidColor, x, y);
            }

            if(sinktime >= sinkLifetime){
                remove();
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
    public void write(ByteBuffer data) {
        data.putFloat(x);
        data.putFloat(y);
        data.put((byte)item.id);
    }

    @Override
    public void read(ByteBuffer data, long time) {
        x = data.getFloat();
        y = data.getFloat();
        item = Item.getByID(data.get());
    }
}
