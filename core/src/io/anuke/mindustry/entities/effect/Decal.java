package io.anuke.mindustry.entities.effect;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.entities.traits.BelowLiquidTrait;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.impl.TimedEntity;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;

import static io.anuke.mindustry.Vars.groundEffectGroup;

/**
 * Class for creating block rubble on the ground.
 */
public abstract class Decal extends TimedEntity implements BelowLiquidTrait, DrawTrait{
    private static final Color color = Color.valueOf("3a3635");

    @Override
    public float lifetime(){
        return 8200f;
    }

    @Override
    public void draw(){
        Draw.color(color.r, color.g, color.b, 1f - Mathf.curve(fin(), 0.98f));
        drawDecal();
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup(){
        return groundEffectGroup;
    }

    abstract void drawDecal();
}
