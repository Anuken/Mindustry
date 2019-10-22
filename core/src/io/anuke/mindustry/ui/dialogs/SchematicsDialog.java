package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Texture.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.TextButton.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;

import static io.anuke.mindustry.Vars.*;

public class SchematicsDialog extends FloatingDialog{
    private SchematicInfoDialog info = new SchematicInfoDialog();
    private String search = "";

    public SchematicsDialog(){
        super("$schematics");
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> {
            ((Texture)t).setWrap(TextureWrap.Repeat);
        };

        shouldPause = true;
        addCloseButton();
        buttons.addImageTextButton("$schematic.import", Icon.loadMapSmall, this::showImport);
        shown(this::setup);
    }

    void setup(){
        search = "";
        Runnable[] rebuildPane = {null};

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.addImage(Icon.zoom);
            s.addField(search, res -> {
                search = res;
                rebuildPane[0].run();
            }).growX();
        }).fillX().padBottom(4);

        cont.row();

        cont.pane(t -> {
            t.top();
            t.margin(20f);
            rebuildPane[0] = () -> {
                t.clear();
                int i = 0;

                if(!schematics.all().contains(s -> search.isEmpty() || s.name().contains(search))){
                    t.add("$none");
                }

                for(Schematic s : schematics.all()){
                    if(!search.isEmpty() && !s.name().contains(search)) continue;

                    Button[] sel = {null};
                    sel[0] = t.addButton(b -> {
                        b.top();
                        b.margin(0f);
                        b.table(buttons -> {
                            buttons.left();
                            buttons.defaults().size(50f);

                            ImageButtonStyle style = Styles.clearPartiali;

                            buttons.addImageButton(Icon.infoSmall, style, () -> {
                                showInfo(s);
                            });

                            buttons.addImageButton(Icon.loadMapSmall, style, () -> {
                                showExport(s);
                            });

                            buttons.addImageButton(Icon.pencilSmall, style, () -> {
                                ui.showTextInput("$schematic.rename", "$name", s.name(), res -> {
                                    s.tags.put("name", res);
                                    s.save();
                                    rebuildPane[0].run();
                                });
                            });

                            if(s.hasSteamID()){
                                buttons.addImageButton(Icon.linkSmall, style, () -> platform.viewListing(s));
                            }else{
                                buttons.addImageButton(Icon.trash16Small, style, () -> {
                                    ui.showConfirm("$confirm", "$schematic.delete.confirm", () -> {
                                        schematics.remove(s);
                                        rebuildPane[0].run();
                                    });
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
                            }).growX().margin(1).pad(4).maxWidth(200f - 8f).padBottom(0);
                        })).size(200f);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        control.input.useSchematic(s);
                        hide();
                    }).pad(4).style(Styles.cleari).get();

                    sel[0].getStyle().up = Tex.pane;

                    if(++i % 4 == 0){
                        t.row();
                    }
                }
            };

            rebuildPane[0].run();
        }).get().setScrollingDisabled(true, false);
    }

    public void showInfo(Schematic schematic){
        info.show(schematic);
    }

    public void showImport(){
        FloatingDialog dialog = new FloatingDialog("$editor.export");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.cleart;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.addImageTextButton("$schematic.copy.import", Icon.copySmall, style, () -> {
                    dialog.hide();
                    try{
                        Schematic s = schematics.readBase64(Core.app.getClipboardText());
                        schematics.add(s);
                        setup();
                        ui.showInfoFade("$schematic.saved");
                        showInfo(s);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith(schematicBaseStart));
                t.row();
                t.addImageTextButton("$schematic.importfile", Icon.saveMapSmall, style, () -> platform.showFileChooser(true, schematicExtension, file -> {
                    dialog.hide();

                    try{
                        Schematic s = Schematics.read(file);
                        schematics.add(s);
                        setup();
                        showInfo(s);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                })).marginLeft(12f);
                t.row();
                if(steam){
                    t.addImageTextButton("$schematic.browseworkshop", Icon.wikiSmall, style, () -> {
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
        FloatingDialog dialog = new FloatingDialog("$editor.export");
        dialog.cont.pane(p -> {
           p.margin(10f);
           p.table(Tex.button, t -> {
               TextButtonStyle style = Styles.cleart;
                t.defaults().size(280f, 60f).left();
                if(steam && !s.hasSteamID()){
                    t.addImageTextButton("$schematic.shareworkshop", Icon.wikiSmall, style,
                        () -> platform.publish(s)).marginLeft(12f);
                    t.row();
                    dialog.hide();
                }
                t.addImageTextButton("$schematic.copy", Icon.copySmall, style, () -> {
                    dialog.hide();
                    ui.showInfoFade("$copied");
                    Core.app.setClipboardText(schematics.writeBase64(s));
                }).marginLeft(12f);
                t.row();
                t.addImageTextButton("$schematic.exportfile", Icon.saveMapSmall, style, () -> platform.showFileChooser(false, schematicExtension, file -> {
                    dialog.hide();
                    try{
                        Schematics.write(s, file);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                })).marginLeft(12f);
            });
        });

        dialog.addCloseButton();
        dialog.show();
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
            boolean checked = getParent().getParent() instanceof Button
                && ((Button)getParent().getParent()).isOver();

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
                Draw.rect(Icon.loading.getRegion(), x + width/2f, y + height/2f, width/4f, height/4f);
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

    public static class SchematicInfoDialog extends FloatingDialog{

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

            Array<ItemStack> arr = schem.requirements();
            cont.table(r -> {
                int i = 0;
                for(ItemStack s : arr){
                    r.addImage(s.item.icon(Cicon.small)).left();
                    r.add(s.amount + "").padLeft(2).left().color(Color.lightGray).padRight(4);

                    if(++i % 4 == 0){
                        r.row();
                    }
                }
            });

            show();
        }
    }
}
