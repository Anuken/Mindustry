package mindustry.entities.units;

import mindustry.type.*;

public class WeaponMount{
    /** weapon associated with this mount */
    public final Weapon weapon;
    /** reload in frames; 0 means ready to fire */
    public float reload;
    /** rotation relative to the unit this mount is on */
    public float rotation;
    /** aiming position in world coordinates */
    public float aimX, aimY;
    /** side that's being shot - only valid for mirrors */
    public boolean side;

    public WeaponMount(Weapon weapon){
        this.weapon = weapon;
    }
}
