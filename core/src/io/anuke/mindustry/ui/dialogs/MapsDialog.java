package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapMeta;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;

import static io.anuke.mindustry.Vars.*;

public class MapsDialog extends FloatingDialog{
    private FloatingDialog dialog;

    public MapsDialog(){
        super("$text.maps");

        addCloseButton();
        buttons().addImageTextButton("$text.editor.importmap", "icon-add", 14 * 2, () -> {
            Platform.instance.showFileChooser("$text.editor.importmap", "Map File", file -> {
                try{
                    DataInputStream stream = new DataInputStream(file.read());
                    MapMeta meta = MapIO.readMapMeta(stream);
                    MapTileData data = MapIO.readTileData(stream, meta, true);
                    stream.close();

                    String name = meta.tags.get("name", file.nameWithoutExtension());

                    if(world.maps.getByName(name) != null && !world.maps.getByName(name).custom){
                        ui.showError(Bundles.format("text.editor.import.exists", name));
                    }else if(world.maps.getByName(name) != null){
                        ui.showConfirm("$text.confirm", "$text.editor.overwrite.confirm", () -> {
                            world.maps.saveMap(name, data, meta.tags);
                            setup();
                        });
                    }else{
                        world.maps.saveMap(name, data, meta.tags);
                        setup();
                    }

                }catch(Exception e){
                    ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
                    Log.err(e);
                }
            }, true, mapExtension);
        }).size(230f, 64f);

        shown(this::setup);
        onResize(() -> {
            if(dialog != null){
                dialog.hide();
            }
        });
    }

    void setup(){
        content().clear();

        Table maps = new Table();
        maps.marginRight(24);

        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = 4;
        float mapsize = 200f;

        int i = 0;
        for(Map map : world.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            TextButton button = maps.addButton("", "clear", () -> showMapInfo(map)).width(mapsize).pad(8).get();
            button.clearChildren();
            button.margin(9);
            button.add(map.meta.tags.get("name", map.name)).growX().center().get().setEllipsis(true);
            button.row();
            button.addImage("white").growX().pad(4).color(Color.GRAY);
            button.row();
            button.stack(new Image(map.texture).setScaling(Scaling.fit), new BorderImage(map.texture).setScaling(Scaling.fit)).size(mapsize - 20f);
            button.row();
            button.add(map.custom ? "$text.custom" : "$text.builtin").color(Color.GRAY).padTop(3);

            i++;
        }

        if(world.maps.all().size == 0){
            maps.add("$text.maps.none");
        }

        content().add(pane).uniformX();
    }

    void showMapInfo(Map map){
        dialog = new FloatingDialog("$text.editor.mapinfo");
        dialog.addCloseButton();

        float mapsize = UIUtils.portrait() ? 160f : 300f;
        Table table = dialog.content();

        table.stack(new Image(map.texture).setScaling(Scaling.fit), new BorderImage(map.texture).setScaling(Scaling.fit)).size(mapsize);

        table.table("clear", desc -> {
            desc.top();
            Table t = new Table();
            t.margin(6);

            ScrollPane pane = new ScrollPane(t);
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
        }).height(mapsize).width(mapsize);

        table.row();

        table.addImageTextButton("$text.editor.openin", "icon-load-map", 16 * 2, () -> {
            try{
                Vars.ui.editor.beginEditMap(map.stream.get());
                dialog.hide();
                hide();
            }catch(Exception e){
                e.printStackTrace();
                ui.showError("$text.error.mapnotfound");
            }
        }).fillX().height(54f).marginLeft(10);

        table.addImageTextButton("$text.delete", "icon-trash-16", 16 * 2, () -> {
            ui.showConfirm("$text.confirm", Bundles.format("text.map.delete", map.name), () -> {
                world.maps.removeMap(map);
                dialog.hide();
                setup();
            });
        }).fillX().height(54f).marginLeft(10).disabled(!map.custom).touchable(map.custom ? Touchable.enabled : Touchable.disabled);

        dialog.show();
    }
}
