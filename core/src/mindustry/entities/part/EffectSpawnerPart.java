package mindustry.entities.part;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.math.Mathf.random;
import static arc.util.Tmp.*;

/**Spawns effects in a rectangle centered on x and y.*/
public class EffectSpawnerPart extends DrawPart{
    public float x, y, width, height, rotation;
    public boolean mirror = false;

    public float effectChance = 0.1f, effectRot, effectRandRot;
    public Effect effect = Fx.sparkShoot;
    public Color effectColor = Color.white;

    public boolean useProgress = true;
    public PartProgress progress = PartProgress.warmup;

    /**Shows the spawn rectangles in red.*/
    public boolean debugDraw = false;

    @Override
    public void draw(PartParams params){
        if(debugDraw){
            for(int i = 0; i < (mirror ? 2 : 1); i++){
                float sign = (i == 0 ? 1f : -1f), rot = params.rotation + (rotation * sign);
                v1.set(x * sign, y).rotate(params.rotation - 90).add(params.x, params.y);

                float z = Draw.z();
                Draw.z(Layer.buildBeam);
                Draw.color(Color.red);
                Draw.rect("error", v1.x, v1.y, width, height, rot - 90f);
                Draw.color();
                Draw.z(z);
            }
        }

        if(Vars.state.isPaused()) return;

        for(int i = 0; i < (mirror ? 2 : 1); i++){
            if(!Vars.state.isPaused() && Mathf.chanceDelta(effectChance * (useProgress ? progress.getClamp(params) : 1f))){
                float sign = (i == 0 ? 1f : -1f), rot = params.rotation + (rotation * sign);
                v1.set(x * sign, y).rotate(params.rotation - 90).add(params.x, params.y);
                v1.add(v2.set(random(-height * 0.5f, height * 0.5f), random(-width * 0.5f, width * 0.5f)).rotate(rot));

                effect.at(v1.x, v1.y, rot + (effectRot * sign) + random(-effectRandRot, effectRandRot), effectColor);
            }
        }
    }
}