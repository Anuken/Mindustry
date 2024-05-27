package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class IconSelectDialog extends Dialog{
    private Intc consumer = i -> Log.info("you have mere seconds");

    public IconSelectDialog(){
        closeOnBack();
        setFillParent(true);

        cont.pane(t -> {
            resized(true, () -> {
                t.clearChildren();
                t.marginRight(19f);
                t.defaults().size(48f);

                t.button(Icon.none, Styles.flati, () -> {
                    hide();
                    consumer.get(0);
                });

                int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                int i = 1;
                for(var key : accessibleIcons){
                    var value = Icon.icons.get(key);

                    t.button(value, Styles.flati, () -> {
                        hide();
                        consumer.get(Iconc.codes.get(key));
                    });

                    if(++i % cols == 0) t.row();
                }

                for(ContentType ctype : defaultContentIcons){
                    t.row();
                    t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                    t.row();

                    i = 0;
                    for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                        if(!u.isHidden() && u.unlocked()){
                            t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                hide();
                                consumer.get(u.emojiChar());
                            });

                            if(++i % cols == 0) t.row();
                        }
                    }
                }
            });
        });
        buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
    }

    public void show(Intc listener){
        consumer = listener;
        super.show();
    }
}
