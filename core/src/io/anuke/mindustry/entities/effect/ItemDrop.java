package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.entities.component.DrawTrait;
import io.anuke.ucore.entities.impl.SolidEntity;

import java.nio.ByteBuffer;

public class ItemDrop extends SolidEntity implements SyncTrait, DrawTrait {
    private Interpolator interpolator = new Interpolator();
    private Item item;

    public static ItemDrop create(Item item, float x, float y){
        ItemDrop drop = Pools.obtain(ItemDrop.class);
        drop.item = item;
        drop.set(x, y);

        return drop;
    }

    /**Internal use only!*/
    public ItemDrop(){}

    @Override
    public void removed() {
        Pools.free(this);
    }

    @Override
    public void draw() {

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
    public void write(ByteBuffer data) {
        data.putFloat(x);
        data.putFloat(y);
    }

    @Override
    public void read(ByteBuffer data, long time) {

    }
}
