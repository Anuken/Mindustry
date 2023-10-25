package mindustry.editor;

import arc.Core;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapLocalesDialog extends BaseDialog{
    /** Width of UI property card */
    private static final float cardWidth = 400f;

    private MapLocales locales;
    private MapLocales lastSaved;
    private boolean saved = true;
    private Table langs;
    private Table main;
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

        selectedLocale = Core.settings.getString("locale");

        langs = new Table(Tex.button);
        main = new Table();

        buttons.add("").uniform();

        buttons.table(t -> {
            t.defaults().pad(3).center();

            t.button("@back", Icon.left, () -> {
                if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> editor.tags.put("locales", JsonIO.write(locales)));
                hide();
            }).size(210f, 64f);
            closeOnBack(() -> {
                if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> editor.tags.put("locales", JsonIO.write(locales)));
            });

            t.button("@editor.apply", Icon.ok, () -> {
                editor.tags.put("locales", JsonIO.write(locales));
                lastSaved = locales.copy();
                saved = true;
            }).size(210f, 64f).disabled(b -> saved);

            t.button("@edit", Icon.edit, this::editDialog).size(210f, 64f);
        }).growX();

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

                a.field("", v -> {
                    searchString = v;
                    buildMain();
                }).update(f -> f.setText(searchString)).maxTextLength(64).padLeft(10f).width(250f).update(f -> f.setMessageText(searchByValue ? "@locales.searchvalue": "@locales.searchname"));

                a.button(Icon.cancel, Styles.emptyi, () -> {
                    searchString = "";
                    buildMain();
                }).padLeft(10f).size(35f);

                a.button(Icon.filter, Styles.emptyi, this::filterDialog).padLeft(10f).size(35f);;
            }).row();

            t.check("@locales.applytoall", applytoall, b -> applytoall = b).pad(10f).row();

            t.add(main).padLeft(20f).center().grow().row();
        }).pad(10f).grow();

        // property addition
        cont.table(Tex.button, t -> {
            TextField name = t.field("name", s -> {
            }).maxTextLength(64).fillX().padTop(10f).get();
            t.row();
            TextField value = t.area("text", s -> {
            }).maxTextLength(1000).fillX().height(140f).get();
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
            }).padTop(10f).size(400f, 50f).fillX().row();
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
                    p.button(loc.getDisplayName(), Styles.flatTogglet, () -> {
                        if(name.equals(selectedLocale)) return;

                        selectedLocale = name;
                        buildTables();
                    }).update(b -> b.setChecked(selectedLocale.equals(name))).size(300f, 50f);
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
        langs.button("@add", Icon.add, this::addLocaleDialog).padTop(10f).width(400f);
    }

    private void buildMain(){
        main.clear();

        StringMap props = locales.get(selectedLocale);

        main.image().color(Pal.gray).height(3f).growX().expandY().top().row();
        main.pane(p -> {
            int cols = (Core.graphics.getWidth() - 380) / ((int)cardWidth + 10);
            if(props.size == 0 || cols == 0){
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

                PropertyStatus status = getPropertyStatus(key, props.get(key));
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
                    t.button(Icon.edit, Styles.emptyi, () -> {
                        propEditDialog(t, propKey[0], propValue[0]);
                    }).size(35f).row();

                    // property value area
                    t.collapser(c -> c.area(propValue[0], v -> {
                        props.put(propKey[0], v);
                        updateCard(t, propKey[0], v);
                        saved = false;
                    }).maxTextLength(1000).height(140f).growX(), () -> shown[0]).colspan(4).growX();

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
        switch(getPropertyStatus(propKey, propValue)){
            case missing -> table.setColor(Pal.accent);
            case same -> table.setColor(Pal.techBlue);
            case correct -> table.setColor(Pal.gray);
        }
    }

    private PropertyStatus getPropertyStatus(String propKey, String propValue){
        for(var bundle : locales.entries()){
            if(bundle.key.equals(selectedLocale)) continue;

            StringMap props = bundle.value;

            if(!props.containsKey(propKey)){
                return PropertyStatus.missing;
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
                    t.button(loc.getDisplayName(), Styles.flatTogglet, () -> {
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
                t.defaults().size(350f, 60f).left();

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

                t.button("@waves.copy", Icon.add, Styles.flatt, () -> {
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

                t.button("@waves.copy", Icon.add, Styles.flatt, () -> {
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
                }).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void filterDialog(){
        BaseDialog dialog = new BaseDialog("@locales.filter");

        dialog.cont.table(t -> {
            t.add("@search").row();
            t.table(b -> {
                b.button("@locales.byname", Styles.togglet, () -> searchByValue = false).size(300f, 50f).checked(v -> !searchByValue);
                b.button("@locales.byvalue", Styles.togglet, () -> searchByValue = true).padLeft(10f).size(300f, 50f).checked(v -> searchByValue);
            }).padTop(5f);
        }).row();

        dialog.cont.table(Tex.whitePane, t ->
        t.button("@locales.showcorrect", Icon.ok, Styles.nonet, () -> showCorrect = !showCorrect).update(b -> {
            ((Image)b.getChildren().get(1)).setDrawable(showCorrect ? Icon.ok : Icon.cancel);
            b.setChecked(showCorrect);
        }).grow().pad(15f)).size(450f, 100f).color(Pal.gray).padTop(50f);

        dialog.cont.row();

        dialog.cont.table(Tex.whitePane, t ->
            t.button("@locales.showmissing", Icon.ok, Styles.nonet, () -> showMissing = !showMissing).update(b -> {
                ((Image)b.getChildren().get(1)).setDrawable(showMissing ? Icon.ok : Icon.cancel);
                b.setChecked(showMissing);
        }).grow().pad(15f)).size(450f, 100f).color(Pal.accent).padTop(50f);

        dialog.cont.row();

        dialog.cont.table(Tex.whitePane, t ->
            t.button("@locales.showsame", Icon.ok, Styles.nonet, () -> showSame = !showSame).update(b -> {
                ((Image)b.getChildren().get(1)).setDrawable(showSame ? Icon.ok : Icon.cancel);
                b.setChecked(showSame);
        }).grow().pad(15f)).size(450f, 100f).color(Pal.techBlue).padTop(50f);

        dialog.buttons.button("@back", Icon.left, () -> {
            buildMain();
            dialog.hide();
        }).size(210f, 64f);
        dialog.closeOnBack(this::buildMain);

        dialog.show();
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
            data.append(prop.key).append(" = ").append(prop.value).append("\n");
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
                    bundles.get(currentLocale).put(line.substring(0, sepIndex), line.substring(sepIndex + 2));
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
                map.put(line.substring(0, sepIndex), line.substring(sepIndex + 2));
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
