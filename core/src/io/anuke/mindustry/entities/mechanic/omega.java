package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class omega extends Mech {
    protected TextureRegion armorRegion;

    public omega(String name, boolean flying) {
        super(name, flying);
        drillPower = 2;
        mineSpeed = 1.5f;
        itemCapacity = 50;
        speed = 0.36f;
        boostSpeed = 0.6f;
        mass = 4f;
        shake = 4f;
        weaponOffsetX = 1;
        weaponOffsetY = 0;
        engineColor = Color.valueOf("feb380");
        health = 320f;
        buildPower = 1.5f;
        weapon = new Weapon("swarmer") {{
            length = 1.5f;
            recoil = 4f;
            reload = 45f;
            shots = 4;
            spacing = 8f;
            inaccuracy = 8f;
            roundrobin = true;
            ejectEffect = Fx.none;
            shake = 3f;
            bullet = Bullets.missileSwarm;
        }};
    }

    @Override
    public float getRotationAlpha(Player player) {
        return 0.6f - player.shootHeat * 0.3f;
    }

    @Override
    public float spreadX(Player player) {
        return player.shootHeat * 2f;
    }

    @Override
    public void load() {
        super.load();
        armorRegion = Core.atlas.find(name + "-armor");
    }

    @Override
    public void updateAlt(Player player) {
        float scl = 1f - player.shootHeat / 2f;
        player.velocity().scl(scl);
    }

    @Override
    public float getExtraArmor(Player player) {
        return player.shootHeat * 30f;
    }

    @Override
    public void draw(Player player) {
        if (player.shootHeat <= 0.01f) return;

        Shaders.build.progress = player.shootHeat;
        Shaders.build.region = armorRegion;
        Shaders.build.time = Time.time() / 10f;
        Shaders.build.color.set(Pal.accent).a = player.shootHeat;
        Draw.shader(Shaders.build);
        Draw.rect(armorRegion, player.x, player.y, player.rotation);
        Draw.shader();
    }
}
