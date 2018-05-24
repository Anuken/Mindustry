package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.Map;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.ui;
import static io.anuke.mindustry.Vars.world;

public class MapsDialog extends FloatingDialog {

    public MapsDialog() {
        super("$text.maps");
        addCloseButton();
        shown(this::setup);
    }

    void setup(){
        content().clear();

        Table maps = new Table();
        maps.marginRight(24);

        ScrollPane pane = new ScrollPane(maps, "clear-black");
        pane.setFadeScrollBars(false);

        int maxwidth = 4;
        float mapsize = 200f;

        int i = 0;
        for(Map map : world.maps().all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            TextButton button = maps.addButton("", "clear", () -> showMapInfo(map)).width(mapsize).pad(8).get();
            button.clearChildren();
            button.margin(6);
            button.add(map.meta.tags.get("name", map.name)).growX().center().get().setEllipsis(true);
            button.row();
            button.addImage("white").growX().pad(4).color(Color.GRAY);
            button.row();
            ((Image)button.stack(new Image(map.texture), new BorderImage(map.texture)).size(mapsize-20f).get().getChildren().first()).setScaling(Scaling.fit);
            button.row();
            button.add(map.custom ? "$text.custom" : "$text.builtin").color(Color.GRAY).padTop(3);

            i ++;
        }

        content().add(pane).uniformX();
    }

    void showMapInfo(Map map){
        FloatingDialog dialog = new FloatingDialog("$text.editor.mapinfo");
        dialog.addCloseButton();

        float mapsize = 300f;
        Table table = dialog.content();

        ((Image) table.stack(new Image(map.texture), new BorderImage(map.texture)).size(mapsize).get().getChildren().first()).setScaling(Scaling.fit);

        table.table("clear", desc -> {
            desc.top();
            Table t = new Table();

            ScrollPane pane = new ScrollPane(t, "clear-black");
            desc.add(pane).grow();

            t.top();
            t.defaults().padTop(10).left();

            t.add("$text.editor.name").padRight(10).color(Color.GRAY).padTop(0);
            t.row();
            t.add(map.meta.tags.get("name", map.name)).growX().wrap().padTop(2);
            t.row();
            t.add("$text.editor.author").padRight(10).color(Color.GRAY);
            t.row();
            t.add(map.meta.author()).growX().wrap().padTop(2);
            t.row();
            t.add("$text.editor.description").padRight(10).color(Color.GRAY).top();
            t.row();
            t.add(map.meta.description()).growX().wrap().padTop(2);
            t.row();
            t.add("$text.editor.oregen.info").padRight(10).color(Color.GRAY);
            t.row();
            t.add(map.meta.hasOreGen() ? "$text.on" : "$text.off").padTop(2);
        }).height(mapsize).width(mapsize).margin(6);

        table.row();

        table.addImageTextButton("$text.editor.openin", "icon-load-map", "clear", 16*2, () -> {
            Vars.ui.editor.beginEditMap(map.stream.get());
            dialog.hide();
            hide();
        }).fillX().height(50f).marginLeft(6);

        table.addImageTextButton("$text.delete", "icon-trash-16", "clear", 16*2, () -> {
            ui.showConfirm("$text.confirm", Bundles.format("text.map.delete", map.name), () -> {
                world.maps().removeMap(map);
                dialog.hide();
                setup();
            });
        }).fillX().height(50f).marginLeft(6).disabled(!map.custom).touchable(map.custom ? Touchable.enabled : Touchable.disabled);

        dialog.show();
    }
}
