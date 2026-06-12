package mindustry.editor.data;

import arc.*;
import arc.files.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static mindustry.Vars.*;

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
            //show new blocks
            ui.editor.rebuildBlockSelection();
        });

        makeButtonOverlay();
        titleTable.remove();

        Table types = new Table();
        types.defaults().size(50f).pad(4f);

        types.button(Icon.menu, Styles.graySquarei, this::showEditMenu);

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

    void showEditMenu(){
        BaseDialog dialog = new BaseDialog("@edit.menu");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                t.button("@asset.importzip", Icon.download, style, () -> FileChooser.open("zip").submit(file -> {

                    //Android doesn't allow accessing the file contents outside the callback; other platforms either copy it already, or don't have dumb permission issues
                    Fi targetFile;
                    if(OS.isAndroid){
                        targetFile = tmpDirectory.child(file.name());
                        file.copyTo(targetFile);
                    }else{
                        targetFile = file;
                    }

                    dialog.hide();
                    ui.loadAnd(() -> {
                        try{
                            Seq<DataAsset> results = new Seq<>();
                            Fi zipped = new ZipFi(targetFile);
                            Seq<String> errors = new Seq<>();
                            for(var type : DataAssetType.all){
                                Fi folder = zipped.child(type.folder);

                                //content has sub-dirs based on type, which need to be passed as context to the reader
                                if(type == DataAssetType.content){
                                    for(ContentType ctype : ContentAsset.loadableContent){
                                        Fi subfolder = folder.child(ctype.folderName);

                                        Seq<Fi> files = subfolder.findAll(f -> type.extensions.contains(f.extension().toLowerCase(Locale.ROOT)));
                                        Log.info(files);

                                        for(Fi cfile : files){
                                            String path = cfile.absolutePath().substring(subfolder.absolutePath().length() + 1);
                                            if(state.data.hasAssetNameOrPath(type, Strings.getFileNameWithoutExtension(path), path)){
                                                errors.add("Asset of type '" + type + "' with name/path '" + path + "' already exists.");
                                                continue;
                                            }

                                            try{
                                                ContentAsset asset = new ContentAsset(cfile.absolutePath().substring(subfolder.absolutePath().length() + 1), ctype, cfile.readString());
                                                results.add(asset);
                                            }catch(Throwable e){
                                                Log.err(e);
                                                errors.add("Error loading content asset: " + cfile + ": " + Strings.getSimpleMessage( e));
                                            }
                                        }
                                    }
                                }else{
                                    Seq<Fi> files = folder.findAll(f -> type.extensions.contains(f.extension().toLowerCase(Locale.ROOT)));

                                    for(Fi cfile : files){
                                        String path = cfile.absolutePath().substring(folder.absolutePath().length() + 1);
                                        if(state.data.hasAssetNameOrPath(type, Strings.getFileNameWithoutExtension(path), path)){
                                            errors.add("Asset of type '" + type + "' with name/path '" + path + "' already exists.");
                                            continue;
                                        }
                                        try{
                                            var asset = type.create();
                                            asset.readFromZip(path, cfile);
                                            results.add(asset);
                                        }catch(Throwable e){
                                            Log.err(e);
                                            errors.add("Error loading data asset: " + cfile + ": " + Strings.getSimpleMessage(e));
                                        }
                                    }
                                }
                            }

                            //close zip file contents
                            zipped.delete();

                            if(results.size > 0){
                                var prev = state.data.getAllAssets().copy();
                                prev.addAll(results);
                                prev.sort();
                                state.data.load(prev);
                                rebuild();
                            }

                            var idiag = new BaseDialog("@asset.image.imports");
                            if(results.size > 0) idiag.cont.add(Core.bundle.format("asset.imported", results.size)).row();
                            if(!errors.isEmpty()){
                                idiag.cont.add("@asset.image.error").padBottom(10f).row();
                                idiag.cont.pane(ep -> {
                                    ep.add(Seq.with(errors).toString("\n", s -> "[gray]- []" + s)).labelAlign(Align.left, Align.left).grow();
                                });
                            }else if(results.size == 0){
                                idiag.cont.add("@asset.image.none");
                            }
                            idiag.buttons.button("@ok", Icon.ok, idiag::hide).size(200f, 64f);
                            idiag.show();

                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    });
                })).marginLeft(12f).row();
                t.button("@asset.exportzip", Icon.upload, style, () -> FileChooser.export("assets", "zip", file -> {
                    dialog.hide();
                    try(OutputStream fos = file.write(false, 8192); ZipOutputStream zos = new ZipOutputStream(fos)){
                        for(var asset : state.data.getAllAssets()){
                            String path = asset.getFullPath();
                            if(asset.isAlwaysEmbedded()){
                                zos.putNextEntry(new ZipEntry(path));
                                zos.write(asset.getData());
                                zos.closeEntry();
                            }else{
                                Fi cacheFile = asset.getCacheFile();
                                if(cacheFile == null) continue;

                                zos.putNextEntry(new ZipEntry(path));
                                zos.write(cacheFile.readBytes());
                                zos.closeEntry();
                            }
                        }
                    }
                })).marginLeft(12f).row();
                t.button("@asset.clearall", Icon.trash, style, () -> {
                    dialog.hide();
                    ui.showConfirm("@asset.clearall.confirm", () -> {
                        state.data.unload();
                        rebuild();
                    });
                }).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

}
