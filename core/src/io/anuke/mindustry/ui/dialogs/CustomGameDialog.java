package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import static io.anuke.mindustry.Vars.world;

public class
CustomGameDialog extends FloatingDialog{
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
        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = (Core.graphics.isPortrait() ? 2 : 4);
        float images = 146f;

        int i = 0;
        maps.defaults().width(170).fillY().top().pad(4f);
        for(Map map : world.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.texture), "clear");
            image.margin(5);
            image.getImageCell().size(images);
            image.top();
            image.row();
            image.add("[accent]" + map.name()).pad(3f).growX().wrap().get().setAlignment(Align.center, Align.center);
            image.row();
            image.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f);

            BorderImage border = new BorderImage(map.texture, 3f);
            border.setScaling(Scaling.fit);
            image.replaceImage(border);

            image.clicked(() -> dialog.show(map));

            maps.add(image);

            i++;
        }

        if(world.maps.all().size == 0){
            maps.add("$maps.none").pad(50);
        }

        cont.add(pane).uniformX();
    }
}