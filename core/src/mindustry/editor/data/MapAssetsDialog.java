package mindustry.editor.data;

import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

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
        new MapPatchesView(), //new MapContentView(),
        new MapBundlesView(),
        new MapImagesView(),
        new MapPatchesView(), //new MapSoundsView(),
        new MapPatchesView(), //new MapMusicView()
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

        makeButtonOverlay();
        titleTable.remove();

        Table types = new Table();
        for(DataAssetType type : DataAssetType.all){
            types.button(typeIcons[type.ordinal()], Styles.grayTogglei, () -> changeType(type))
            .checked(b -> currentType == type).tooltip(type.localized()).size(50f).pad(4f);
        }
        types.table(Styles.grayPanel, t -> {
            t.margin(8f);
            t.label(() -> currentType.localized());
        }).height(50f).pad(4f);

        cont.top().left();

        cont.add(types).left().row();

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

        rebuild();
    }

    void rebuild(){
        list.center().top();
        list.clearChildren();

        views[currentType.ordinal()].build(this, list);

        /*
        list.defaults().pad(4f);
        float h = 50f;

        for(var content : state.data.getAssets(currentType)){
            list.button(content.path, Styles.grayt, () -> {

            }).size(mobile ? 390f : 450f, h).margin(10f).with(b -> b.getLabel().setAlignment(Align.left, Align.left));

            list.row();
        }
         */
    }

}
