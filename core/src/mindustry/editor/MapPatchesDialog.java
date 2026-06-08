package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapPatchesDialog extends BaseDialog{
    private Table list;
    private MapPatchImagesDialog images = new MapPatchImagesDialog();
    private DataAssetType currentType = DataAssetType.music;

    private TextureRegionDrawable[] typeIcons = {
    Icon.fileCode,
    Icon.box,
    Icon.fileText,
    Icon.image,
    Icon.volumeUp,
    Icon.music
    };

    public MapPatchesDialog(){
        super("@editor.patches");

        shown(this::setup);

        addCloseButton();
        getCell(cont).grow();
        //buttons.button("@editor.patches.guide", Icon.link, () -> Core.app.openURI(patchesGuideURL)).size(190f, 64f);

        //buttons.button("@editor.images", Icon.image, () -> images.show()).size(190f, 64f);

        //buttons.button("@add", Icon.add, () -> showImport(this::addPatch)).size(190f, 64f);

        Table types = new Table();
        for(DataAssetType type : DataAssetType.all){
            types.button(typeIcons[type.ordinal()], Styles.squareTogglei, () -> {
                currentType = type;
                setup();
            }).checked(b -> currentType == type).tooltip(type.name()).size(50f).pad(4f);
        }

        cont.top().left();

        cont.add(types).left().row();

        cont.pane(t -> list = t).grow().top();
    }

    private void setup(){
        list.clearChildren();

        list.defaults().pad(4f);
        float h = 50f;

        for(var content : state.data.getAssets(currentType)){
            list.button(content.path, Styles.grayt, () -> {

            }).size(mobile ? 390f : 450f, h).margin(10f).with(b -> b.getLabel().setAlignment(Align.left, Align.left));

            list.row();
        }

        if(true) return;

        var patches = state.data.getPatches();

        if(patches.isEmpty()){
            list.add("@editor.patches.none");
        }else{

            list.defaults().pad(4f);
            for(var patch : patches){
                int fields = countFields(patch.json);

                if(patch.warnings.size > 0){
                    list.button(Icon.warning, Styles.graySquarei, iconMed, () -> {
                        BaseDialog dialog = new BaseDialog("@editor.patches.errors");
                        dialog.cont.top().pane(p -> {
                            p.top();

                            for(var warning : patch.warnings){
                                p.table(Styles.grayPanel, in -> {
                                    in.add(warning.replaceAll("\t", "  "), Styles.monoLabel).grow().wrap();
                                }).margin(6f).growX().pad(3f).row();
                            }
                        }).grow();
                        dialog.addCloseButton();
                        dialog.show();
                    }).size(h);
                }else{
                    list.add().size(h);
                }

                list.button((patch.name.isEmpty() ? "<unnamed>\n" : "[accent]" + patch.name + "\n") + "[lightgray][[" + Core.bundle.format("editor.patch.fields", fields) + "]", Styles.grayt, () -> {
                    BaseDialog dialog = new BaseDialog(Core.bundle.format("editor.patch", patch.name.isEmpty() ? "<unnamed>" : patch.name));
                    dialog.cont.top().pane(p -> {
                        p.top();
                        p.table(Styles.grayPanel, in -> {
                            in.add(patch.patch.replaceAll("\t", "  "), Styles.monoLabel).grow().wrap().left().labelAlign(Align.left);
                        }).margin(6f).growX().pad(5f).row();
                    }).grow();
                    dialog.addCloseButton();
                    dialog.show();
                }).size(mobile ? 390f : 450f, h).margin(10f).with(b -> {
                    b.getLabel().setAlignment(Align.left, Align.left);
                });

                list.button(Icon.copy, Styles.graySquarei, Vars.iconMed, () -> {
                    Core.app.setClipboardText(patch.patch);
                    ui.showInfoFade("@copied");
                }).size(h);

                list.button(Icon.refresh, Styles.graySquarei, Vars.iconMed, () -> {
                    showImport(str -> addPatch(str, patches.indexOf(patch)));
                }).size(h);

                list.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                    ui.showConfirm("@editor.patches.delete.confirm",  () -> {
                        patches.remove(patch);
                        setup();
                    });
                }).size(h);

                list.row();
            }
        }
    }

    void showImport(Cons<String> handler){
        BaseDialog dialog = new BaseDialog("@editor.import");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.button("@schematic.copy.import", Icon.copy, style, () -> {
                    dialog.hide();
                    handler.get(Core.app.getClipboardText());
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                t.row();
                t.button("@schematic.importfile", Icon.download, style, () -> platform.showMultiFileChooser(file -> {
                    dialog.hide();
                    handler.get(file.readString());
                }, "json", "hjson", "json5")).marginLeft(12f);
                t.row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    void addPatch(String patch){
        addPatch(patch, -1);
    }

    void addPatch(String patch, int replaceIndex){
        var patches = state.data.getPatches();
        var oldPatches = patches.copy();

        try{
            Jval.read(patch); //validation

            if(replaceIndex == -1){
                patches.add(new PatchAsset(patch));
            }else{
                patches.set(replaceIndex, new PatchAsset(patch));
            }

            state.data.reloadPatches(patches);

            setup();
        }catch(Exception e){
            patches.set(oldPatches);
            ui.showException("@editor.patches.importerror", e);
        }
    }

    int countFields(JsonValue value){
        if(value.isObject() || value.isArray()){
            int sum = 0;
            for(var child : value){
                sum += countFields(child);
            }
            return Math.max(sum, 1);
        }else{
            return 1;
        }
    }
}
