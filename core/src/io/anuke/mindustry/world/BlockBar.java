package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;

public class BlockBar {
    public final ValueSupplier value;
    public final Color color;
    public final boolean top;

    public BlockBar(Color color, boolean top, ValueSupplier value) {
        this.value = value;
        this.color = color;
        this.top = top;
    }

    public interface ValueSupplier{
        float get(Tile tile);
    }
}
