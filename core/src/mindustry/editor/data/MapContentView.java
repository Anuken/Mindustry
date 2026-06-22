package mindustry.editor.data;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
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

import java.util.regex.*;

import static mindustry.Vars.*;

public class MapContentView implements AssetView{
    private static final Pattern unsafeNamePattern = Pattern.compile("[\\[\\]{}`!@#$%^&8*();:,]");

    static ObjectMap<ContentType, TextureRegionDrawable> contentIcons = ObjectMap.of(
    ContentType.item, Icon.box,
    ContentType.liquid, Icon.liquid,
    ContentType.unit, Icon.units,
    ContentType.block, Icon.distribution,
    ContentType.planet, Icon.planet,
    ContentType.weather, Icon.drizzle,
    ContentType.status, Icon.power
    );

    @Override
    public void build(MapAssetsDialog diag, Table list){
        float h = 50f;

        var contents = state.data.getContent();

        ContentType lastType = null;

        list.defaults().pad(4f);
        for(var content : contents){
            if(diag.searchString != null && !content.name.toLowerCase().contains(diag.searchString)) continue;

            if(lastType != content.type){
                lastType = content.type;
                list.table(t -> {
                    t.left();
                    t.image(contentIcons.get(content.type)).color(Color.lightGray).size(iconSmall);
                    t.add("@content." + content.type.name()).left().color(Color.lightGray).padLeft(12f).padRight(4f);
                    t.image(Tex.whiteui).growX().color(Color.lightGray).height(4f);
                }).colspan(6).fillX().padBottom(8f).row();
            }

            if(content.warnings.size > 0){
                list.button(Icon.warning, Styles.graySquarei, iconMed, () -> {
                    BaseDialog dialog = new BaseDialog("@asset.content.errors");
                    dialog.cont.top().pane(p -> {
                        p.top();

                        var warns = content.warnings;
                        if(content.errored){
                            warns = new Seq<>();
                            warns.add(Iconc.warning + " This content has critical errors, and cannot be used until the errors are fixed.");
                            warns.addAll(content.warnings);
                        }

                        for(var warning : warns){
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

            list.button(content.content instanceof UnlockableContent u && u.uiIcon != null && (u.uiIcon.found() || u.fullIcon.found()) ? new TextureRegionDrawable(u.uiIcon.found() ? u.uiIcon : u.fullIcon) :
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

            list.button(Icon.refresh, Styles.graySquarei, Vars.iconMed, () -> {
                Cons<String> handler = str -> {
                    content.data = str;
                    ui.loadAnd(() -> {
                        state.data.reloadContent(false);
                        state.data.regenerateContentSprites(false);
                        diag.rebuild();
                    });
                };

                BaseDialog dialog = new BaseDialog("@editor.import");
                dialog.cont.pane(p -> {
                    p.margin(10f);
                    p.table(Tex.button, t -> {
                        TextButtonStyle style = Styles.flatt;
                        t.defaults().size(280f, 60f).left();
                        t.row();
                        t.button("@import.clipboard", Icon.copy, style, () -> {
                            dialog.hide();
                            handler.get(Core.app.getClipboardText());
                        }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                        t.row();
                        t.button("@import.file", Icon.download, style, () -> FileChooser.open("json", "hjson", "json5").submit(file -> {
                            dialog.hide();
                            handler.get(file.readString());
                        })).marginLeft(12f);
                        t.row();
                    });
                });

                dialog.addCloseButton();
                dialog.show();
            }).size(h);

            list.button(Icon.export, Styles.graySquarei, Vars.iconMed, () -> {
                BaseDialog dialog = new BaseDialog("@editor.export");
                dialog.addCloseButton();
                dialog.cont.pane(p -> {
                    p.margin(10f);
                    p.table(Tex.button, t -> {
                        TextButtonStyle style = Styles.flatt;
                        t.defaults().size(280f, 60f).left();
                        t.row();
                        t.button("@copy.clipboard", Icon.copy, style, () -> {
                            dialog.hide();
                            Core.app.setClipboardText(content.data);
                            ui.showInfoFade("@copied");
                        }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                        t.row();
                        t.button("@export.file", Icon.export, style, () -> {
                            dialog.hide();
                            FileChooser.export(content.name, "json", file -> file.writeString(content.data));
                        }).marginLeft(12f);
                        t.row();
                    });
                });
                dialog.show();
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

    void addContent(MapAssetsDialog diag, ContentType type, String path, String data){
        ui.loadAnd(() -> {
            ContentAsset asset = new ContentAsset(path, type, data);
            state.data.getContent().add(asset);
            state.data.reloadContent(false);
            state.data.regenerateContentSprites(false);
            diag.rebuild();
        });
    }

    void showImport(MapAssetsDialog diag){
        ContentType[] selected = {ContentType.item};
        boolean[] invalid = {false};

        BaseDialog choose = new BaseDialog("@asset.content.add");
        choose.setFillParent(false);
        choose.closeOnBack();
        choose.cont.add("@name").padRight(10f);
        var nameField = choose.cont.field("", text -> {})
        .with(t -> {
            t.setFilter((field, c) -> !Character.isWhitespace(c));
            t.setValidator(MapContentView::isSafeContentName);
            t.changed(() -> invalid[0] = content.byName(t.getText()) != null || !t.isValid());
        }).width(400f).get();
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

        choose.cont.label(() -> !nameField.isValid() ? "@asset.content.badname" : "@asset.content.exists").colspan(2).visible(() -> invalid[0]);

        choose.buttons.button("@cancel", Icon.cancel, choose::hide).size(170f, 64f);

        choose.buttons.button("@asset.content.import.file", Icon.fileText, () -> FileChooser.open("json", "json5", "hjson").submit(file -> {
            choose.hide();
            addContent(diag, selected[0], nameField.getText() + ".json", file.readString());
            diag.rebuild();
        })).size(200f, 64f).disabled(b -> nameField.getText().isEmpty() || invalid[0]);

        choose.buttons.button("@asset.content.import.clipboard", Icon.copy, () -> {
            String text = Core.app.getClipboardText();
            if(text == null) return;

            choose.hide();
            addContent(diag, selected[0], nameField.getText() + ".json", text);
        }).size(200f, 64f).disabled(b -> nameField.getText().isEmpty() || invalid[0] || Core.app.getClipboardText() == null);
        choose.show();
    }

    static boolean isSafeContentName(String name){
        return Strings.isSafeFilename(name) && !unsafeNamePattern.matcher(name).find();
    }
}
