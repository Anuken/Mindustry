package mindustry.entities.effect;

import arc.Core;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import arc.graphics.Color;
import arc.graphics.g2d.Fill;

import static arc.graphics.g2d.Draw.*;
import static arc.math.Angles.trnsx;
import static arc.math.Angles.trnsy;

public class CasingEffect extends Effect {
    public float
            addlen = 2f, mullen = 10f,
            width = 2f, height = 3f;
    public boolean
            doubled = false,
            drawFill = false;
    public Color[] colors = {};
    public TextrueRegion casing;

    public CasingEffect() {
        layer(Layer.bullet);
        lifetime = 34f;
    }

    public CasingEffect(float life, float addlen, float mullen) {
        this();
        lifetime = life;
        this.addlen = addlen;
        this.mullen = mullen;
    }

    public CasingEffect(float life, float addlen, float mullen, float w, float h) {
        this();
        lifetime = life;
        this.addlen = addlen;
        this.mullen = mullen;
        this.width = w;
        this.height = h;
    }

    public CasingEffect(float life, float addlen, float mullen, float w, float h, boolean doubled) {
        this();
        lifetime = life;
        this.addlen = addlen;
        this.mullen = mullen;
        this.width = w;
        this.height = h;
        this.doubled = doubled;
    }

    public CasingEffect(float life, float addlen, float mullen, boolean doubled) {
        this();
        lifetime = life;
        this.addlen = addlen;
        this.mullen = mullen;
        this.doubled = doubled;
    }

    @Override
    public void init() {
        if (casing == null) casing = Core.atlas.find("casing");
    }

    @Override
    public void render(EffectContainer e) {
        if(colors.length < 3){
            color(Pal.lightOrange, Pal.lightishGray, Pal.lightishGray, e.fin());
        }else{
            color(colors[0], colors[1], colors[2], e.fin());
        }
        alpha(e.fout(0.5f));
        float rot = Math.abs(e.rotation) + 90f;
        if(!doubled){
            int i = -Mathf.sign(e.rotation);
            draw(e, rot, i);
        }else{
            for(int i : Mathf.signs){
                draw(e, rot, i);
            }
        }
    }

    protected void draw(EffectContainer e, float rot, int i) {
        float len = (addlen + e.finpow() * mullen) * i;
        float lr = rot + Mathf.randomSeedRange(e.id + i + 6, 20f * e.fin()) * i;

        if(drawFill){
            Fill.rect(
                    e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                    e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                    width, height, rot + e.fin() * 50f * i
            );
        }else{
            rect(casing,
                    e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                    e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                    width, height, rot + e.fin() * 50f * i
            );
        }
    }
}
