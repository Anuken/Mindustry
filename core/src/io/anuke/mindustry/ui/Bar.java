package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.function.FloatProvider;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.style.Drawable;
import io.anuke.arc.util.pooling.Pools;

public class Bar extends Element{
    private static Rectangle scissor = new Rectangle();

    private FloatProvider fraction;
    private String name = "";
    private float value, lastValue, blink;
    private Color blinkColor = new Color();

    public Bar(String name, Color color, FloatProvider fraction){
        this.fraction = fraction;
        this.name = Core.bundle.get(name);
        this.blinkColor.set(color);
        lastValue = value = fraction.get();
        setColor(color);
    }

    public Bar(Supplier<String> name, Supplier<Color> color, FloatProvider fraction){
        this.fraction = fraction;
        lastValue = value = Mathf.clamp(fraction.get());
        update(() -> {
            this.name = name.get();
            this.blinkColor.set(color.get());
            setColor(color.get());
        });
    }

    public Bar blink(Color color){
        blinkColor.set(color);
        return this;
    }

    @Override
    public void draw(){
        float computed = Mathf.clamp(fraction.get());
        if(!Mathf.isEqual(lastValue, computed)){
            blink = 1f;
            lastValue = computed;
        }

        blink = Mathf.lerpDelta(blink, 0f, 0.2f);
        value = Mathf.lerpDelta(value, computed, 0.15f);

        Draw.colorl(0.1f);
        Draw.drawable("bar", x, y, width, height);
        Draw.color(color, blinkColor, blink);

        Drawable top = Core.scene.skin.getDrawable("bar-top");
        float topWidth = width * value;

        if(topWidth > Core.atlas.find("bar-top").getWidth()){
            top.draw(x, y, topWidth, height);
        }else{
            if(ScissorStack.pushScissors(scissor.set(x, y, topWidth, height))){
                top.draw(x, y, Core.atlas.find("bar-top").getWidth(), height);
                ScissorStack.popScissors();
            }
        }

        Draw.color();

        BitmapFont font = Core.scene.skin.getFont("default-font");
        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        lay.setText(font, name);

        font.setColor(Color.WHITE);
        font.draw(name, x + width / 2f - lay.width / 2f, y + height / 2f + lay.height / 2f + 1);

        Pools.free(lay);
    }
}
