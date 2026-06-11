package mindustry.editor.data;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

/*
TODO:
• dividers for content types
• drop down for import/export zip, clear all
 */
public class MapAssetsDialog extends BaseDialog{
    private static final TextureRegionDrawable[] typeIcons = {
        Icon.fileCode,
        Icon.box,
        Icon.fileText,
        Icon.image,
        Icon.volumeUp,
        Icon.music
    };

    private AssetView[] views = {
        new MapPatchesView(),
        new MapContentView(),
        new MapBundlesView(),
        new MapImagesView(),
        new MapAudioView(DataAssetType.sound),
        new MapAudioView(DataAssetType.music),
    };

    TextField searchField;
    @Nullable String searchString;

    private Table list;
    private DataAssetType currentType = DataAssetType.music;

    public MapAssetsDialog(){
        super("@patches");

        shown(() -> {
            searchString = null;
            searchField.setText("");
            changeType(DataAssetType.patch);
        });

        hidden(() -> {
            list.clearChildren();
            //in case items are added
            DataPatcher.fixContentArrays();
        });

        makeButtonOverlay();
        titleTable.remove();

        Table types = new Table();
        types.defaults().size(50f).pad(4f);

        types.button(Icon.menu, Styles.graySquarei, () -> {

        });

        types.image(Tex.whiteui, Pal.accent).size(4f, 50f);

        for(DataAssetType type : DataAssetType.all){
            types.button(typeIcons[type.ordinal()], Styles.grayTogglei, () -> changeType(type))
            .checked(b -> currentType == type).tooltip(type.localized());
        }
        types.image(Tex.whiteui, Pal.accent).size(4f, 50f);

        types.table(Styles.grayPanel, t -> {
            t.margin(8f);
            t.label(() -> currentType.localized());
        }).height(50f).width(Float.NEGATIVE_INFINITY);

        types.add().growX();

        types.button("@asset.guide", Icon.link, Styles.grayt, () -> Core.app.openURI(patchesGuideURL)).marginLeft(10f).size(200f, 50f).pad(4f);

        cont.top().left();

        cont.add(types).growX().left().row();

        cont.table(search -> {
            search.image(Icon.zoom);

            searchField = search.field("", t -> {
                searchString = t.length() > 0 ? t.toLowerCase() : null;
                rebuild();
            }).growX().get();
            searchField.setMessageText("@search");
        }).growX().row();

        cont.pane(t -> list = t).grow().top();
    }

    void changeType(DataAssetType type){
        currentType = type;

        buttons.clearChildren();
        addCloseButton();
        views[currentType.ordinal()].buildButtons(this, buttons);

        //make sure new assets appear correctly when switching to the content view
        if(type == DataAssetType.content){
            state.data.reloadContent(false);
        }

        rebuild();
    }

    void rebuild(){
        list.center().top();
        list.clearChildren();
        list.marginBottom(70f);

        views[currentType.ordinal()].build(this, list);
    }

}
