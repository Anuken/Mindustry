package mindustry.ui;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** An ItemDisplay, but for liquids. */
public class LiquidDisplay extends Table{
    public final Liquid liquid;
    public final float amount;
    public final boolean perSecond;

    public LiquidDisplay(Liquid liquid, float size, float amount, boolean perSecond){
        this.liquid = liquid;
        this.amount = amount;
        this.perSecond = perSecond;

        left();
        add(new Stack(){{
            Image i = new Image(liquid.uiIcon).setScaling(Scaling.fit);
            i.setAlign(Align.left);
            add(i);

            if(!perSecond && amount != 0){
                Table t = new Table().left().bottom();
                t.add(Strings.autoFixed(amount, 2)).style(Styles.outlineLabel);
                add(t);
            }
        }}).height(size).minWidth(size).left();
        add(liquid.localizedName + (perSecond && amount != 0 ? "\n[lightgray]" + Strings.autoFixed(amount, 2) + StatUnit.perSecond.localized() : "[]")).left().style(Styles.outlineLabel);
    }

    public LiquidDisplay(Liquid liquid, float amount, boolean perSecond){
        this(liquid, iconMed, amount, perSecond);
    }
}
