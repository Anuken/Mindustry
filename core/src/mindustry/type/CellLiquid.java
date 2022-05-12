package mindustry.type;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.entities.Puddles.*;

public class CellLiquid extends Liquid{
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public int cells = 8;

    public CellLiquid(String name, Color color){
        super(name, color);
    }

    public CellLiquid(String name){
        super(name);
    }

    @Override
    public void drawPuddle(Puddle puddle){
        super.drawPuddle(puddle);

        Draw.z(Layer.debris - 0.5f);

        int id = puddle.id;
        float amount = puddle.amount, x = puddle.x, y = puddle.y;
        float f = Mathf.clamp(amount / (maxLiquid / 1.5f));
        float smag = puddle.tile.floor().isLiquid ? 0.8f : 0f, sscl = 25f;
        float length = Math.max(f, 0.3f) * 9f;

        rand.setSeed(id);
        for(int i = 0; i < cells; i++){
            Draw.z(Layer.debris - 0.5f + i/1000f + (id % 100) / 10000f);
            Tmp.v1.trns(rand.random(360f), rand.random(length));
            float vx = x + Tmp.v1.x, vy = y + Tmp.v1.y;

            Draw.color(colorFrom, colorTo, rand.random(1f));

            Fill.circle(
            vx + Mathf.sin(Time.time + i * 532, sscl, smag),
            vy + Mathf.sin(Time.time + i * 53, sscl, smag),
            f * 3.8f * rand.random(0.2f, 1f) * Mathf.absin(Time.time + ((i + id) % 60) * 54, 75f * rand.random(1f, 2f), 1f));
        }

        Draw.color();
    }
}
