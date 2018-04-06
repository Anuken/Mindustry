package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Lightning extends Entity {
    private Array<Vector2> lines = new Array<Vector2>();

    public Lightning(Team team, SolidEntity damager, float x, float y, float angle, int length){
        float step = 3f;

        for(int i = 0; i < length; i ++){
            lines.add(new Vector2(x, y));

            float x2 = x + Angles.trnsx(angle, step);
            float y2 = y + Angles.trnsy(angle, step);
            angle += Mathf.range(30f);

            if(Mathf.chance(0.1)){
                new Lightning(team, damager, x2, y2, angle + Mathf.range(100f), length/2).add();
            }

            x = x2;
            y = y2;
        }

        lines.add(new Vector2(x, y));
    }
}
