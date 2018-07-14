package io.anuke.mindustry.world.blocks.units;

import io.anuke.mindustry.content.StatusEffects;

public class ShieldProjector extends Projector{

    public ShieldProjector(String name){
        super(name);

        status = StatusEffects.shielded;
    }
}
