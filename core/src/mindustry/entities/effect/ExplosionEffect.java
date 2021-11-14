package mindustry.entities.effect;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

public class ExplosionEffect extends Effect{
    public Color waveColor = Pal.missileYellow, smokeColor = Color.gray, sparkColor = Pal.missileYellowBack;
    public float waveLife = 6f, waveStroke = 3f, waveRad = 15f, waveRadBase = 2f, sparkStroke = 1f, sparkRad = 23f, sparkLen = 3f, smokeSize = 4f, smokeSizeBase = 0.5f, smokeRad = 23f;
    public int smokes = 5, sparks = 4;

    public ExplosionEffect(){
        clip = 100f;
        lifetime = 22;

        renderer = e -> {
            color(waveColor);

            e.scaled(waveLife, i -> {
                stroke(waveStroke * i.fout());
                Lines.circle(e.x, e.y, waveRadBase + i.fin() * waveRad);
            });

            color(smokeColor);

            if(smokeSize > 0){
                randLenVectors(e.id, smokes, 2f + smokeRad * e.finpow(), (x, y) -> {
                    Fill.circle(e.x + x, e.y + y, e.fout() * smokeSize + smokeSizeBase);
                });
            }

            color(sparkColor);
            stroke(e.fout() * sparkStroke);

            randLenVectors(e.id + 1, sparks, 1f + sparkRad * e.finpow(), (x, y) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * sparkLen);
                Drawf.light(e.x + x, e.y + y, e.fout() * sparkLen * 4f, sparkColor, 0.7f);
            });
        };
    }
}
