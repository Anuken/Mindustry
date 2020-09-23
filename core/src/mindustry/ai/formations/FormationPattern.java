package mindustry.ai.formations;

import arc.math.geom.*;

/**
 * The {@code FormationPattern} interface represents the shape of a formation and generates the slot offsets, relative to its
 * anchor point. Since formations can be scalable the pattern must be able to determine if a given number of slots is supported.
 * <p>
 * Each particular pattern (such as a V, wedge, circle) needs its own instance of a class that implements this
 * {@code FormationPattern} interface.
 * @author davebaol
 */
public abstract class FormationPattern{
    public int slots;
    /** Spacing between members. */
    public float spacing = 20f;

    /** Returns the location of the given slot index. */
    public abstract Vec3 calculateSlotLocation(Vec3 out, int slot);

    /**
     * Returns true if the pattern can support the given number of slots
     * @param slotCount the number of slots
     * @return {@code true} if this pattern can support the given number of slots; {@code false} othervwise.
     */
    public boolean supportsSlots(int slotCount){
        return true;
    }
}
