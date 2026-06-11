package mindustry.editor.data;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.io.*;

import static mindustry.Vars.*;

public class MapBundlesView implements AssetView{

    @Override
    public void build(MapAssetsDialog diag, Table list){
        float h = 50f;

        var bundles = state.data.getBundles();

        list.defaults().pad(4f);
        for(var bundle : bundles){
            if(diag.searchString != null && !bundle.name.toLowerCase().contains(diag.searchString)) continue;

            //TODO: handle invalid files
            Fi file = bundle.getCacheFile();

            if(file == null){
                list.button(Icon.warning, Styles.graySquarei, iconMed, () -> ui.showInfo("@asset.broken")).size(h);
            }else{
                list.add().size(h);
            }

            bundle.tryLoadCache();

            list.button(("[accent]" + bundle.name + "\n") + "[lightgray][[" + Core.bundle.format("bundle.lines", (bundle.cachedBundle == null ? null : bundle.cachedBundle.size)) + "]", Styles.grayt, () -> {
                BaseDialog dialog = new BaseDialog(bundle.name);
                dialog.cont.top().pane(p -> {
                    p.top();
                    p.table(Styles.grayPanel, in -> {
                        try{
                            in.add(file.readString(), Styles.monoLabel).grow().wrap().left().labelAlign(Align.left);
                        }catch(Exception e){
                            ui.showException(e);
                        }
                    }).margin(6f).growX().pad(5f).row();
                }).grow();
                dialog.addCloseButton();
                dialog.show();
            }).size(mobile ? 390f : 450f, h).margin(10f).with(b -> {
                b.getLabel().setAlignment(Align.left, Align.left);
            }).disabled(v -> file == null);

            list.button(Icon.copy, Styles.graySquarei, Vars.iconMed, () -> {
                try{
                    Core.app.setClipboardText(file.readString());
                    ui.showInfoFade("@copied");
                }catch(Exception e){
                    ui.showException(e);
                }
            }).size(h).disabled(file == null);

            list.button(Icon.export, Styles.graySquarei, Vars.iconMed, () -> {
                FileChooser.export(bundle.name, "properties", file::copyTo);
            }).size(h).disabled(file == null);

            list.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                ui.showConfirm("@asset.delete.confirm",  () -> {
                    bundles.remove(bundle);
                    diag.rebuild();
                });
            }).size(h);

            list.row();
        }

        if(list.getChildren().isEmpty()){
            list.add("@patch.none");
        }
    }

    @Override
    public void buildButtons(MapAssetsDialog diag, Table buttons){
        buttons.button("@add", Icon.add, () -> {
            showImport(this::addBundle);
            diag.rebuild();
        }).size(190f, 64f);
    }

    void addBundle(String path, String text){
        //TODO: what if there are duplicates??
        try{
            var map = new StringMap();
            PropertiesUtils.load(map, new StringReader(text));
            byte[] bytes = text.getBytes(Strings.utf8);
            BundleAsset bundle = new BundleAsset();
            bundle.setPath(path);
            bundle.updateData(bytes);
            state.data.getBundles().add(bundle);
        }catch(Exception e){
            ui.showException(e);
        }
    }

    void showImport(Cons2<String, String> handler){
        BaseDialog dialog = new BaseDialog("@editor.import");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.button("@schematic.copy.import", Icon.copy, style, () -> {
                    dialog.hide();
                    //TODO bad name
                    handler.get("bundle.properties", Core.app.getClipboardText());
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                t.row();
                t.button("@schematic.importfile", Icon.download, style, () -> FileChooser.open("properties", "txt").submitMulti(files -> {
                    dialog.hide();
                    for(var file : files){
                        handler.get(file.name(), file.readString());
                    }
                })).marginLeft(12f);
                t.row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }
}
