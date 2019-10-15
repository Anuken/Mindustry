package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.*;

public class CustomGameDialog extends FloatingDialog{
    private MapPlayDialog dialog = new MapPlayDialog();

    public CustomGameDialog(){
        super("$customgame");
        addCloseButton();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        clearChildren();
        add(titleTable);
        row();
        stack(cont, buttons).grow();
        buttons.bottom();
        cont.clear();

        Table maps = new Table();
        maps.marginRight(14);
        maps.marginBottom(55f);
        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = Mathf.clamp((int)(Core.graphics.getWidth() / Scl.scl(200)), 1, 8);
        float images = 146f;

        int i = 0;
        maps.defaults().width(170).fillY().top().pad(4f);
        for(Map map : Vars.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.safeTexture()), Styles.cleari);
            image.margin(5);
            image.top();

            Image img = image.getImage();
            img.remove();

            image.row();
            image.table(t -> {
                t.left();
                for(Gamemode mode : Gamemode.all){
                    if(mode.valid(map) && Core.atlas.has("icon-mode-" + mode.name())){
                        t.addImage(Core.atlas.drawable("icon-mode-" + mode.name())).size(16f).pad(4f);
                    }
                }
            }).left();
            image.row();
            image.add(map.name()).pad(1f).growX().wrap().left().get().setEllipsis(true);
            image.row();
            image.addImage(Tex.whiteui, Pal.gray).growX().pad(3).height(4f);
            image.row();
            image.add(img).size(images);


            BorderImage border = new BorderImage(map.safeTexture(), 3f);
            border.setScaling(Scaling.fit);
            image.replaceImage(border);

            image.clicked(() -> dialog.show(map));

            maps.add(image);

            i++;
        }

        if(Vars.maps.all().size == 0){
            maps.add("$maps.none").pad(50);
        }

        cont.add(pane).uniformX();
    }
}