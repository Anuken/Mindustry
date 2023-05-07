package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.util.regex.*;

import static mindustry.Vars.*;

public class SchematicsDialog extends BaseDialog{
    private static final float tagh = 42f;
    private SchematicInfoDialog info = new SchematicInfoDialog();
    private Schematic firstSchematic;
    private String search = "";
    private TextField searchField;
    private Runnable rebuildPane = () -> {}, rebuildTags = () -> {};
    private Pattern ignoreSymbols = Pattern.compile("[`~!@#$%^&*()\\-_=+{}|;:'\",<.>/?]");
    private Seq<String> tags, selectedTags = new Seq<>();
    private boolean checkedTags;

    public SchematicsDialog(){
        super("@schematics");
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> t.setWrap(TextureWrap.repeat);

        tags = Core.settings.getJson("schematic-tags", Seq.class, String.class, Seq::new);

        shouldPause = true;
        addCloseButton();
        buttons.button("@schematic.import", Icon.download, this::showImport);
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        if(!checkedTags){
            checkTags();
            checkedTags = true;
        }

        search = "";

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane.run();
            }).growX().get();
        }).fillX().padBottom(4);

        cont.row();

        cont.table(in -> {
            in.left();
            in.add("@schematic.tags").padRight(4);

            //tags (no scroll pane visible)
            in.pane(Styles.noBarPane, t -> {
                rebuildTags = () -> {
                    t.clearChildren();
                    t.left();

                    t.defaults().pad(2).height(tagh);
                    for(var tag : tags){
                        t.button(tag, Styles.togglet, () -> {
                            if(selectedTags.contains(tag)){
                                selectedTags.remove(tag);
                            }else{
                                selectedTags.add(tag);
                            }
                            rebuildPane.run();
                        }).checked(selectedTags.contains(tag)).with(c -> c.getLabel().setWrap(false));
                    }
                };
                rebuildTags.run();
            }).fillX().height(tagh).scrollY(false);

            in.button(Icon.pencilSmall, () -> {
                showAllTags();
            }).size(tagh).pad(2).tooltip("@schematic.edittags");
        }).height(tagh).fillX();

        cont.row();

        cont.pane(t -> {
            t.top();

            t.update(() -> {
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == searchField && firstSchematic != null){
                    if(!Vars.state.rules.schematicsAllowed){
                        ui.showInfo("@schematic.disabled");
                    }else{
                        control.input.useSchematic(firstSchematic);
                        hide();
                    }
                }
            });

            rebuildPane = () -> {
                int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);

                t.clear();
                int i = 0;
                String searchString = ignoreSymbols.matcher(search.toLowerCase()).replaceAll("");

                firstSchematic = null;

                for(Schematic s : schematics.all()){
                    //make sure *tags* fit
                    if(selectedTags.any() && !s.labels.containsAll(selectedTags)) continue;
                    //make sure search fits
                    if(!search.isEmpty() && !ignoreSymbols.matcher(s.name().toLowerCase()).replaceAll("").contains(searchString)) continue;
                    if(firstSchematic == null) firstSchematic = s;

                    Button[] sel = {null};
                    sel[0] = t.button(b -> {
                        b.top();
                        b.margin(0f);
                        b.table(buttons -> {
                            buttons.left();
                            buttons.defaults().size(50f);

                            ImageButtonStyle style = Styles.clearNonei;

                            buttons.button(Icon.info, style, () -> {
                                showInfo(s);
                            });

                            buttons.button(Icon.upload, style, () -> {
                                showExport(s);
                            });

                            buttons.button(Icon.pencil, style, () -> {
                                new BaseDialog("@schematic.rename"){{
                                    setFillParent(true);

                                    cont.margin(30);

                                    cont.add("@schematic.tags").padRight(6f);
                                    cont.table(tags -> buildTags(s, tags, false)).maxWidth(400f).fillX().left().row();

                                    cont.margin(30).add("@name").padRight(6f);
                                    TextField nameField = cont.field(s.name(), null).size(400f, 55f).left().get();

                                    cont.row();

                                    cont.margin(30).add("@editor.description").padRight(6f);
                                    TextField descField = cont.area(s.description(), Styles.areaField, t -> {}).size(400f, 140f).left().get();

                                    Runnable accept = () -> {
                                        s.tags.put("name", nameField.getText());
                                        s.tags.put("description", descField.getText());
                                        s.save();
                                        hide();
                                        rebuildPane.run();
                                    };

                                    buttons.defaults().size(120, 54).pad(4);
                                    buttons.button("@ok", accept).disabled(b -> nameField.getText().isEmpty());
                                    buttons.button("@cancel", this::hide);

                                    keyDown(KeyCode.enter, () -> {
                                        if(!nameField.getText().isEmpty() && Core.scene.getKeyboardFocus() != descField){
                                            accept.run();
                                        }
                                    });
                                    keyDown(KeyCode.escape, this::hide);
                                    keyDown(KeyCode.back, this::hide);
                                    show();
                                }};
                            });

                            if(s.hasSteamID()){
                                buttons.button(Icon.link, style, () -> platform.viewListing(s));
                            }else{
                                buttons.button(Icon.trash, style, () -> {
                                    if(s.mod != null){
                                        ui.showInfo(Core.bundle.format("mod.item.remove", s.mod.meta.displayName()));
                                    }else{
                                        ui.showConfirm("@confirm", "@schematic.delete.confirm", () -> {
                                            schematics.remove(s);
                                            rebuildPane.run();
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
                            if(!Vars.state.rules.schematicsAllowed){
                                ui.showInfo("@schematic.disabled");
                            }else{
                                control.input.useSchematic(s);
                                hide();
                            }
                        }
                    }).pad(4).style(Styles.flati).get();

                    sel[0].getStyle().up = Tex.pane;

                    if(++i % cols == 0){
                        t.row();
                    }
                }

                if(firstSchematic == null){
                    t.add("@none");
                }
            };

            rebuildPane.run();
        }).grow().scrollX(false);
    }

    public void showInfo(Schematic schematic){
        info.show(schematic);
    }

    public void showImport(){
        BaseDialog dialog = new BaseDialog("@editor.export");
        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                t.row();
                t.button("@schematic.copy.import", Icon.copy, style, () -> {
                    dialog.hide();
                    try{
                        Schematic s = Schematics.readBase64(Core.app.getClipboardText());
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        ui.showInfoFade("@schematic.saved");
                        checkTags(s);
                        showInfo(s);
                    }catch(Throwable e){
                        ui.showException(e);
                    }
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith(schematicBaseStart));
                t.row();
                t.button("@schematic.importfile", Icon.download, style, () -> platform.showFileChooser(true, schematicExtension, file -> {
                    dialog.hide();

                    try{
                        Schematic s = Schematics.read(file);
                        s.removeSteamID();
                        schematics.add(s);
                        setup();
                        showInfo(s);
                        checkTags(s);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                })).marginLeft(12f);
                t.row();
                if(steam){
                    t.button("@schematic.browseworkshop", Icon.book, style, () -> {
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
        BaseDialog dialog = new BaseDialog("@editor.export");
        dialog.cont.pane(p -> {
           p.margin(10f);
           p.table(Tex.button, t -> {
               TextButtonStyle style = Styles.flatt;
                t.defaults().size(280f, 60f).left();
                if(steam && !s.hasSteamID()){
                    t.button("@schematic.shareworkshop", Icon.book, style,
                        () -> platform.publish(s)).marginLeft(12f);
                    t.row();
                    dialog.hide();
                }
                t.button("@schematic.copy", Icon.copy, style, () -> {
                    dialog.hide();
                    ui.showInfoFade("@copied");
                    Core.app.setClipboardText(schematics.writeBase64(s));
                }).marginLeft(12f);
                t.row();
                t.button("@schematic.exportfile", Icon.export, style, () -> {
                    dialog.hide();
                    platform.export(s.name(), schematicExtension, file -> Schematics.write(s, file));
                }).marginLeft(12f);
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }


    //adds all new tags to the global list of tags
    //alternatively, unknown tags could be discarded on import?
    void checkTags(){
        ObjectSet<String> encountered = new ObjectSet<>();
        encountered.addAll(tags);
        for(Schematic s : schematics.all()){
            for(var tag : s.labels){
                if(encountered.add(tag)){
                    tags.add(tag);
                }
            }
        }
    }

    //adds any new tags found to the global tag list
    //TODO remove tags from it instead?
    void checkTags(Schematic s){
        boolean any = false;
        for(var tag : s.labels){
            if(!tags.contains(tag)){
                tags.add(tag);
                any = true;
            }
        }
        if(any){
            rebuildTags.run();
        }
    }

    void tagsChanged(){
        rebuildTags.run();
        if(selectedTags.any()){
            rebuildPane.run();
        }

        Core.settings.putJson("schematic-tags", String.class, tags);
    }

    void addTag(Schematic s, String tag){
        s.labels.add(tag);
        s.save();
        tagsChanged();
    }

    void removeTag(Schematic s, String tag){
        s.labels.remove(tag);
        s.save();
        tagsChanged();
    }

    //shows a dialog for creating a new tag
    void showNewTag(Cons<String> result){
        ui.showTextInput("@schematic.addtag", "", "", out -> {
            if(tags.contains(out)){
                ui.showInfo("@schematic.tagexists");
            }else{
                tags.add(out);
                tagsChanged();
                result.get(out);
            }
        });
    }

    void showNewIconTag(Cons<String> cons){
        new Dialog(){{
            closeOnBack();
            setFillParent(true);

            cont.pane(t -> {
                resized(true, () -> {
                    t.clearChildren();
                    t.marginRight(19f);
                    t.defaults().size(48f);

                    int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

                    for(ContentType ctype : defaultContentIcons){
                        t.row();
                        t.image().colspan(cols).growX().width(Float.NEGATIVE_INFINITY).height(3f).color(Pal.accent);
                        t.row();

                        int i = 0;
                        for(UnlockableContent u : content.getBy(ctype).<UnlockableContent>as()){
                            if(!u.isHidden() && u.unlockedNow() && u.hasEmoji() && !tags.contains(u.emoji())){
                                t.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                                    String out = u.emoji() + "";

                                    tags.add(out);
                                    tagsChanged();
                                    cons.get(out);

                                    hide();
                                });

                                if(++i % cols == 0) t.row();
                            }
                        }
                    }
                });
            });
            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
        }}.show();
    }

    void showAllTags(){
        var dialog = new BaseDialog("@schematic.edittags");
        dialog.addCloseButton();
        Runnable[] rebuild = {null};
        dialog.cont.pane(p -> {
            rebuild[0] = () -> {
                p.clearChildren();
                p.defaults().fillX().left();

                float sum = 0f;
                Table current = new Table().left();

                for(var tag : tags){

                    var next = new Table(Tex.button, n -> {
                        n.add(tag).padRight(4);

                        n.add().growX();
                        n.defaults().size(30f);

                        //delete
                        n.button(Icon.cancelSmall, Styles.emptyi, () -> {
                            ui.showConfirm("@schematic.tagdelconfirm", () -> {
                                for(Schematic s : schematics.all()){
                                    if(s.labels.any()){
                                        s.labels.remove(tag);
                                        s.save();
                                    }
                                }
                                selectedTags.remove(tag);
                                tags.remove(tag);
                                tagsChanged();
                                rebuildPane.run();
                                rebuild[0].run();
                            });
                        });
                        //rename
                        n.button(Icon.pencilSmall, Styles.emptyi, () -> {
                            ui.showTextInput("@schematic.renametag", "@name", tag, result -> {
                                //same tag, nothing was renamed
                                if(result.equals(tag)) return;

                                if(tags.contains(result)){
                                    ui.showInfo("@schematic.tagexists");
                                }else{
                                    for(Schematic s : schematics.all()){
                                        if(s.labels.any()){
                                            s.labels.replace(tag, result);
                                            s.save();
                                        }
                                    }
                                    selectedTags.replace(tag, result);
                                    tags.replace(tag, result);
                                    tagsChanged();
                                    rebuild[0].run();
                                }
                            });
                        });
                        //move
                        n.button(Icon.upSmall, Styles.emptyi, () -> {
                            int idx = tags.indexOf(tag);
                            if(idx > 0){
                                tags.swap(idx, idx - 1);
                                tagsChanged();
                                rebuild[0].run();
                            }
                        });
                    });

                    next.pack();
                    float w = next.getPrefWidth() + Scl.scl(6f);

                    if(w + sum >= Core.graphics.getWidth() * (Core.graphics.isPortrait() ? 1f : 0.8f)){
                        p.add(current).row();
                        current = new Table();
                        current.left();
                        current.add(next).height(tagh).pad(2);
                        sum = 0;
                    }else{
                        current.add(next).height(tagh).pad(2);
                    }

                    sum += w;
                }

                if(sum > 0){
                    p.add(current).row();
                }

                p.table(t -> {
                    t.left().defaults().fillX().height(tagh).pad(2);

                    t.button("@schematic.texttag", Icon.add, () -> showNewTag(res -> rebuild[0].run())).wrapLabel(false).get().getLabelCell().padLeft(5);
                    t.button("@schematic.icontag", Icon.add, () -> showNewIconTag(res -> rebuild[0].run())).wrapLabel(false).get().getLabelCell().padLeft(5);
                });

            };

            resized(true, rebuild[0]);
        });
        dialog.show();
    }

    void buildTags(Schematic schem, Table t){
        buildTags(schem, t, true);
    }

    void buildTags(Schematic schem, Table t, boolean name){
        t.clearChildren();
        t.left();

        //sort by order in the main target array. the complexity of this is probably awful
        schem.labels.sort(s -> tags.indexOf(s));

        if(name) t.add("@schematic.tags").padRight(4);
        t.pane(s -> {
            s.left();
            s.defaults().pad(3).height(tagh);
            for(var tag : schem.labels){
                s.table(Tex.button, i -> {
                    i.add(tag).padRight(4).height(tagh).labelAlign(Align.center);
                    i.button(Icon.cancelSmall, Styles.emptyi, () -> {
                        removeTag(schem, tag);
                        buildTags(schem, t, name);
                    }).size(tagh).padRight(-9f).padLeft(-9f);
                });
            }

        }).fillX().left().height(tagh).scrollY(false);

        t.button(Icon.addSmall, () -> {
            var dialog = new BaseDialog("@schematic.addtag");
            dialog.addCloseButton();
            dialog.cont.pane(p -> resized(true, () -> {
                p.clearChildren();

                float sum = 0f;
                Table current = new Table().left();
                for(var tag : tags){
                    if(schem.labels.contains(tag)) continue;

                    var next = Elem.newButton(tag, () -> {
                        addTag(schem, tag);
                        buildTags(schem, t, name);
                        dialog.hide();
                    });
                    next.getLabel().setWrap(false);

                    next.pack();
                    float w = next.getPrefWidth() + Scl.scl(6f);

                    if(w + sum >= Core.graphics.getWidth() * (Core.graphics.isPortrait() ? 1f : 0.8f)){
                        p.add(current).row();
                        current = new Table();
                        current.left();
                        current.add(next).height(tagh).pad(2);
                        sum = 0;
                    }else{
                        current.add(next).height(tagh).pad(2);
                    }

                    sum += w;
                }

                if(sum > 0){
                    p.add(current).row();
                }

                Cons<String> handleTag = res -> {
                    dialog.hide();
                    addTag(schem, res);
                    buildTags(schem, t, name);
                };

                p.row();

                p.table(v -> {
                    v.left().defaults().fillX().height(tagh).pad(2);
                    v.button("@schematic.texttag", Icon.add, () -> showNewTag(handleTag)).wrapLabel(false).get().getLabelCell().padLeft(4);
                    v.button("@schematic.icontag", Icon.add, () -> showNewIconTag(handleTag)).wrapLabel(false).get().getLabelCell().padLeft(4);
                });
            }));
            dialog.show();
        }).size(tagh).tooltip("@schematic.addtag");
    }

    @Override
    public Dialog show(){
        super.show();

        if(Core.app.isDesktop() && searchField != null){
            Core.scene.setKeyboardFocus(searchField);
        }

        return this;
    }

    public static class SchematicImage extends Image{
        public float scaling = 16f;
        public float thickness = 4f;
        public Color borderColor = Pal.gray;

        private Schematic schematic;
        private Texture lastTexture;
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
            }else if(lastTexture != null && lastTexture.isDisposed()){
                set = wasSet = false;
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
            TextureRegionDrawable draw = new TextureRegionDrawable(new TextureRegion(lastTexture = schematics.getPreview(schematic)));
            setDrawable(draw);
            setScaling(Scaling.fit);
        }
    }

    public class SchematicInfoDialog extends BaseDialog{

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
            cont.table(tags -> buildTags(schem, tags)).fillX().left().row();
            cont.row();
            cont.add(new SchematicImage(schem)).maxSize(800f);
            cont.row();

            ItemSeq arr = schem.requirements();
            cont.table(r -> {
                int i = 0;
                for(ItemStack s : arr){
                    r.image(s.item.uiIcon).left().size(iconMed);
                    r.label(() -> {
                        Building core = player.core();
                        if(core == null || state.isMenu() || state.rules.infiniteResources || core.items.has(s.item, s.amount)) return "[lightgray]" + s.amount + "";
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
