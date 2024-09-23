package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class GlobalVarsDialog extends BaseDialog{

    public GlobalVarsDialog(){
        super("@logic.globals");

        addCloseButton();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        float prefWidth = Math.min(Core.graphics.getWidth() * 0.9f / Scl.scl(1f) - 240f, 600f);
        cont.clearChildren();

        cont.pane(t -> {
            t.margin(10f).marginRight(16f);
            t.defaults().fillX().fillY();
            for(var entry : logicVars.getEntries()){
                if(entry.name.startsWith("section")){
                    Color color = Pal.accent;
                    t.add("@lglobal." + entry.name).fillX().center().labelAlign(Align.center).colspan(4).color(color).padTop(4f).padBottom(2f).row();
                    t.image(Tex.whiteui).height(4f).color(color).colspan(4).padBottom(8f).row();
                }else{
                    Color varColor = Pal.gray;
                    float stub = 8f, mul = 0.5f, pad = 4;

                    String desc = entry.description;
                    if(desc == null || desc.isEmpty()){
                        desc = Core.bundle.get("lglobal." + entry.name, "");
                    }

                    String fdesc = desc;

                    t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                    t.stack(new Image(Tex.whiteui, varColor), new Label(" " + entry.name + " ", Styles.outlineLabel)).padRight(pad);

                    t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                    t.table(Tex.pane, out -> out.add(fdesc).style(Styles.outlineLabel).width(prefWidth).padLeft(2).padRight(2).wrap()).padRight(pad);

                    t.row();

                    t.add().fillX().colspan(4).height(4).row();
                }
            }
        }).grow();
    }
}
