package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.input.KeyCode;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public abstract class MapListDialog extends BaseDialog{
    BaseDialog activeDialog;

    private String searchString;
    private Seq<Gamemode> modes = new Seq<>();
    private Table mapTable = new Table();
    private TextField searchField;

    private boolean
    showBuiltIn = Core.settings.getBool("editorshowbuiltinmaps", true),
    showCustom = Core.settings.getBool("editorshowcustommaps", true),
    showModded = Core.settings.getBool("editorshowmoddedmaps", true),
    searchAuthor = Core.settings.getBool("editorsearchauthor", false),
    searchDescription = Core.settings.getBool("editorsearchdescription", false),
    searchModname = Core.settings.getBool("editorsearchmodname", false),
    prioritizeModded = Core.settings.getBool("editorprioritizemodded", false),
    prioritizeCustom = Core.settings.getBool("editorprioritizecustom", false),
    displayType;

    public MapListDialog(String title, boolean displayType){
        super(title);

        this.displayType = displayType;

        buttons.remove();

        addCloseListener();

        shown(this::setup);
        onResize(() -> {
            if(activeDialog != null){
                activeDialog.hide();
            }
            setup();
        });
    }

    void buildButtons(){}

    abstract void showMap(Map map);

    void setup(){
        makeButtonOverlay();

        buttons.clearChildren();

        searchString = null;

        if(Core.graphics.isPortrait() && displayType){
            buttons.button("@back", Icon.left, this::hide).size(210f * 2f, 64f).colspan(2);
            buttons.row();
        }else{
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }

        buildButtons();

        cont.clear();

        rebuildMaps();

        ScrollPane pane = new ScrollPane(mapTable);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabledX(true);

        Table search = new Table();
        search.image(Icon.zoom);
        searchField = search.field("", t -> {
            searchString = t.length() > 0 ? t.toLowerCase() : null;
            rebuildMaps();
        }).maxTextLength(50).growX().get();
        searchField.setMessageText("@editor.search");
        search.button(Icon.filter, Styles.emptyi, this::showMapFilters).tooltip("@editor.filters");

        cont.add(search).growX();
        cont.row();
        cont.add(pane).padLeft(28f).uniformX().grow().padBottom(64f);
    }

    void rebuildMaps(){
        mapTable.clear();

        mapTable.marginRight(12f);

        int maxwidth = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        float mapsize = 200f;
        boolean noMapsShown = true;

        int i = 0;

        Seq<Map> mapList = new Seq<>();
        
        if(showCustom) mapList.addAll(maps.customMaps());
        if(showBuiltIn) mapList.addAll(maps.defaultMaps());
        if(showModded) mapList.addAll(maps.moddedMaps());

        mapList = mapList.distinct();

        if(mapList != null){
            if(prioritizeModded){
                Seq<Map> ordered = new Seq<>();
                ordered.addAll(mapList.select(m -> m.mod != null)).sortComparing(m -> m.mod.meta.displayName);
                ordered.addAll(mapList.select(m -> m.mod == null));
                mapList = ordered;
            }
            if(prioritizeCustom){
                Seq<Map> ordered = new Seq<>();
                ordered.addAll(mapList.select(m -> m.custom)).sortComparing(m -> m.plainName());
                ordered.addAll(mapList.select(m -> !m.custom));
                mapList = ordered;
            }
            for(Map map : mapList){

                boolean invalid = false;
                for(Gamemode mode : modes){
                    invalid |= !mode.valid(map);
                }
                if(invalid || (searchString != null
                    && !map.plainName().toLowerCase().contains(searchString)
                    && (!searchAuthor || !map.plainAuthor().toLowerCase().contains(searchString))
                    && (!searchDescription || !map.plainDescription().toLowerCase().contains(searchString))
                    && (!searchModname || !(map.mod == null ? "" : Strings.stripColors(map.mod.meta.displayName).toLowerCase()).contains(searchString)))){
                    continue;
                }

                noMapsShown = false;

                if(i % maxwidth == 0){
                    mapTable.row();
                }

                TextButton button = mapTable.button("", Styles.grayt, () -> showMap(map)).width(mapsize).bottom().pad(8).get();
                button.clearChildren();
                button.margin(9);
                button.bottom();

                //TODO hide in editor?
                button.table(t -> {
                    t.left();
                    for(Gamemode mode : Gamemode.all){
                        TextureRegionDrawable icon = Vars.ui.getIcon("mode" + Strings.capitalize(mode.name()) + "Small");
                        if(mode.valid(map) && Core.atlas.isFound(icon.getRegion())){
                            t.image(icon).size(16f).pad(4f);
                        }
                    }
                    if(t.getChildren().size == 0){
                        t.add().size(16f).pad(4f);
                    }
                }).left().row();

                button.add(map.name()).width(mapsize - 18f).center().get().setEllipsis(true);
                button.row();
                button.image().growX().pad(4).color(Pal.gray);
                button.row();
                button.stack(new Image(map.safeTexture()).setScaling(Scaling.fit), new BorderImage(map.safeTexture()).setScaling(Scaling.fit)).size(mapsize - 20f);

                if(displayType){
                    button.row();
                    button.add(map.custom ? "@custom" : map.workshop ? "@workshop" : map.mod != null ? "[lightgray]" + map.mod.meta.displayName : "@builtin").color(Color.gray).padTop(3);
                }

                i++;
            }
        }

        if(noMapsShown){
            mapTable.add("@maps.none");
        }
    }

    void showMapFilters(){
        activeDialog = new BaseDialog("@editor.filters");
        activeDialog.addCloseButton();
        activeDialog.cont.table(menu -> {
            menu.table(tab -> {
                // Gamemodes
                tab.table(t -> {
                    t.add("@editor.filters.mode").padBottom(6f).row();
                    t.table(Tex.button, left -> {
                        for(Gamemode mode : Gamemode.all){
                            TextureRegionDrawable icon = Vars.ui.getIcon("mode" + Strings.capitalize(mode.name()));
                            if(Core.atlas.isFound(icon.getRegion())){
                                left.button(icon, Styles.emptyTogglei, () -> {
                                    if(modes.contains(mode)){
                                        modes.remove(mode);
                                    }else{
                                        modes.add(mode);
                                    }
                                    rebuildMaps();
                                }).left().size(60f).checked(modes.contains(mode)).tooltip("@mode." + mode.name() + ".name");
                            }
                        }
                    });
                }).pad(5f);
                tab.add().width(60f);
                // Priorities
                tab.table(t -> {
                    t.add("@editor.filters.priorities").padBottom(6f).row();
                    t.table(Tex.button, right ->{
                        right.button(ui.getIcon("players"), Styles.emptyTogglei, () -> {
                            prioritizeCustom = !prioritizeCustom;
                            if(prioritizeModded){
                                prioritizeModded = false;
                                Core.settings.put("editorprioritizemodded", false);
                            }
                            Core.settings.put("editorprioritizecustom", prioritizeCustom);
                            rebuildMaps();
                        }).size(60f).checked(b-> showCustom && prioritizeCustom).tooltip("@editor.filters.prioritizecustom").disabled(b -> !showCustom);
                        right.button(ui.getIcon("hammer"), Styles.emptyTogglei, () -> {
                            prioritizeModded = !prioritizeModded;
                            if(prioritizeCustom){
                                prioritizeCustom = false;
                                Core.settings.put("editorprioritizecustom", false);
                            }
                            Core.settings.put("editorprioritizemodded", prioritizeModded);
                            rebuildMaps();
                        }).size(60f).checked(b-> showModded && prioritizeModded).tooltip("@editor.filters.prioritizemod").disabled(b -> !showModded);
                    });
                }).expandX().pad(5f);
            }).padBottom(10f);
            menu.row();

            menu.add("@editor.filters.type").width(120f).left().row();
            menu.table(Tex.button, t -> {
                t.button("@custom", Styles.flatTogglet, () -> {
                    showCustom = !showCustom;
                    Core.settings.put("editorshowcustommaps", showCustom);
                    if(!showCustom){
                        prioritizeCustom = false;
                        Core.settings.put("editorprioritizecustom", false);
                    }
                    rebuildMaps();
                }).size(150f, 60f).checked(showCustom);
                t.button("@builtin", Styles.flatTogglet, () -> {
                    showBuiltIn = !showBuiltIn;
                    Core.settings.put("editorshowbuiltinmaps", showBuiltIn);
                    rebuildMaps();
                }).size(150f, 60f).checked(showBuiltIn);
                t.button("@modded", Styles.flatTogglet, () -> {
                    showModded = !showModded;
                    Core.settings.put("editorshowmoddedmaps", showModded);
                    if(!showModded){
                        prioritizeModded = false;
                        Core.settings.put("editorprioritizemodded", false);
                    }
                    rebuildMaps();
                }).size(150f, 60f).checked(showModded);
            }).padBottom(10f);
            menu.row();
            menu.add("@editor.filters.search").width(120f).left().row();
            menu.table(Tex.button, t -> {
                t.button("@editor.filters.author", Styles.flatTogglet, () -> {
                    searchAuthor = !searchAuthor;
                    Core.settings.put("editorsearchauthor", searchAuthor);
                    rebuildMaps();
                }).size(150f, 60f).checked(searchAuthor);
                t.button("@editor.filters.description", Styles.flatTogglet, () -> {
                    searchDescription = !searchDescription;
                    Core.settings.put("editorsearchdescription", searchDescription);
                    rebuildMaps();
                }).size(150f, 60f).checked(searchDescription);
                t.button("@editor.filters.modname", Styles.flatTogglet, () -> {
                    searchModname = !searchModname;
                    Core.settings.put("editorsearchmodname", searchModname);
                    rebuildMaps();
                }).size(150f, 60f).checked(searchModname);
            });
        });

        activeDialog.show();
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
