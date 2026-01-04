package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapPatchesDialog extends BaseDialog{
    private Table list;

    public MapPatchesDialog(){
        super("@editor.patches");

        shown(this::setup);

        addCloseButton();
        buttons.button("@editor.patches.guide", Icon.link, () -> Core.app.openURI(patchesGuideURL)).size(200, 64f);

        buttons.button("@add", Icon.add, () -> showImport(this::addPatch)).size(200f, 64f);

        cont.top();
        getCell(cont).grow();

        cont.pane(t -> list = t);
    }

    private void setup(){
        list.clearChildren();
        var patches = state.patcher.patches;

        if(patches.isEmpty()){
            list.add("@editor.patches.none");
        }else{
            Table t = list;

            t.defaults().pad(4f);
            float h = 50f;
            for(var patch : patches){
                int fields = countFields(patch.json);

                if(patch.warnings.size > 0){
                    t.button(Icon.warning, Styles.graySquarei, iconMed, () -> {
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
                    t.add().size(h);
                }

                t.button((patch.name.isEmpty() ? "<unnamed>\n" : "[accent]" + patch.name + "\n") + "[lightgray][[" + Core.bundle.format("editor.patch.fields", fields) + "]", Styles.grayt, () -> {
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

                t.button(Icon.copy, Styles.graySquarei, Vars.iconMed, () -> {
                    Core.app.setClipboardText(patch.patch);
                    ui.showInfoFade("@copied");
                }).size(h);

                t.button(Icon.refresh, Styles.graySquarei, Vars.iconMed, () -> {
                    showImport(str -> addPatch(str, patches.indexOf(patch)));
                }).size(h);

                t.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                    ui.showConfirm("@editor.patches.delete.confirm",  () -> {
                        patches.remove(patch);
                        setup();
                    });
                }).size(h);

                t.row();
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
        var oldPatches = state.patcher.patches.copy();
        try{
            Jval.read(patch); //validation
            Seq<String> patches = state.patcher.patches.map(p -> p.patch);
            if(replaceIndex == -1){
                patches.add(patch);
            }else{
                patches.set(replaceIndex, patch);
            }
            state.patcher.apply(patches);

            setup();
        }catch(Exception e){
            state.patcher.patches.set(oldPatches);
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
