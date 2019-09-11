package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.Platform.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.platform;

public class BrowseMapsDialog extends FloatingDialog{

    public BrowseMapsDialog(){
        super("$maps.browse");
        shown(this::setup);
    }

    void setup(){
        cont.clear();

        cont.addImage(Icon.refresh);
        platform.findMaps("", list -> {
            Table maps = new Table();
            maps.marginRight(24);

            ScrollPane pane = new ScrollPane(maps);
            pane.setFadeScrollBars(false);

            int maxwidth = Mathf.clamp((int)(Core.graphics.getWidth() / Scl.scl(230)), 1, 8);
            float mapsize = 200f;

            int i = 0;
            for(PostedMap map : list){

                if(i % maxwidth == 0){
                    maps.row();
                }

                TextButton button = maps.addButton("", Styles.cleart, map::openPage).width(mapsize).pad(8).get();
                button.clearChildren();
                button.margin(9);
                button.add(map.name()).width(mapsize - 18f).center().get().setEllipsis(true);
                button.row();
                button.addImage().growX().pad(4).color(Pal.gray);
                button.row();
                Stack stack = button.stack(new Image(Icon.refresh)).size(mapsize - 20f).get();
                map.preview(file -> {
                    Core.assets.load(new AssetDescriptor<>(file, Texture.class)).loaded = ct -> {
                        Texture tex = (Texture)ct;
                        stack.clearChildren();
                        stack.add(new Image(tex).setScaling(Scaling.fit));
                        stack.add(new BorderImage(tex).setScaling(Scaling.fit));
                    };
                });

                i++;
            }

            if(Vars.maps.all().size == 0){
                maps.add("$maps.none");
            }

            cont.add(buttons).growX();
            cont.row();
            cont.add(pane).uniformX();
        });
    }
}
