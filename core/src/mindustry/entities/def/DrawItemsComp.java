package mindustry.entities.def;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.itemSize;

@Component
abstract class DrawItemsComp implements Drawc, Itemsc, Posc, Rotc{
    transient float x, y, rotation;

    float itemTime;

    //drawn after base
    @Override
    @MethodPriority(3)
    public void draw(){
        boolean number = isLocal();
        itemTime = Mathf.lerpDelta(itemTime, Mathf.num(hasItem()), 0.05f);

        //draw back items
        if(itemTime > 0.01f){
            float backTrns = 5f;
            float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * itemTime;

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
            Draw.rect(item().icon(Cicon.medium),
            x + Angles.trnsx(rotation + 180f, backTrns),
            y + Angles.trnsy(rotation + 180f, backTrns),
            size, size, rotation);

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
            x + Angles.trnsx(rotation + 180f, backTrns),
            y + Angles.trnsy(rotation + 180f, backTrns),
            (3f + Mathf.absin(Time.time(), 5f, 1f)) * itemTime);

            if(isLocal()){
                Fonts.outline.draw(stack().amount + "",
                x + Angles.trnsx(rotation + 180f, backTrns),
                y + Angles.trnsy(rotation + 180f, backTrns) - 3,
                Pal.accent, 0.25f * itemTime / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }
}
