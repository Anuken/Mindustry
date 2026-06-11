package mindustry.editor.data;

import arc.*;
import arc.files.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapContentView implements AssetView{
    static ObjectMap<ContentType, TextureRegionDrawable> contentIcons = ObjectMap.of(
    ContentType.item, Icon.box,
    ContentType.liquid, Icon.liquid,
    ContentType.unit, Icon.units,
    ContentType.block, Icon.distribution,
    ContentType.planet, Icon.planet,
    ContentType.weather, Icon.power, //TODO cloud icon
    ContentType.status, Icon.power
    );

    @Override
    public void build(MapAssetsDialog diag, Table list){
        float h = 50f;

        var contents = state.data.getContent();

        list.defaults().pad(4f);
        for(var content : contents){
            if(diag.searchString != null && !content.name.toLowerCase().contains(diag.searchString)) continue;

            if(content.warnings.size > 0){
                list.button(Icon.warning, Styles.graySquarei, iconMed, () -> {
                    BaseDialog dialog = new BaseDialog("@asset.content.errors");
                    dialog.cont.top().pane(p -> {
                        p.top();

                        for(var warning : content.warnings){
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

            list.button(content.content instanceof UnlockableContent u && (u.uiIcon.found() || u.fullIcon.found()) ? new TextureRegionDrawable(u.uiIcon.found() ? u.uiIcon : u.fullIcon) :
                contentIcons.get(content.type, Icon.warning), Styles.graySquarei, iconMed, () -> {}).touchable(Touchable.disabled).size(h);

            list.button(("[accent]" + content.name + "\n") + "[lightgray]" + Core.bundle.get("content." + content.type), Styles.grayt, () -> {
                BaseDialog dialog = new BaseDialog(content.name);
                dialog.cont.top().pane(p -> {
                    p.top();
                    p.table(Styles.grayPanel, in -> {
                        in.add(content.data.replaceAll("\t", "  "), Styles.monoLabel).grow().wrap().left().labelAlign(Align.left);
                    }).margin(6f).growX().pad(5f).row();
                }).grow();
                dialog.addCloseButton();
                dialog.show();
            }).size(mobile ? 390f : 450f, h).margin(10f).with(b -> {
                b.getLabel().setAlignment(Align.left, Align.left);
            });

            list.button(Icon.copy, Styles.graySquarei, Vars.iconMed, () -> {
                Core.app.setClipboardText(content.data);
                ui.showInfoFade("@copied");
            }).size(h);

            list.button(Icon.export, Styles.graySquarei, Vars.iconMed, () -> {
                if(ios){
                    try{
                        Fi out = tmpDirectory.child(Strings.getFileName(content.path));
                        out.writeString(content.data);
                        platform.shareFile(out);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }else{
                    platform.showFileChooser(false, "properties", out -> {
                        try{
                            out.writeString(content.data);
                        }catch(Exception e){
                            ui.showException(e);
                        }
                    });
                }
            }).size(h);

            list.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                ui.showConfirm("@asset.delete.confirm",  () -> {
                    contents.remove(content);
                    Vars.content.remove(content.content);
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
        buttons.button("@add", Icon.add, () -> showImport(diag)).size(190f, 64f);
    }

    void addContent(ContentType type, String path, String data){
        ContentAsset asset = new ContentAsset(type.folderName + "/" + path, type, data);
        state.data.getContent().add(asset);
        state.data.reloadContent(false);
    }

    void showImport(MapAssetsDialog diag){
        ContentType[] selected = {ContentType.item};
        boolean[] invalid = {false};

        BaseDialog choose = new BaseDialog("@asset.content.add");
        choose.setFillParent(false);
        choose.closeOnBack();
        choose.cont.add("@name").padRight(10f);
        var nameField = choose.cont.field("", text -> invalid[0] = content.byName(text) != null)
        .with(t -> t.setFilter((field, c) -> !Character.isWhitespace(c))).width(400f).get();
        choose.cont.row();

        choose.cont.add("@asset.content.type").padRight(10f);
        choose.cont.table(t -> {
            t.table(Styles.grayPanel, c -> {
                c.margin(8f);
                c.label(() -> "@content." + selected[0].name());
            }).height(50f).pad(4f).marginLeft(5f).marginRight(5f).width(160f);

            t.image(Tex.whiteui, Pal.accent).size(4f, 50f).pad(4f);

            for(ContentType type : ContentAsset.loadableContent){
                t.button(contentIcons.get(type), Styles.grayTogglei, iconMed, () -> selected[0] = type).checked(b -> selected[0] == type).size(50f).pad(4f).tooltip("@content." + type.name());
            }
        });

        choose.cont.row();

        choose.cont.add("@asset.content.exists").colspan(2).visible(() -> invalid[0]);

        choose.buttons.button("@cancel", Icon.cancel, choose::hide).size(170f, 64f);

        choose.buttons.button("@asset.content.import.file", Icon.fileText, () -> platform.showMultiFileChooser(file -> {
            choose.hide();
            addContent(selected[0], nameField.getText(), file.readString());
            diag.rebuild();
        }, "json", "hjson", "json5")).size(200f, 64f).disabled(b -> nameField.getText().isEmpty() || invalid[0]);

        choose.buttons.button("@asset.content.import.clipboard", Icon.copy, () -> {
            String text = Core.app.getClipboardText();
            if(text == null) return;

            choose.hide();
            addContent(selected[0], nameField.getText(), text);
            diag.rebuild();
        }).size(200f, 64f).disabled(b -> nameField.getText().isEmpty() || invalid[0] || Core.app.getClipboardText() == null);
        choose.show();
    }
}
