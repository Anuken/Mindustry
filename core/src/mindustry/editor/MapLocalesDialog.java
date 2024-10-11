package mindustry.editor;

import arc.Core;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapLocalesDialog extends BaseDialog{
    /** Width of UI property card. */
    private static final float cardWidth = 400f;
    /** Style for filter options buttons */
    private static final TextButtonStyle filterStyle = new TextButtonStyle(){{
        up = down = checked = over = Tex.whitePane;
        font = Fonts.outline;
        fontColor = Color.lightGray;
        overFontColor = Pal.accent;
        disabledFontColor = Color.gray;
        disabled = Styles.black;
    }};
    /** Icons for use in map locales dialog. */
    private static final ContentType[] contentIcons = {ContentType.item, ContentType.block, ContentType.liquid, ContentType.status, ContentType.unit};

    private MapLocales locales;
    private MapLocales lastSaved;
    private boolean saved = true;
    private Table langs;
    private Table main;
    private Table propView;
    private String selectedLocale;

    private boolean applytoall = true;
    private boolean collapsed = false;
    private String searchString = "";
    private boolean searchByValue = false;
    private boolean showCorrect = true;
    private boolean showMissing = true;
    private boolean showSame = true;

    public MapLocalesDialog(){
        super("@editor.locales");

        selectedLocale = MapLocales.currentLocale();

        langs = new Table(Tex.button);
        main = new Table();
        propView = new Table();

        buttons.add("").uniform();

        buttons.table(t -> {
            t.defaults().pad(3).center();

            t.button("@back", Icon.left, () -> {
                if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> {
                    editor.tags.put("locales", JsonIO.write(locales));
                    state.mapLocales = locales;
                });
                hide();
            }).size(210f, 64f);
            closeOnBack(() -> {
                if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> {
                    editor.tags.put("locales", JsonIO.write(locales));
                    state.mapLocales = locales;
                });
            });

            t.button("@editor.apply", Icon.ok, () -> {
                editor.tags.put("locales", JsonIO.write(locales));
                state.mapLocales = locales;
                lastSaved = locales.copy();
                saved = true;
            }).size(210f, 64f).disabled(b -> saved);

            t.button("@edit", Icon.edit, this::editDialog).size(210f, 64f);
        }).growX();

        resized(this::buildMain);

        buttons.button("?", () -> ui.showInfo("@locales.info")).size(60f, 64f).uniform();

        shown(this::setup);
    }

    public void show(MapLocales locales){
        this.locales = locales;
        lastSaved = locales.copy();
        saved = true;
        show();
    }

    private void setup(){
        cont.clear();

        buildTables();

        cont.add(langs).left();

        cont.table(t -> {
            // search/collapse all/filter
            t.table(a -> {
                a.button(Icon.downOpen, Styles.emptyTogglei, () -> {
                    collapsed = !collapsed;
                    buildMain();
                }).update(b -> {
                    b.replaceImage(new Image(collapsed ? Icon.upOpen : Icon.downOpen));
                    b.setChecked(collapsed);
                }).size(35f);

                a.button(Icon.filter, Styles.emptyi, () -> filterDialog(this::buildMain)).padLeft(10f).size(35f);

                var field = a.field("", v -> {
                    searchString = v;
                    buildMain();
                }).update(f -> f.setText(searchString)).maxTextLength(64).padLeft(10f).width(250f).update(f -> f.setMessageText(searchByValue ? "@locales.searchvalue": "@locales.searchname")).get();

                a.button(Icon.cancel, Styles.emptyi, () -> {
                    searchString = "";
                    field.setText("");
                    buildMain();
                }).padLeft(10f).size(35f);
            }).row();

            t.check("@locales.applytoall", applytoall, b -> applytoall = b).pad(10f).row();

            t.add(main).center().grow().row();
        }).pad(10f).grow();

        // property addition
        cont.table(Tex.button, t -> {
            TextField name = t.field("name", s -> {}).maxTextLength(64).fillX().padTop(10f).get();
            t.row();
            TextField value = t.area("text", s -> {}).maxTextLength(1000).fillX().height(140f).get();
            t.row();

            t.button("@add", Icon.add, () -> {
                if(applytoall){
                    for(var locale : locales.values()){
                        locale.put(name.getText(), value.getText());
                    }
                }else{
                    locales.get(selectedLocale).put(name.getText(), value.getText());
                }

                saved = false;
                buildMain();
            }).padTop(10f).size(cardWidth, 50f).fillX().row();
        }).right();
    }

    private void buildTables(){
        if(!locales.containsKey(selectedLocale)){
            locales.put(selectedLocale, new StringMap());
        }

        buildLocalesTable();
        buildMain();
    }

    private void buildLocalesTable(){
        langs.clear();

        langs.pane(p -> {
            for(var loc : Vars.locales){
                String name = loc.toString();

                if(locales.containsKey(name)){
                    p.button(loc.getDisplayName(Core.bundle.getLocale()), Styles.flatTogglet, () -> {
                        if(name.equals(selectedLocale)) return;

                        selectedLocale = name;
                        buildTables();
                    }).update(b -> b.setChecked(selectedLocale.equals(name))).width(200f).minHeight(50f);
                    p.button(Icon.edit, Styles.flati, () -> localeEditDialog(name)).size(50f);
                    p.button(Icon.trash, Styles.flati, () -> ui.showConfirm("@confirm", "@locales.deletelocale", () -> {
                        locales.remove(name);

                        selectedLocale = (locales.size != 0 ? locales.keys().next() : Core.settings.getString("locale"));
                        saved = false;
                        buildTables();
                    })).size(50f).row();
                }
            }
        }).row();
        langs.button("@add", Icon.add, this::addLocaleDialog).padTop(10f).width(250f);
    }

    private void buildMain(){
        main.clear();

        StringMap props = locales.get(selectedLocale);

        main.image().color(Pal.gray).height(3f).growX().expandY().top().row();
        main.pane(p -> {
            int cols = Math.max(1, (int)((Core.graphics.getWidth() / Scl.scl() - 410f) / cardWidth) - 1);
            if(props.size == 0){
                main.add("@empty").center().row();
                return;
            }
            p.defaults().top();

            Table[] colTables = new Table[cols];
            for(var i = 0; i < cols; i++){
                colTables[i] = new Table();
            }
            int i = 0;

            // To sort properties in alphabetic order
            Seq<String> keys = props.keys().toSeq().sort();

            for(var key : keys){
                var comparsionString = (searchByValue ? props.get(key).toLowerCase() : key.toLowerCase());
                if(!searchString.isEmpty() && !comparsionString.contains(searchString.toLowerCase())) continue;

                PropertyStatus status = getPropertyStatus(key, props.get(key), selectedLocale, false);
                if(status == PropertyStatus.correct && !showCorrect) continue;
                if(status == PropertyStatus.missing && !showMissing) continue;
                if(status == PropertyStatus.same && !showSame) continue;

                colTables[i].table(Tex.whitePane, t -> {
                    boolean[] shown = {!collapsed};
                    String[] propKey = {key};
                    String[] propValue = {props.get(key)};

                    // collapse button
                    t.button(Icon.downOpen, Styles.emptyTogglei, () -> shown[0] = !shown[0]).update(b -> {
                        b.replaceImage(new Image(shown[0] ? Icon.upOpen : Icon.downOpen));
                        b.setChecked(shown[0]);
                    }).size(35f);

                    // property name field
                    t.field(propKey[0], (f, c) -> c != '=' && c != ':', v -> {
                        if(props.containsKey(v)){
                            t.setColor(Color.valueOf("f25555"));
                            return;
                        }

                        if(applytoall){
                            for(var bundle : locales.values()){
                                if(!bundle.containsKey(v)){
                                    String value = bundle.get(propKey[0]);
                                    if(value == null) continue;

                                    bundle.remove(propKey[0]);
                                    bundle.put(v, value);
                                }
                            }
                        }else{
                            if(!props.containsKey(v)){
                                props.remove(propKey[0]);
                                props.put(v, propValue[0]);
                            }
                        }

                        propKey[0] = v;
                        updateCard(t, v, propValue[0]);
                        saved = false;
                    }).maxTextLength(64).width(cardWidth - 125f);

                    // remove button
                    t.button(Icon.trash, Styles.emptyi, () -> {
                        if(applytoall){
                            for(var bundle : locales.values()){
                                bundle.remove(propKey[0]);
                            }
                        }else{
                            props.remove(propKey[0]);
                        }
                        saved = false;
                        buildMain();
                    }).size(35f);

                    // more actions
                    t.button(Icon.edit, Styles.emptyi, () -> propEditDialog(t, propKey[0], propValue[0])).size(35f).row();

                    // property value area
                    t.collapser(c -> c.area(propValue[0], v -> {
                        props.put(propKey[0], v);
                        updateCard(t, propKey[0], v);
                        saved = false;
                    }).maxTextLength(1000).height(140f).update(a -> {
                        propValue[0] = props.get(propKey[0]);
                        a.setText(props.get(propKey[0]));
                    }).growX(), () -> shown[0]).colspan(4).growX();

                    updateCard(t, propKey[0], propValue[0]);
                }).top().width(cardWidth).pad(5f).row();

                i = ++i % cols;
            }

            if(!colTables[0].hasChildren()){
                main.add("@empty").center().row();
            }else{
                p.add(colTables);
            }
        }).growX().row();
        main.image().color(Pal.gray).height(3f).growX().expandY().bottom().row();
    }

    private void updateCard(Table table, String propKey, String propValue){
        updateCard(table, propKey, propValue, selectedLocale, false);
    }

    private void updateCard(Table table, String propKey, String propValue, String locale, boolean viewCard){
        switch(getPropertyStatus(propKey, propValue, locale, viewCard)){
            case missing -> table.setColor(Pal.accent);
            case same -> table.setColor(Pal.techBlue);
            case correct -> table.setColor(Pal.gray);
        }
    }

    // Property statuses for main dialog and property view dialog are a bit different
    private PropertyStatus getPropertyStatus(String propKey, String propValue, String locale, boolean forView){
        if(forView && propValue == null) return PropertyStatus.missing;

        for(var bundle : locales.entries()){
            if(!forView && bundle.key.equals(selectedLocale)) continue;
            if(forView && bundle.key.equals(locale)) continue;

            StringMap props = bundle.value;

            if(!props.containsKey(propKey)){
                if(!forView) return PropertyStatus.missing;
            }else{
                if(props.get(propKey).equals(propValue)){
                    return PropertyStatus.same;
                }
            }
        }

        return PropertyStatus.correct;
    }

    private void addLocaleDialog(){
        BaseDialog dialog = new BaseDialog("@add");

        dialog.cont.pane(t -> {
            for(var loc : Vars.locales){
                String name = loc.toString();

                if(!locales.containsKey(name)){
                    t.button(loc.getDisplayName(Core.bundle.getLocale()), Styles.flatTogglet, () -> {
                        if(name.equals(selectedLocale)) return;

                        locales.put(name, new StringMap());

                        selectedLocale = name;
                        saved = false;
                        buildTables();
                        dialog.hide();
                    }).update(b -> b.setChecked(selectedLocale.equals(name))).size(400f, 50f).row();
                }
            }
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void propEditDialog(Table card, String key, String value){
        BaseDialog dialog = new BaseDialog("@edit");

        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                t.defaults().size(450f, 60f).left();

                t.button("@locales.addtoother", Icon.add, Styles.flatt, () -> {
                    for(var bundle : locales.values()){
                        if(!bundle.containsKey(key)){
                            bundle.put(key, value);
                        }
                    }

                    saved = false;
                    updateCard(card, key, value);
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@locales.viewproperty", Icon.zoom, Styles.flatt, () -> {
                    viewPropertyDialog(key);
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@locales.addicon", Icon.image, Styles.flatt, () -> {
                    addIconDialog(res -> {
                        locales.get(selectedLocale).put(key, value + res);
                        saved = false;
                    });
                    dialog.hide();
                }).marginLeft(12f).row();

                t.button("@locales.rollback", Icon.undo, Styles.flatt, () -> {
                    locales.get(selectedLocale).put(key, lastSaved.get(selectedLocale).get(key));
                    buildTables();
                    dialog.hide();
                }).disabled(b -> {
                    if(!lastSaved.containsKey(selectedLocale)) return true;
                    StringMap savedMap = lastSaved.get(selectedLocale);
                    return !savedMap.containsKey(key) || savedMap.get(key).equals(locales.get(selectedLocale).get(key));
                }).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void localeEditDialog(String locale){
        BaseDialog dialog = new BaseDialog("@edit");

        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                t.defaults().size(350f, 60f).left();

                t.button("@waves.copy", Icon.copy, Styles.flatt, () -> {
                    Core.app.setClipboardText(writeLocale(locale));
                    ui.showInfoFade("@copied");
                    dialog.hide();
                }).marginLeft(12f).row();
                t.button("@waves.load", Icon.download, Styles.flatt, () -> {
                    locales.put(locale, readLocale(Core.app.getClipboardText()));
                    buildTables();
                    saved = false;
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void editDialog(){
        BaseDialog dialog = new BaseDialog("@edit");

        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, t -> {
                t.defaults().size(450f, 60f).left();

                t.button("@waves.copy", Icon.copy, Styles.flatt, () -> {
                    Core.app.setClipboardText(writeBundles());
                    ui.showInfoFade("@copied");
                    dialog.hide();
                }).marginLeft(12f).row();
                t.button("@waves.load", Icon.download, Styles.flatt, () -> {
                    locales = readBundles(Core.app.getClipboardText());
                    buildTables();
                    saved = false;
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null).marginLeft(12f).row();
                t.button("@locales.rollback", Icon.undo, Styles.flatt, () -> {
                    locales = lastSaved.copy();
                    saved = true;
                    buildTables();
                    dialog.hide();
                }).disabled(b -> saved).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void viewPropertyDialog(String key){
        BaseDialog dialog = new BaseDialog(Core.bundle.format("locales.viewing", key));

        dialog.cont.table(t -> {
            t.button(Icon.filter, Styles.emptyi, () -> filterDialog(() -> buildPropView(key))).size(35f);

            var field = t.field(searchString, v -> {
                searchString = v;
                buildPropView(key);
            }).update(f -> f.setText(searchString)).maxTextLength(64).padLeft(10f).width(250f).update(f -> f.setMessageText(searchByValue ? "@locales.searchvalue" : "@locales.searchlocale")).get();

            t.button(Icon.cancel, Styles.emptyi, () -> {
                searchString = "";
                field.setText("");
                buildPropView(key);
            }).padLeft(10f).size(35f);
        }).row();

        buildPropView(key);
        dialog.cont.add(propView).grow().center().row();

        dialog.addCloseButton();
        dialog.closeOnBack();
        dialog.hidden(this::buildMain);

        dialog.show();
    }

    private void buildPropView(String key){
        propView.clear();

        propView.image().color(Pal.gray).height(3f).fillX().top().row();
        propView.pane(p -> {
            int cols = Math.max(1, (int)((Core.graphics.getWidth() / Scl.scl() - 100f) / cardWidth));
            if(cols == 0){
                propView.add("@empty").center().row();
                return;
            }
            p.defaults().top();

            Table[] colTables = new Table[cols];
            for(var i = 0; i < cols; i++){
                colTables[i] = new Table();
            }
            int i = 0;

            for(var loc : Vars.locales){
                String name = loc.toString();
                if(!locales.containsKey(name)) continue;

                PropertyStatus status = getPropertyStatus(key, locales.get(name).get(key), name, true);
                if(status == PropertyStatus.correct && !showCorrect) continue;
                if(status == PropertyStatus.missing && !showMissing) continue;
                if(status == PropertyStatus.same && !showSame) continue;

                if(status != PropertyStatus.missing){
                    var comparsionString = (searchByValue ? locales.get(name).get(key).toLowerCase() : loc.getDisplayName(Core.bundle.getLocale()).toLowerCase());
                    if(!searchString.isEmpty() && !comparsionString.contains(searchString.toLowerCase())) continue;
                }

                colTables[i].table(Tex.whitePane, t -> {
                    t.add(loc.getDisplayName(Core.bundle.getLocale())).left().color(Pal.accent).row();
                    t.image().color(Pal.accent).fillX().row();

                    if(status == PropertyStatus.missing){
                        t.table(b ->
                        b.button("@add", Icon.add, () -> {
                            locales.get(name).put(key, "moai");

                            t.getCells().get(2).clearElement();
                            t.getCells().remove(2);

                            t.area(locales.get(name).get(key), v -> {
                                locales.get(name).put(key, v);
                                saved = false;
                            }).maxTextLength(1000).height(140f).growX().row();
                        }).size(160f, 50f)).height(140f).growX().row();
                    }else{
                        t.area(locales.get(name).get(key), v -> {
                            locales.get(name).put(key, v);
                            saved = false;
                        }).maxTextLength(1000).height(140f).growX().row();
                    }
                }).update(t -> updateCard(t, key, locales.get(name).get(key), name, true)).top().width(cardWidth).pad(5f).row();

                i = ++i % cols;
            }

            if(!colTables[0].hasChildren()){
                propView.add("@empty").center().row();
            }else{
                p.add(colTables);
            }
        }).grow().row();
        propView.image().color(Pal.gray).height(3f).fillX().bottom().row();
    }

    private void filterDialog(Runnable hidden){
        BaseDialog dialog = new BaseDialog("@locales.filter");

        dialog.cont.table(t -> {
            t.add("@search").row();
            t.table(b -> {
                b.button("@locales.byname", Styles.togglet, () -> searchByValue = false).size(300f, 50f).checked(v -> !searchByValue);
                b.button("@locales.byvalue", Styles.togglet, () -> searchByValue = true).padLeft(10f).size(300f, 50f).checked(v -> searchByValue);
            }).padTop(5f);
        }).row();

        dialog.cont.button("@locales.showcorrect", Icon.ok, filterStyle, () -> showCorrect = !showCorrect).update(b -> {
            ((Image)b.getChildren().get(1)).setDrawable(showCorrect ? Icon.ok : Icon.cancel);
            b.setChecked(showCorrect);
        }).size(450f, 100f).color(Pal.gray).padTop(65f);

        dialog.cont.row();

        dialog.cont.button("@locales.showmissing", Icon.ok, filterStyle, () -> showMissing = !showMissing).update(b -> {
            ((Image)b.getChildren().get(1)).setDrawable(showMissing ? Icon.ok : Icon.cancel);
            b.setChecked(showMissing);
        }).size(450f, 100f).color(Pal.accent).padTop(65f);

        dialog.cont.row();

        dialog.cont.button("@locales.showsame", Icon.ok, filterStyle, () -> showSame = !showSame).update(b -> {
            ((Image)b.getChildren().get(1)).setDrawable(showSame ? Icon.ok : Icon.cancel);
            b.setChecked(showSame);
        }).size(450f, 100f).color(Pal.techBlue).padTop(65f);

        dialog.buttons.button("@back", Icon.left, () -> {
            hidden.run();
            dialog.hide();
        }).size(210f, 64f);
        dialog.closeOnBack(hidden);

        dialog.show();
    }

    private void addIconDialog(Cons<String> cons){
        BaseDialog dialog = new BaseDialog("@locales.addicon");

        Table icons = new Table();
        TextField search = Elem.newField("", v -> iconsTable(icons, v.replace(" ", "").toLowerCase(), dialog, cons));
        search.setMessageText("@search");

        dialog.cont.table(t -> {
            t.add(search).maxTextLength(64).padLeft(10f).width(250f);

            t.button(Icon.cancel, Styles.emptyi, () -> {
                search.setText("");
                iconsTable(icons, "", dialog, cons);
            }).padLeft(10f).size(35f);
        }).row();

        dialog.cont.pane(icons).scrollX(false);
        dialog.resized(true, () -> iconsTable(icons, search.getText().replace(" ", "").toLowerCase(), dialog, cons));

        dialog.addCloseButton();
        dialog.closeOnBack();
        dialog.setFillParent(true);
        dialog.show();
    }

    private void iconsTable(Table table, String search, Dialog dialog, Cons<String> cons){
        table.clear();

        table.marginRight(19f).marginLeft(12f);
        table.defaults().size(48f);

        int cols = (int)Math.min(20, Core.graphics.getWidth() / Scl.scl(52f));

        int i = 0;

        var codes = new ObjectIntMap<>(Iconc.codes);

        for(var name : codes.keys()){
            if(!name.toLowerCase().contains(search)) codes.remove(name);
        }

        if(codes.size > 0) table.image().colspan(cols).growX().width(-1f).height(3f).color(Pal.accent).row();

        for(var icon : codes){
            String res = (char)icon.value + "";

            table.button(Icon.icons.get(icon.key), Styles.flati, iconMed, () -> {
                cons.get(res);
                dialog.hide();
            }).tooltip(icon.key);

            if(++i % cols == 0) table.row();
        }

        for(ContentType ctype : contentIcons){
            var all = content.getBy(ctype).<UnlockableContent>as().select(u -> u.localizedName.replace(" ", "").toLowerCase().contains(search) && u.uiIcon.found());

            table.row();
            if(all.size > 0) table.image().colspan(cols).growX().width(-1f).height(3f).color(Pal.accent).row();

            i = 0;
            for(UnlockableContent u : all){
                table.button(new TextureRegionDrawable(u.uiIcon), Styles.flati, iconMed, () -> {
                    cons.get(u.emoji() + "");
                    dialog.hide();
                }).tooltip(u.localizedName);

                if(++i % cols == 0) table.row();
            }
        }

        var teams = new Seq<>(Team.baseTeams);
        teams = teams.select(u -> u.localized().toLowerCase().contains(search) && Core.atlas.has("team-" + u.name));

        table.row();
        if(teams.size > 0) table.image().colspan(cols).growX().width(-1f).height(3f).color(Pal.accent).row();

        for(Team team : teams){
            var region = Core.atlas.find("team-" + team.name);

            table.button(new TextureRegionDrawable(region), Styles.flati, iconMed, () -> {
                cons.get(team.emoji);
                dialog.hide();
            }).tooltip(team.localized());

            if(++i % cols == 0) table.row();
        }
    }

    private String writeBundles(){
        StringBuilder data = new StringBuilder();

        for(var locale : locales.keys()){
            data.append(locale).append(":\n").append(writeLocale(locale));
        }

        return data.toString();
    }

    private String writeLocale(String key){
        StringBuilder data = new StringBuilder();

        if(!locales.containsKey(key)) return "";

        for(var prop : locales.get(key).entries()){
            // Convert \n in plain text to \\n, then convert newlines to \n
            data.append(prop.key).append(" = ").append(prop.value
            .replace("\\n", "\\\\n").replace("\n", "\\n")).append("\n");
        }

        return data.toString();
    }

    private MapLocales readBundles(String data){
        MapLocales bundles = new MapLocales();

        String currentLocale = "";

        for(var line : data.split("\\r?\\n|\\r")){
            if(line.endsWith(":") && !line.contains("=")){
                currentLocale = line.substring(0, line.length() - 1);
                bundles.put(currentLocale, new StringMap());
            }else{
                int sepIndex = line.indexOf(" = ");
                if(sepIndex != -1 && !currentLocale.isEmpty()){
                    // Convert \n in file to newlines in text, then revert newlines with escape characters
                    bundles.get(currentLocale).put(line.substring(0, sepIndex), line.substring(sepIndex + 3)
                    .replace("\\n", "\n").replace("\\\n", "\\n"));
                }
            }
        }

        return bundles;
    }

    private StringMap readLocale(String data){
        StringMap map = new StringMap();

        for(var line : data.split("\\r?\\n|\\r")){
            int sepIndex = line.indexOf(" = ");
            if(sepIndex != -1){
                // Convert \n in file to newlines in text, then revert newlines with escape characters
                map.put(line.substring(0, sepIndex), line.substring(sepIndex + 3)
                .replace("\\n", "\n").replace("\\\n", "\\n"));
            }
        }

        return map;
    }

    private enum PropertyStatus{
        correct,
        missing,
        same
    }
}
