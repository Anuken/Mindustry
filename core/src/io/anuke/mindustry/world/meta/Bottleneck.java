package io.anuke.mindustry.world.meta;

import io.anuke.arc.graphics.*;

/**
 * "port" of https://mods.factorio.com/mods/trold/Bottleneck
 */
public enum Bottleneck{
    none(Color.green),    // working smoothly
    output(Color.yellow), // no room to output
    input(Color.red);     // insufficient input

    public final Color color;

    Bottleneck(Color color){
        this.color = color;
    }
}
