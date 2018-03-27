package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.world.Tile;

public class Centrifuge extends GenericCrafter {
    protected float powerUsed = 0.1f;
    protected float timeUsed = 360f;

    public Centrifuge(String name) {
        super(name);
    }

    @Override
    public void update(Tile tile){

    }
}
