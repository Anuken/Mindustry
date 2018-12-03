package io.anuke.mindustry.world.meta;

import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;

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
}
