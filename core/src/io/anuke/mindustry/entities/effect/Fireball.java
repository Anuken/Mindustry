package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.effectGroup;
import static io.anuke.mindustry.Vars.world;

public class Fireball extends TimedEntity {
    private float rotation;
    private float speed;
    private Color color;

    public Fireball(float x, float y, Color color, float rotation){
        set(x, y);
        this.rotation = rotation;
        this.color = color;
        lifetime = 30f + Mathf.random(40f);
        speed = 0.6f + Mathf.random(2f);
    }

    @Override
    public void update() {
        super.update();

        float speed = this.speed * fout();
        x += Angles.trnsx(rotation, speed);
        y += Angles.trnsy(rotation, speed);

        if(Mathf.chance(0.04 * Timers.delta())){
            Tile tile = world.tileWorld(x, y);
            if(tile != null){
                new Fire(tile).add();
            }
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.fireballsmoke, x, y);
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.ballfire, x, y);
        }
    }

    @Override
    public void draw() {
        Draw.color(Palette.lightFlame, color, Color.GRAY, fin());
        Fill.circle(x, y, 3f * fout());
        Draw.reset();
    }

    @Override
    public Fireball add(){
        super.update();
        return add(effectGroup);
    }
}
