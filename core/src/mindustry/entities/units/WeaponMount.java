package mindustry.entities.units;

import arc.util.*;
import mindustry.audio.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import mindustry.type.weapons.ReloadWeapon.*;

public class WeaponMount extends ReloadWeaponMount{
    /** weapon barrel recoil */
    public @Nullable float[] recoils;
    /** is the weapon actively charging */
    public boolean charging;
    /** counts up to 1 when charging, 0 when not */
    public float charge;
    /** counter for which barrel bullets have been fired from; used for alternating patterns */
    public int barrelCounter;
    /** Last aim length of weapon. Only used for point lasers. */
    public float lastLength;
    /** last fired bullet */
    public @Nullable Bullet bullet;
    /** sound loop for continuous weapons */
    public @Nullable SoundLoop sound;

    public WeaponMount(BaseWeapon weapon){
        super(weapon);
    }
}
