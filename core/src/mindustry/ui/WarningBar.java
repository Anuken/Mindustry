package mindustry.ui;

import arc.graphics.g2d.*;
import arc.scene.*;
import mindustry.graphics.*;

public class WarningBar extends Element{
    public float barWidth = 40f, spacing = barWidth*2, skew = barWidth;

    {
        setColor(Pal.accent);
    }

    @Override
    public void draw(){
        Draw.color(color);
        Draw.alpha(parentAlpha);

        int amount = (int)(width / spacing) + 2;

        for(int i = 0; i < amount; i++){
            float rx = x + (i - 1)*spacing;
            Fill.quad(
            rx, y,
            rx + skew, y + height,
            rx + skew + barWidth, y + height,
            rx + barWidth, y
            );
        }
        Lines.stroke(3f);
        Lines.line(x, y, x + width, y);
        Lines.line(x, y + height, x + width, y + height);

        Draw.reset();
    }
}
