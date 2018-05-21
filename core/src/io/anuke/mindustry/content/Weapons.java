package io.anuke.mindustry.content;

import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.Weapon;

public class Weapons {
    public static final Weapon

    blaster = new Weapon("blaster") {{
        length = 1.5f;
        reload = 15f;
        roundrobin = true;
        ejectEffect = ShootFx.shellEjectSmall;
        setAmmo(AmmoTypes.basicIron);
    }};
}
