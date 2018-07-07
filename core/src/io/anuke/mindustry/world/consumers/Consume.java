package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;

public interface Consume {
    void update(TileEntity entity);
    boolean valid();
}
