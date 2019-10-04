package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.*;

public class MapsDialog extends FloatingDialog{
    private FloatingDialog dialog;

    public MapsDialog(){
        super("$maps");

        buttons.remove();

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                Core.app.post(this::hide);
            }
        });

        shown(this::setup);
        onResize(() -> {
            if(dialog != null){
                dialog.hide();
            }
            setup();
        });
    }

    void setup(){
        buttons.clearChildren();

        if(Core.graphics.isPortrait()){
            buttons.addImageTextButton("$back", Icon.arrowLeft, this::hide).size(210f*2f, 64f).colspan(2);
            buttons.row();
        }else{
            buttons.addImageTextButton("$back", Icon.arrowLeft, this::hide).size(210f, 64f);
        }

        buttons.addImageTextButton("$editor.newmap", Icon.add, () -> {
            ui.showTextInput("$editor.newmap", "$name", "", text -> {
                Runnable show = () -> ui.loadAnd(() -> {
                    hide();
                    ui.editor.show();
                    ui.editor.editor.getTags().put("name", text);
                    Events.fire(new MapMakeEvent());
                });

                if(maps.byName(text) != null){
                    ui.showErrorMessage("$editor.exists");
                }else{
                    show.run();
                }
            });
        }).size(210f, 64f);

        buttons.addImageTextButton("$editor.importmap", Icon.load, () -> {
            platform.showFileChooser(true, mapExtension, file -> {
                ui.loadAnd(() -> {
                    maps.tryCatchMapError(() -> {
                        if(MapIO.isImage(file)){
                            ui.showErrorMessage("$editor.errorimage");
                            return;
                        }

                        Map map = MapIO.createMap(file, true);


                        //when you attempt to import a save, it will have no name, so generate one
                        String name = map.tags.getOr("name", () -> {
                            String result = "unknown";
                            int number = 0;
                            while(maps.byName(result + number++) != null);
                            return result + number;
                        });

                        //this will never actually get called, but it remains just in case
                        if(name == null){
                            ui.showErrorMessage("$editor.errorname");
                            return;
                        }

                        Map conflict = maps.all().find(m -> m.name().equals(name));

                        if(conflict != null && !conflict.custom){
                            ui.showInfo(Core.bundle.format("editor.import.exists", name));
                        }else if(conflict != null){
                            ui.showConfirm("$confirm", "$editor.overwrite.confirm", () -> {
                                maps.tryCatchMapError(() -> {
                                    maps.removeMap(conflict);
                                    maps.importMap(map.file);
                                    setup();
                                });
                            });
                        }else{
                            maps.importMap(map.file);
                            setup();
                        }

                    });
                });
            });
        }).size(210f, 64f);


        cont.clear();

        Table maps = new Table();
        maps.marginRight(24);

        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = Mathf.clamp((int)(Core.graphics.getWidth() / Scl.scl(230)), 1, 8);
        float mapsize = 200f;

        int i = 0;
        for(Map map : Vars.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            TextButton button = maps.addButton("", Styles.cleart, () -> showMapInfo(map)).width(mapsize).pad(8).get();
            button.clearChildren();
            button.margin(9);
            button.add(map.name()).width(mapsize - 18f).center().get().setEllipsis(true);
            button.row();
            button.addImage().growX().pad(4).color(Pal.gray);
            button.row();
            button.stack(new Image(map.safeTexture()).setScaling(Scaling.fit), new BorderImage(map.safeTexture()).setScaling(Scaling.fit)).size(mapsize - 20f);
            button.row();
            button.add(map.custom ? "$custom" : map.workshop ? "$workshop" : "$builtin").color(Color.gray).padTop(3);

            i++;
        }

        if(Vars.maps.all().size == 0){
            maps.add("$maps.none");
        }

        cont.add(buttons).growX();
        cont.row();
        cont.add(pane).uniformX();
    }

    void showMapInfo(Map map){
        dialog = new FloatingDialog("$editor.mapinfo");
        dialog.addCloseButton();

        float mapsize = Core.graphics.isPortrait() ? 160f : 300f;
        Table table = dialog.cont;

        table.stack(new Image(map.safeTexture()).setScaling(Scaling.fit), new BorderImage(map.safeTexture()).setScaling(Scaling.fit)).size(mapsize);

        table.table(Styles.black, desc -> {
            desc.top();
            Table t = new Table();
            t.margin(6);

            ScrollPane pane = new ScrollPane(t);
            desc.add(pane).grow();

            t.top();
            t.defaults().padTop(10).left();

            t.add("$editor.name").padRight(10).color(Color.gray).padTop(0);
            t.row();
            t.add(map.name()).growX().wrap().padTop(2);
            t.row();
            t.add("$editor.author").padRight(10).color(Color.gray);
            t.row();
            t.add(map.custom && map.author().isEmpty() ? "Anuke" : map.author()).growX().wrap().padTop(2);
            t.row();
            t.add("$editor.description").padRight(10).color(Color.gray).top();
            t.row();
            t.add(map.description()).growX().wrap().padTop(2);
        }).height(mapsize).width(mapsize);

        table.row();

        table.addImageTextButton("$editor.openin", Icon.loadMapSmall, () -> {
            try{
                Vars.ui.editor.beginEditMap(map.file);
                dialog.hide();
                hide();
            }catch(Exception e){
                e.printStackTrace();
                ui.showErrorMessage("$error.mapnotfound");
            }
        }).fillX().height(54f).marginLeft(10);

        table.addImageTextButton(map.workshop && steam ? "$view.workshop" : "$delete", map.workshop && steam ? Icon.linkSmall : Icon.trash16Small, () -> {
            if(map.workshop && steam){
                platform.viewMapListing(map);
            }else{
                ui.showConfirm("$confirm", Core.bundle.format("map.delete", map.name()), () -> {
                    maps.removeMap(map);
                    dialog.hide();
                    setup();
                });
            }
        }).fillX().height(54f).marginLeft(10).disabled(!map.workshop && !map.custom);

        dialog.show();
    }
}
