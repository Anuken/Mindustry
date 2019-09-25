package io.anuke.mindustry.entities.effect;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.type.TimedEntity;
import io.anuke.mindustry.entities.traits.BelowLiquidTrait;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.mindustry.graphics.Pal;

import static io.anuke.mindustry.Vars.groundEffectGroup;

/**
 * Class for creating block rubble on the ground.
 */
public abstract class Decal extends TimedEntity implements BelowLiquidTrait, DrawTrait{

    @Override
    public float lifetime(){
        return 3600;
    }

    @Override
    public void draw(){
        Draw.color(Pal.rubble.r, Pal.rubble.g, Pal.rubble.b, 1f - Mathf.curve(fin(), 0.98f));
        drawDecal();
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup(){
        return groundEffectGroup;
    }

    abstract void drawDecal();
}
