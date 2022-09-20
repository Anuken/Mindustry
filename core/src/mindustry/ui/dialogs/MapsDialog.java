package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapsDialog extends BaseDialog{
    private BaseDialog dialog;
    private String searchString;
    private Seq<Gamemode> modes = new Seq<>();
    private Table mapTable = new Table();
    private TextField searchField;

    private boolean showBuiltIn = Core.settings.getBool("editorshowbuiltinmaps", true);
    private boolean showCustom = Core.settings.getBool("editorshowcustommaps", true);
    private boolean searchAuthor = Core.settings.getBool("editorsearchauthor", false);
    private boolean searchDescription = Core.settings.getBool("editorsearchdescription", false);

    public MapsDialog(){
        super("@maps");

        buttons.remove();

        addCloseListener();

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

        searchString = null;

        if(Core.graphics.isPortrait()){
            buttons.button("@back", Icon.left, this::hide).size(210f * 2f, 64f).colspan(2);
            buttons.row();
        }else{
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }

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

        cont.clear();

        rebuildMaps();

        ScrollPane pane = new ScrollPane(mapTable);
        pane.setFadeScrollBars(false);

        Table search = new Table();
        search.image(Icon.zoom);
        searchField = search.field("", t -> {
            searchString = t.length() > 0 ? t.toLowerCase() : null;
            rebuildMaps();
        }).maxTextLength(50).growX().get();
        searchField.setMessageText("@editor.search");
        search.button(Icon.filter, Styles.emptyi, this::showMapFilters);

        cont.add(search).growX();
        cont.row();
        cont.add(pane).uniformX().growY();
        cont.row();
        cont.add(buttons).growX();
    }

    void rebuildMaps(){
        mapTable.clear();

        mapTable.marginRight(24);

        int maxwidth = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        float mapsize = 200f;
        boolean noMapsShown = true;

        int i = 0;

        Seq<Map> mapList = showCustom ?
            showBuiltIn ? maps.all() : maps.customMaps() :
            showBuiltIn ? maps.defaultMaps() : null;

        if(mapList != null){
            for(Map map : mapList){

                boolean invalid = false;
                for(Gamemode mode : modes){
                    invalid |= !mode.valid(map);
                }
                if(invalid || (searchString != null
                    && !Strings.stripColors(map.name()).toLowerCase().contains(searchString)
                    && (!searchAuthor || !Strings.stripColors(map.author()).toLowerCase().contains(searchString))
                    && (!searchDescription || !Strings.stripColors(map.description()).toLowerCase().contains(searchString)))){
                    continue;
                }

                noMapsShown = false;

                if(i % maxwidth == 0){
                    mapTable.row();
                }

                TextButton button = mapTable.button("", Styles.flatt, () -> showMapInfo(map)).width(mapsize).pad(8).get();
                button.clearChildren();
                button.margin(9);
                button.add(map.name()).width(mapsize - 18f).center().get().setEllipsis(true);
                button.row();
                button.image().growX().pad(4).color(Pal.gray);
                button.row();
                button.stack(new Image(map.safeTexture()).setScaling(Scaling.fit), new BorderImage(map.safeTexture()).setScaling(Scaling.fit)).size(mapsize - 20f);
                button.row();
                button.add(map.custom ? "@custom" : map.workshop ? "@workshop" : map.mod != null ? "[lightgray]" + map.mod.meta.displayName() : "@builtin").color(Color.gray).padTop(3);

                i++;
            }
        }

        if(noMapsShown){
            mapTable.add("@maps.none");
        }
    }

    void showMapFilters(){
        dialog = new BaseDialog("@editor.filters");
        dialog.addCloseButton();
        dialog.cont.table(menu -> {
            menu.add("@editor.filters.mode").width(150f).left();
            menu.table(t -> {
                for(Gamemode mode : Gamemode.all){
                    TextureRegionDrawable icon = Vars.ui.getIcon("mode" + Strings.capitalize(mode.name()));
                    if(Core.atlas.isFound(icon.getRegion())){
                        t.button(icon, Styles.emptyTogglei, () -> {
                            if(modes.contains(mode)){
                                modes.remove(mode);
                            }else{
                                modes.add(mode);
                            }
                            rebuildMaps();
                        }).size(60f).checked(modes.contains(mode)).tooltip("@mode." + mode.name() + ".name");
                    }
                }
            }).padBottom(10f);
            menu.row();

            menu.add("@editor.filters.type").width(150f).left();
            menu.table(Tex.button, t -> {
                t.button("@custom", Styles.flatTogglet, () -> {
                    showCustom = !showCustom;
                    Core.settings.put("editorshowcustommaps", showCustom);
                    Core.settings.forceSave();
                    rebuildMaps();
                }).size(150f, 60f).checked(showCustom);
                t.button("@builtin", Styles.flatTogglet, () -> {
                    showBuiltIn = !showBuiltIn;
                    Core.settings.put("editorshowbuiltinmaps", showBuiltIn);
                    Core.settings.forceSave();
                    rebuildMaps();
                }).size(150f, 60f).checked(showBuiltIn);
            }).padBottom(10f);
            menu.row();

            menu.add("@editor.filters.search").width(150f).left();
            menu.table(Tex.button, t -> {
                t.button("@editor.filters.author", Styles.flatTogglet, () -> {
                    searchAuthor = !searchAuthor;
                    Core.settings.put("editorsearchauthor", searchAuthor);
                    Core.settings.forceSave();
                    rebuildMaps();
                }).size(150f, 60f).checked(searchAuthor);
                t.button("@editor.filters.description", Styles.flatTogglet, () -> {
                    searchDescription = !searchDescription;
                    Core.settings.put("editorsearchdescription", searchDescription);
                    Core.settings.forceSave();
                    rebuildMaps();
                }).size(150f, 60f).checked(searchDescription);
            });
        });

        dialog.show();
    }

    void showMapInfo(Map map){
        dialog = new BaseDialog("@editor.mapinfo");
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
                e.printStackTrace();
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

    @Override
    public Dialog show(){
        super.show();

        if(Core.app.isDesktop() && searchField != null){
            Core.scene.setKeyboardFocus(searchField);
        }

        return this;
    }
}
