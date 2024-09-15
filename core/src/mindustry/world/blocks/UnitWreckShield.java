package mindustry.world.blocks;

import mindustry.gen.*;

//TODO: horrible API design, but I'm not sure of a better way to do this right now. please don't use this class
public interface UnitWreckShield{
    /** @return whether the shield was able to absorb the unit wreck; this should apply damage to the shield if true is returned. */
    boolean absorbWreck(Unit unit, float damage);
}
