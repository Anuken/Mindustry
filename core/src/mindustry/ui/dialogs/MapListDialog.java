package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
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
    searchAuthor = Core.settings.getBool("editorsearchauthor", false),
    searchDescription = Core.settings.getBool("editorsearchdescription", false),
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
        cont.add(pane).padLeft(36f).uniformX().growY();
    }

    void rebuildMaps(){
        mapTable.clear();

        mapTable.marginRight(18f);

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
                    button.add(map.custom ? "@custom" : map.workshop ? "@workshop" : map.mod != null ? "[lightgray]" + map.mod.meta.displayName() : "@builtin").color(Color.gray).padTop(3);
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
                    rebuildMaps();
                }).size(150f, 60f).checked(showCustom);
                t.button("@builtin", Styles.flatTogglet, () -> {
                    showBuiltIn = !showBuiltIn;
                    Core.settings.put("editorshowbuiltinmaps", showBuiltIn);
                    rebuildMaps();
                }).size(150f, 60f).checked(showBuiltIn);
            }).padBottom(10f);
            menu.row();

            menu.add("@editor.filters.search").width(150f).left();
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
