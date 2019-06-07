package io.anuke.mindustry.entities.mechanic;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Weapon;

public class javelin extends Mech {
    float minV = 3.6f;
    float maxV = 6f;
    TextureRegion shield;

    public javelin(String name, boolean flying) {
        super(name, flying);
        drillPower = -1;
        speed = 0.11f;
        drag = 0.01f;
        mass = 2f;
        health = 170f;
        engineColor = Color.valueOf("d3ddff");
        cellTrnsY = 1f;
        weapon = new Weapon("missiles"){{
            length = 1.5f;
            reload = 70f;
            shots = 4;
            inaccuracy = 2f;
            roundrobin = true;
            ejectEffect = Fx.none;
            velocityRnd = 0.2f;
            spacing = 1f;
            bullet = Bullets.missileJavelin;
        }};
    }

    @Override
    public void load(){
        super.load();
        shield = Core.atlas.find(name + "-shield");
    }

    @Override
    public float getRotationAlpha(Player player){
        return 0.5f;
    }

    @Override
    public void updateAlt(Player player){
        float scl = scld(player);
        if(Mathf.chance(Time.delta() * (0.15 * scl))){
            Effects.effect(Fx.hitLancer, Pal.lancerLaser, player.x, player.y);
            Lightning.create(player.getTeam(), Pal.lancerLaser, 10f,
                    player.x + player.velocity().x, player.y + player.velocity().y, player.rotation, 14);
        }
    }

    @Override
    public void draw(Player player){
        float scl = scld(player);
        if(scl < 0.01f) return;
        Draw.color(Pal.lancerLaser);
        Draw.alpha(scl / 2f);
        Draw.blend(Blending.additive);
        Draw.rect(shield, player.x + Mathf.range(scl / 2f), player.y + Mathf.range(scl / 2f), player.rotation - 90);
        Draw.blend();
    }

    float scld(Player player){
        return Mathf.clamp((player.velocity().len() - minV) / (maxV - minV));
    }
}
