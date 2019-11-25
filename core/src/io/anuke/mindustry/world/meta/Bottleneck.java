package io.anuke.mindustry.world.meta;

import io.anuke.arc.graphics.*;

/**
 * "port" of https://mods.factorio.com/mods/trold/Bottleneck
 */
public enum Bottleneck{
    none,   // green, doing fine <3
    output, // yellow, limited by output
    input;  // red, limited by input

    public static Color color(Bottleneck state){
        switch(state){
            case none  : return Color.green;
            case output: return Color.yellow;
            case input : return Color.red;
            default    : return Color.white;
        }
    }
}
