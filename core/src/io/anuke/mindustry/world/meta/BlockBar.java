package io.anuke.mindustry.world.meta;

import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

public class BlockBar{
    public final ValueSupplier value;
    public final BarType type;
    public final boolean top;

    public BlockBar(BarType type, boolean top, ValueSupplier value){
        this.value = value;
        this.type = type;
        this.top = top;
    }

    public interface ValueSupplier{
        default float get(Tile tile){
            return getValue(tile) / getMax(tile);
        }

        float getValue(Tile tile);

        float getMax(Tile tile);
    }

    public static class Value implements ValueSupplier{
        @FunctionalInterface
        public interface TileCallable{
            float call(Tile tile);
        }

        private TileCallable value;
        private TileCallable max;

        public Value(TileCallable value, TileCallable max){
            this.value = value;
            this.max = max;
        }

        @Override
        public float getValue(Tile tile){
            return value.call(tile);
        }

        @Override
        public float getMax(Tile tile){
            return max.call(tile);
        }
    }
}
