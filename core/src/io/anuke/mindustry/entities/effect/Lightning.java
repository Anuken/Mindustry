package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Lightning extends TimedEntity {
    private static Array<SolidEntity> entities = new Array<>();
    private static Rectangle rect = new Rectangle();
    private static float wetDamageMultiplier = 2;

    private Array<Vector2> lines = new Array<>();
    private float angle;

    public Lightning(Team team, Effect effect, int damage, float x, float y, float targetAngle, int length){
        this.x = x;
        this.y = y;
        this.lifetime = 10f;
        this.angle = targetAngle;

        float step = 3f;
        float range = 6f;
        float attractRange = 20f;

        entities.clear();

        Units.getNearbyEnemies(team, rect, entities::add);

        for(int i = 0; i < length; i ++){
            lines.add(new Vector2(x, y));

            float fx = x, fy = y;
            float x2 = x + Angles.trnsx(angle, step);
            float y2 = y + Angles.trnsy(angle, step);
            float fangle = angle;
            angle += Mathf.range(30f);

            rect.setSize(attractRange).setCenter(x, y);

            Units.getNearbyEnemies(team, rect, entity -> {
                float dst = entity.distanceTo(x2, y2);
                if(dst < attractRange) {
                    angle = Mathf.slerp(angle, Angles.angle(x2, y2, entity.x, entity.y), (attractRange - dst) / attractRange / 4f);
                }

                Rectangle hitbox = entity.hitbox.getRect(entity.x, entity.y, range);

                if(hitbox.contains(x2, y2) || hitbox.contains(fx, fy)){
                    int result = damage;

                    if(entity.status.current() == StatusEffects.wet)
                        result = (int)(result * wetDamageMultiplier);

                    entity.damage(result);
                    Effects.effect(effect, x2, y2, fangle);
                }
            });

            if(Mathf.chance(0.1)){
                new Lightning(team, effect, damage, x2, y2, angle + Mathf.range(100f), length/3).add();
            }

            x = x2;
            y = y2;
        }

        lines.add(new Vector2(x, y));
    }

    @Override
    public void draw() {
        float lx = x, ly = y;
        Draw.color(Palette.lancerLaser, Color.WHITE, fin());
        for(int i = 0; i < lines.size; i ++){
            Vector2 v = lines.get(i);
            Lines.stroke(fout() * 3f + 1f-(float)i/lines.size);
            Lines.line(lx, ly, v.x, v.y);
            lx = v.x;
            ly = v.y;
        }
        Draw.color();
    }
}
