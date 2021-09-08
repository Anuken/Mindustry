package mindustry.entities.units;

import arc.util.*;
import mindustry.audio.*;
import mindustry.gen.*;
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
    /** current bullet for continuous weapons */
    public @Nullable Bullet bullet;
    /** sound loop for continuous weapons */
    public @Nullable SoundLoop sound;
    /** current target; used for autonomous weapons and AI */
    public @Nullable Teamc target;
    /** retarget counter */
    public float retarget = 0f;

    public WeaponMount(Weapon weapon){
        this.weapon = weapon;
    }
}
