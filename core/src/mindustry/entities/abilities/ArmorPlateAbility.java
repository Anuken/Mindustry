package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ArmorPlateAbility extends Ability{
    public TextureRegion plateRegion;
    public Color color = Color.valueOf("d1efff");

    public float healthMultiplier = 0.2f;
    public float z = Layer.effect;

    protected float warmup;

    @Override
    public void update(Unit unit){
        super.update(unit);

        warmup = Mathf.lerpDelta(warmup, unit.isShooting() ? 1f : 0f, 0.1f);
        unit.healthMultiplier += warmup * healthMultiplier;
    }

    @Override
    public void draw(Unit unit){
        if(warmup > 0.001f){
            if(plateRegion == null){
                plateRegion = Core.atlas.find(unit.type.name + "-armor", unit.type.region);
            }

            Draw.draw(z <= 0 ? Draw.z() : z, () -> {
                Shaders.armor.region = plateRegion;
                Shaders.armor.progress = warmup;
                Shaders.armor.time = -Time.time / 20f;

                Draw.color(color);
                Draw.shader(Shaders.armor);
                Draw.rect(Shaders.armor.region, unit.x, unit.y, unit.rotation - 90f);
                Draw.shader();

                Draw.reset();
            });
        }
    }
}
