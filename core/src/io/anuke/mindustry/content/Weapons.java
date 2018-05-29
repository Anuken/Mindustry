package io.anuke.mindustry.content;

import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Weapon;

public class Weapons implements ContentList {
    public static Weapon blaster;

    @Override
    public void load() {

        blaster = new Weapon("blaster") {{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletIron);
        }};
    }
}
