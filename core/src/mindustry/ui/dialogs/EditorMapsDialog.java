package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class EditorMapsDialog extends MapListDialog{

    public EditorMapsDialog(){
        super("@maps", true);
    }

    @Override
    void buildButtons(){
        buttons.button("@editor.newmap", Icon.add, () -> {
            ui.showTextInput("@editor.newmap", "@editor.mapname", "", text -> {
                Runnable show = () -> ui.loadAnd(() -> {
                    hide();
                    ui.editor.show();
                    editor.tags.put("name", text);
                    Events.fire(new MapMakeEvent());
                });

                if(maps.byName(text) != null){
                    ui.showErrorMessage("@editor.exists");
                }else{
                    show.run();
                }
            });
        }).size(210f, 64f);

        buttons.button("@editor.importmap", Icon.upload, () -> {
            platform.showFileChooser(true, mapExtension, file -> {
                ui.loadAnd(() -> {
                    maps.tryCatchMapError(() -> {
                        if(MapIO.isImage(file)){
                            ui.showErrorMessage("@editor.errorimage");
                            return;
                        }

                        Map map = MapIO.createMap(file, true);

                        //when you attempt to import a save, it will have no name, so generate one
                        String name = map.tags.get("name", () -> {
                            String result = "unknown";
                            int number = 0;
                            while(maps.byName(result + number++) != null) ;
                            return result + number;
                        });

                        //this will never actually get called, but it remains just in case
                        if(name == null){
                            ui.showErrorMessage("@editor.errorname");
                            return;
                        }

                        Map conflict = maps.all().find(m -> m.name().equals(name));

                        if(conflict != null && !conflict.custom){
                            ui.showInfo(Core.bundle.format("editor.import.exists", name));
                        }else if(conflict != null){
                            ui.showConfirm("@confirm", Core.bundle.format("editor.overwrite.confirm", map.name()), () -> {
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
    }

    @Override
    void showMap(Map map){
        BaseDialog dialog = activeDialog = new BaseDialog("@editor.mapinfo");
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

            t.add("@editor.mapname").padRight(10).color(Color.gray).padTop(0);
            t.row();
            t.add(map.name()).growX().wrap().padTop(2);
            t.row();
            t.add("@editor.author").padRight(10).color(Color.gray);
            t.row();
            t.add(!map.custom && map.tags.get("author", "").isEmpty() ? "Anuke" : map.author()).growX().wrap().padTop(2);
            t.row();

            if(!map.tags.get("description", "").isEmpty()){
                t.add("@editor.description").padRight(10).color(Color.gray).top();
                t.row();
                t.add(map.description()).growX().wrap().padTop(2);
            }
        }).height(mapsize).width(mapsize);

        table.row();

        table.button("@editor.openin", Icon.export, () -> {
            try{
                Vars.ui.editor.beginEditMap(map.file);
                dialog.hide();
                hide();
            }catch(Exception e){
                Log.err(e);
                ui.showErrorMessage("@error.mapnotfound");
            }
        }).fillX().height(54f).marginLeft(10);

        table.button(map.workshop && steam ? "@view.workshop" : "@delete", map.workshop && steam ? Icon.link : Icon.trash, () -> {
            if(map.workshop && steam){
                platform.viewListing(map);
            }else{
                ui.showConfirm("@confirm", Core.bundle.format("map.delete", map.name()), () -> {
                    maps.removeMap(map);
                    dialog.hide();
                    setup();
                });
            }
        }).fillX().height(54f).marginLeft(10).disabled(!map.workshop && !map.custom);

        dialog.show();
    }

}
