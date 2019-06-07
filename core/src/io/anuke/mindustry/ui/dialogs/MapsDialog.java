package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import static io.anuke.mindustry.Vars.*;

public class MapsDialog extends FloatingDialog{
    private FloatingDialog dialog;

    public MapsDialog(){
        super("$maps");

        addCloseButton();
        buttons.addImageTextButton("$editor.importmap", "icon-add", 14 * 2, () -> {
            Platform.instance.showFileChooser("$editor.importmap", "Map File", file -> {
                world.maps.tryCatchMapError(() -> {
                    if(MapIO.isImage(file)){
                        ui.showError("$editor.errorimage");
                        return;
                    }

                    Map map;
                    if(file.extension().equalsIgnoreCase(mapExtension)){
                        map = MapIO.createMap(file, true);
                    }else{
                        map = world.maps.makeLegacyMap(file);
                    }

                    String name = map.tags.get("name");
                    if(name == null){
                        ui.showError("$editor.errorname");
                        return;
                    }

                    Map conflict = world.maps.all().find(m -> m.name().equals(name));

                    if(conflict != null && !conflict.custom){
                        ui.showInfo(Core.bundle.format("editor.import.exists", name));
                    }else if(conflict != null){
                        ui.showConfirm("$confirm", "$editor.overwrite.confirm", () -> {
                            world.maps.tryCatchMapError(() -> {
                                world.maps.importMap(file);
                                setup();
                            });
                        });
                    }else{
                        world.maps.importMap(map.file);
                        setup();
                    }

                });
            }, true, FileChooser.anyMapFiles);
        }).size(230f, 64f);

        shown(this::setup);
        onResize(() -> {
            if(dialog != null){
                dialog.hide();
            }
        });
    }

    void setup(){
        cont.clear();

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
            button.add(map.name()).width(mapsize - 18f).center().get().setEllipsis(true);
            button.row();
            button.addImage("white").growX().pad(4).color(Color.GRAY);
            button.row();
            button.stack(new Image(map.texture).setScaling(Scaling.fit), new BorderImage(map.texture).setScaling(Scaling.fit)).size(mapsize - 20f);
            button.row();
            button.add(map.custom ? "$custom" : "$builtin").color(Color.GRAY).padTop(3);

            i++;
        }

        if(world.maps.all().size == 0){
            maps.add("$maps.none");
        }

        cont.add(pane).uniformX();
    }

    void showMapInfo(Map map){
        dialog = new FloatingDialog("$editor.mapinfo");
        dialog.addCloseButton();

        float mapsize = Core.graphics.isPortrait() ? 160f : 300f;
        Table table = dialog.cont;

        table.stack(new Image(map.texture).setScaling(Scaling.fit), new BorderImage(map.texture).setScaling(Scaling.fit)).size(mapsize);

        table.table("flat", desc -> {
            desc.top();
            Table t = new Table();
            t.margin(6);

            ScrollPane pane = new ScrollPane(t);
            desc.add(pane).grow();

            t.top();
            t.defaults().padTop(10).left();

            t.add("$editor.name").padRight(10).color(Color.GRAY).padTop(0);
            t.row();
            t.add(map.name()).growX().wrap().padTop(2);
            t.row();
            t.add("$editor.author").padRight(10).color(Color.GRAY);
            t.row();
            t.add(map.author()).growX().wrap().padTop(2);
            t.row();
            t.add("$editor.description").padRight(10).color(Color.GRAY).top();
            t.row();
            t.add(map.description()).growX().wrap().padTop(2);
        }).height(mapsize).width(mapsize);

        table.row();

        table.addImageTextButton("$editor.openin", "icon-load-map", 16 * 2, () -> {
            try{
                Vars.ui.editor.beginEditMap(map.file);
                dialog.hide();
                hide();
            }catch(Exception e){
                e.printStackTrace();
                ui.showError("$error.mapnotfound");
            }
        }).fillX().height(54f).marginLeft(10);

        table.addImageTextButton("$delete", "icon-trash-16", 16 * 2, () -> {
            ui.showConfirm("$confirm", Core.bundle.format("map.delete", map.name()), () -> {
                world.maps.removeMap(map);
                dialog.hide();
                setup();
            });
        }).fillX().height(54f).marginLeft(10).disabled(!map.custom).touchable(map.custom ? Touchable.enabled : Touchable.disabled);

        dialog.show();
    }
}
