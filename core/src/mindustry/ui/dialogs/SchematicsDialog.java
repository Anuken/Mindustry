package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class SchematicsDialog extends BaseDialog{
    private SchematicInfoDialog info = new SchematicInfoDialog();
    private Schematic firstSchematic;
    private String search = "";
    private TextField searchField;

    public SchematicsDialog(){
        super("$schematics");
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> {
            ((Texture)t).setWrap(TextureWrap.repeat);
        };

        shouldPause = true;
        addCloseButton();
        buttons.button("$schematic.import", Icon.download, this::showImport);
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        search = "";
        Runnable[] rebuildPane = {null};

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane[0].run();
            }).growX().get();
        }).fillX().padBottom(4);

        cont.row();

        cont.pane(t -> {
            t.top();
            t.margin(20f);

            t.update(() -> {
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == searchField && firstSchematic != null){
                    control.input.useSchematic(firstSchematic);
                    hide();
                }
            });

            rebuildPane[0] = () -> {
                int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);

                t.clear();
                int i = 0;
                String regex = "[`~!@#$%^&*()-_=+[{]}|;:'\",<.>/?]";
                String searchString = search.toLowerCase().replaceAll(regex, " ");

                firstSchematic = null;

                for(Schematic s : schematics.all()){
                    if(!search.isEmpty() && !s.name().toLowerCase().replaceAll(regex, " ").contains(searchString)) continue;
                    if(firstSchematic == null) firstSchematic = s;

                    Button[] sel = {null};
                    sel[0] = t.button(b -> {
                        b.top();
                        b.margin(0f);
                        b.table(buttons -> {
                            buttons.left();
                            buttons.defaults().size(50f);

                            ImageButtonStyle style = Styles.clearPartiali;

                            buttons.button(Icon.info, style, () -> {
                                showInfo(s);
                            });

                            buttons.button(Icon.upload, style, () -> {
                                showExport(s);
                            });

                            buttons.button(Icon.pencil, style, () -> {
                                ui.showTextInput("$schematic.rename", "$name", s.name(), res -> {
                                    Schematic replacement = schematics.all().find(other -> other.name().equals(res) && other != s);
                                    if(replacement != null){
                                        //renaming to an existing schematic is not allowed, as it is not clear how the tags would be merged, and which one should be removed
                                        ui.showErrorMessage("$schematic.exists");
                                        return;
                                    }

                                    s.tags.put("name", res);
                                    s.save();
                                    rebuildPane[0].run();
                                });
                            });

                            if(s.hasSteamID()){
                                buttons.button(Icon.link, style, () -> platform.viewListing(s));
                            }else{
                                buttons.button(Icon.trash, style, () -> {
                                    if(s.mod != null){
                                        ui.showInfo(Core.bundle.format("mod.item.remove", s.mod.meta.displayName()));
                                    }else{
                                        ui.showConfirm("$confirm", "$schematic.delete.confirm", () -> {
                                            schematics.remove(s);
                                            rebuildPane[0].run();
                                        });
                                    }
                                });
                            }

                        }).growX().height(50f);
                        b.row();
                        b.stack(new SchematicImage(s).setScaling(Scaling.fit), new Table(n -> {
                            n.top();
                            n.table(Styles.black3, c -> {
                                Label label = c.add(s.name()).style(Styles.outlineLabel).color(Color.white).top().growX().maxWidth(200f - 8f).get();
                                label.setEllipsis(true);
                                label.setAlignment(Align.center);
                            }).growX().margin(1).pad(4).maxWidth(Scl.scl(200f - 8f)).padBottom(0);
                        })).size(200f);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        if(state.isMenu()){
                            showInfo(s);
                        }else{
                            control.input.useSchematic(s);
                            hide();
                        }
                    }).pad(4).style(Styles.cleari).get();

                    sel[0].getStyle().up = Tex.pane;

                    if(++i % cols == 0){
                        t.row();
                    }
                }

                if(firstSchematic == null){
                    t.add("$none");
                }
            };

            rebuildPane[0].run();
        }).get().setScrollingDisabled(true, false);
    }

    public void showInfo(Schematic schematic){
        info.show(schematic);
    }

    public void showImport(){
        BaseDialog dialog = new BaseDialog("$editor.export");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.cleart;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.button("$schematic.copy.import", Icon.copy, style, () -> {
                    dialog.hide();
                    try{
                        Schematic s = Schematics.readBase64(Core.app.getClipboardText());
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        ui.showInfoFade("$schematic.saved");
                        showInfo(s);
                    }catch(Throwable e){
                        ui.showException(e);
                    }
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith(schematicBaseStart));
                t.row();
                t.button("$schematic.importfile", Icon.download, style, () -> platform.showFileChooser(true, schematicExtension, file -> {
                    dialog.hide();

                    try{
                        Schematic s = Schematics.read(file);
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        showInfo(s);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                })).marginLeft(12f);
                t.row();
                if(steam){
                    t.button("$schematic.browseworkshop", Icon.book, style, () -> {
                        dialog.hide();
                        platform.openWorkshop();
                    }).marginLeft(12f);
                }
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    public void showExport(Schematic s){
        BaseDialog dialog = new BaseDialog("$editor.export");
        dialog.cont.pane(p -> {
           p.margin(10f);
           p.table(Tex.button, t -> {
               TextButtonStyle style = Styles.cleart;
                t.defaults().size(280f, 60f).left();
                if(steam && !s.hasSteamID()){
                    t.button("$schematic.shareworkshop", Icon.book, style,
                        () -> platform.publish(s)).marginLeft(12f);
                    t.row();
                    dialog.hide();
                }
                t.button("$schematic.copy", Icon.copy, style, () -> {
                    dialog.hide();
                    ui.showInfoFade("$copied");
                    Core.app.setClipboardText(schematics.writeBase64(s));
                }).marginLeft(12f);
                t.row();
                t.button("$schematic.exportfile", Icon.export, style, () -> {
                    dialog.hide();
                    platform.export(s.name(), schematicExtension, file -> Schematics.write(s, file));
                }).marginLeft(12f);
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    public void focusSearchField(){
        if(searchField == null) return;

        Core.scene.setKeyboardFocus(searchField);
    }

    public static class SchematicImage extends Image{
        public float scaling = 16f;
        public float thickness = 4f;
        public Color borderColor = Pal.gray;

        private Schematic schematic;
        boolean set;

        public SchematicImage(Schematic s){
            super(Tex.clear);
            setScaling(Scaling.fit);
            schematic = s;

            if(schematics.hasPreview(s)){
                setPreview();
                set = true;
            }
        }

        @Override
        public void draw(){
            boolean checked = parent.parent instanceof Button
                && ((Button)parent.parent).isOver();

            boolean wasSet = set;
            if(!set){
                Core.app.post(this::setPreview);
                set = true;
            }

            Texture background = Core.assets.get("sprites/schematic-background.png", Texture.class);
            TextureRegion region = Draw.wrap(background);
            float xr = width / scaling;
            float yr = height / scaling;
            region.setU2(xr);
            region.setV2(yr);
            Draw.color();
            Draw.alpha(parentAlpha);
            Draw.rect(region, x + width/2f, y + height/2f, width, height);

            if(wasSet){
                super.draw();
            }else{
                Draw.rect(Icon.refresh.getRegion(), x + width/2f, y + height/2f, width/4f, height/4f);
            }

            Draw.color(checked ? Pal.accent : borderColor);
            Draw.alpha(parentAlpha);
            Lines.stroke(Scl.scl(thickness));
            Lines.rect(x, y, width, height);
            Draw.reset();
        }

        private void setPreview(){
            TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion(schematics.getPreview(schematic)));
            setDrawable(draw);
            setScaling(Scaling.fit);
        }
    }

    public static class SchematicInfoDialog extends BaseDialog{

        SchematicInfoDialog(){
            super("");
            setFillParent(true);
            addCloseButton();
        }

        public void show(Schematic schem){
            cont.clear();
            title.setText("[[" + Core.bundle.get("schematic") + "] " +schem.name());

            cont.add(Core.bundle.format("schematic.info", schem.width, schem.height, schem.tiles.size)).color(Color.lightGray);
            cont.row();
            cont.add(new SchematicImage(schem)).maxSize(800f);
            cont.row();

            Seq<ItemStack> arr = schem.requirements();
            cont.table(r -> {
                int i = 0;
                for(ItemStack s : arr){
                    r.image(s.item.icon(Cicon.small)).left();
                    r.label(() -> {
                        Building core = player.core();
                        if(core == null || state.rules.infiniteResources || core.items.has(s.item, s.amount)) return "[lightgray]" + s.amount + "";
                        return (core.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]") + Math.min(core.items.get(s.item), s.amount) + "[lightgray]/" + s.amount;
                    }).padLeft(2).left().padRight(4);

                    if(++i % 4 == 0){
                        r.row();
                    }
                }
            });
            cont.row();
            float cons = schem.powerConsumption() * 60, prod = schem.powerProduction() * 60;
            if(!Mathf.zero(cons) || !Mathf.zero(prod)){
                cont.table(t -> {

                    if(!Mathf.zero(prod)){
                        t.image(Icon.powerSmall).color(Pal.powerLight).padRight(3);
                        t.add("+" + Strings.autoFixed(prod, 2)).color(Pal.powerLight).left();

                        if(!Mathf.zero(cons)){
                            t.add().width(15);
                        }
                    }

                    if(!Mathf.zero(cons)){
                        t.image(Icon.powerSmall).color(Pal.remove).padRight(3);
                        t.add("-" + Strings.autoFixed(cons, 2)).color(Pal.remove).left();
                    }
                });
            }

            show();
        }
    }
}
