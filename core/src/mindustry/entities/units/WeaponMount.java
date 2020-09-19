package mindustry.entities.units;

import mindustry.type.*;

public class WeaponMount{
    /** weapon associated with this mount */
    public final Weapon weapon;
    /** reload in frames; 0 means ready to fire */
    public float reload;
    /** rotation relative to the unit this mount is on */
    public float rotation;
    /** destination rotation; do not modify! */
    public float targetRotation;
    /** current heat, 0 to 1*/
    public float heat;
    /** aiming position in world coordinates */
    public float aimX, aimY;
    /** whether to shoot right now */
    public boolean shoot = false;
    /** whether to rotate to face the target right now */
    public boolean rotate = false;
    /** extra state for alternating weapons */
    public boolean side;

    public WeaponMount(Weapon weapon){
        this.weapon = weapon;
    }
}
