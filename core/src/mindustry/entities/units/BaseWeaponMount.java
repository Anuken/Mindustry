package mindustry.entities.units;

import mindustry.type.*;
import mindustry.type.weapons.*;

public class BaseWeaponMount{
    /** weapon associated with this mount */
    public final BaseWeapon weapon;
    /** rotation relative to the unit this mount is on */
    public float rotation;
    /** weapon recoil */
    public float recoil;
    /** current heat, 0 to 1*/
    public float heat;
    /** lerps to 1 when shooting, 0 when not */
    public float warmup;
    /** aiming position in world coordinates */
    public float aimX, aimY;
    /** whether to shoot right now */
    public boolean shoot = false;
    /** whether to rotate to face the target right now */
    public boolean rotate = false;
    /** destination rotation; do not modify! */
    public float targetRotation;

    public BaseWeaponMount(BaseWeapon weapon){
        this.weapon = weapon;
        this.rotation = weapon.baseRotation;
    }
}
