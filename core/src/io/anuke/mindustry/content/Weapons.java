package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;

public class Weapons implements ContentList {
    public static Weapon blaster, shockgun, sapper, swarmer;

    @Override
    public void load() {

        blaster = new Weapon("blaster") {{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletLead);
        }};

        shockgun = new Weapon("shockgun") {{
            length = 1f;
            reload = 50f;
            roundrobin = true;
            shots = 6;
            inaccuracy = 15f;
            recoil = 2f;
            velocityRnd = 0.7f;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.shotgunTungsten);
        }};

        sapper = new Weapon("sapper") {{
            length = 1.5f;
            reload = 12f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletCarbide);
        }};

        swarmer = new Weapon("swarmer") {{
            length = 1.5f;
            reload = 10f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletThermite);
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return Upgrade.all();
    }
}
