package mindustry.entities.units;

import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;

public class PointDefenseMount extends WeaponMount{
	public float timerTarget = 0;
	public @Nullable Bullet target;

	public PointDefenseMount(Weapon weapon){
        super(weapon);
    }
}