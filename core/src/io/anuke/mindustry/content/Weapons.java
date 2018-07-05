package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;

public class Weapons implements ContentList {
    public static Weapon blaster, chainBlaster, shockgun, sapper, swarmer, bomber, flakgun, flamethrower, missiles;

    @Override
    public void load() {

        blaster = new Weapon("blaster") {{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletLead);
        }};

        missiles = new Weapon("missiles") {{
            length = 1.5f;
            reload = 40f;
            shots = 2;
            inaccuracy = 10f;
            roundrobin = false;
            roundrobin = true;
            ejectEffect = Fx.none;
            setAmmo(AmmoTypes.weaponMissile);
        }};

        chainBlaster = new Weapon("chain-blaster") {{
            length = 1.5f;
            reload = 30f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletLead, AmmoTypes.bulletCarbide, AmmoTypes.bulletTungsten, AmmoTypes.bulletSilicon, AmmoTypes.bulletThorium);
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

        flakgun = new Weapon("flakgun") {{
            length = 1f;
            reload = 70f;
            roundrobin = true;
            shots = 1;
            inaccuracy = 3f;
            recoil = 3f;
            velocityRnd = 0.1f;
            ejectEffect = ShootFx.shellEjectMedium;
            setAmmo(AmmoTypes.shellCarbide);
        }};

        flamethrower = new Weapon("flamethrower") {{
            length = 1f;
            reload = 14f;
            roundrobin = true;
            recoil = 1f;
            ejectEffect = Fx.none;
            setAmmo(AmmoTypes.flamerThermite);
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
            setAmmo(AmmoTypes.bulletPyratite);
        }};

        bomber = new Weapon("bomber") {{
            length = 0f;
            width = 2f;
            reload = 5f;
            roundrobin = true;
            ejectEffect = Fx.none;
            velocityRnd = 1f;
            inaccuracy = 40f;
            setAmmo(AmmoTypes.bombExplosive, AmmoTypes.bombIncendiary, AmmoTypes.bombOil);
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return Upgrade.all();
    }
}
