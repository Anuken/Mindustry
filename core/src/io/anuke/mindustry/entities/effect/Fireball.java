package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.effectGroup;

public class Fireball extends TimedEntity {
    private float rotation;
    private float speed = 0.3f;
    private Color color;

    public Fireball(float x, float y, Color color, float rotation){
        set(x, y);
        this.rotation = rotation;
        this.color = color;
        lifetime = 70f;
        speed += Mathf.random(1f);
    }

    @Override
    public void update() {
        super.update();

        float speed = this.speed - fin()*0.1f;
        x += Angles.trnsx(rotation, speed);
        y += Angles.trnsy(rotation, speed);
    }

    @Override
    public void draw() {
        Draw.color(Palette.lightFlame, color, Color.GRAY, fin());
        Fill.circle(x, y, 3f * fout() + 0.5f);
        Draw.reset();
    }

    @Override
    public Fireball add(){
        super.update();
        return add(effectGroup);
    }
}
