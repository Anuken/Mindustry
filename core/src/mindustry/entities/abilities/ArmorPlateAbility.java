package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ArmorPlateAbility extends Ability{
    public TextureRegion plateRegion;
    public TextureRegion shineRegion;
    public String plateSuffix = "-armor";
    public String shineSuffix = "-shine";
    /** Color of the shine. If null, uses team color. */
    public @Nullable Color color = null;
    public float shineSpeed = 1f;
    public float z = -1;

    /** Whether to draw the plate region. */
    public boolean drawPlate = true;
    /** Whether to draw the shine over the plate region. */
    public boolean drawShine = true;

    public float healthMultiplier = 0.2f;

    protected float warmup;

    @Override
    public void update(Unit unit){
        super.update(unit);

        warmup = Mathf.lerpDelta(warmup, unit.isShooting() ? 1f : 0f, 0.1f);
        unit.healthMultiplier += warmup * healthMultiplier;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("damagereduction", Strings.autoFixed(-healthMultiplier * 100f, 1)));
    }

    @Override
    public void draw(Unit unit){
        if(!drawPlate && !drawShine) return;

        if(warmup > 0.001f){
            if(plateRegion == null){
                plateRegion = Core.atlas.find(unit.type.name + plateSuffix, unit.type.region);
                shineRegion = Core.atlas.find(unit.type.name + shineSuffix, plateRegion);
            }

            float pz = Draw.z();
            if(z > 0) Draw.z(z);

            if(drawPlate){
                Draw.alpha(warmup);
                Draw.rect(plateRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.alpha(1f);
            }

            if(drawShine){
                Draw.draw(Draw.z(), () -> {
                    Shaders.armor.region = shineRegion;
                    Shaders.armor.progress = warmup;
                    Shaders.armor.time = -Time.time / 20f * shineSpeed;

                    Draw.color(color == null ? unit.team.color : color);
                    Draw.shader(Shaders.armor);
                    Draw.rect(shineRegion, unit.x, unit.y, unit.rotation - 90f);
                    Draw.shader();

                    Draw.reset();
                });
            }

            Draw.z(pz);
        }
    }
}
