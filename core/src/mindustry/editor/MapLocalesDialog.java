package mindustry.editor;

import arc.Core;
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
    private Table langs;
    private Table main;
    private String selectedLocale;
    private boolean applytoall = true;
    private boolean saved = true;

    public MapLocalesDialog(){
        super("@editor.locales");

        selectedLocale = Core.settings.getString("locale");

        langs = new Table(Tex.button);
        main = new Table();

        buttons.add().growX().width(-1);

        buttons.button("@back", Icon.left, () -> {
            if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> editor.tags.put("locales", JsonIO.write(locales)));
            hide();
        }).size(210f, 64f);
        closeOnBack(() -> {
            if(!saved) ui.showConfirm("@editor.locales", "@editor.savechanges", () -> editor.tags.put("locales", JsonIO.write(locales)));
        });

        buttons.button("@editor.apply", Icon.ok, () -> {
            editor.tags.put("locales", JsonIO.write(locales));
            saved = true;
        }).size(210f, 64f).disabled(b -> saved);
        buttons.add().growX().width(-1);

        buttons.button("?", () -> ui.showInfo("@locales.info")).size(60f, 64f);

        shown(this::setup);
    }

    public void show(MapLocales locales){
        this.locales = locales;
        saved = true;
        show();
    }

    private void setup(){
        cont.clear();

        buildTables();

        cont.add(langs).left();

        cont.table(t -> {
            t.image().color(Pal.gray).height(3f).fillX().row();
            t.add(main).padLeft(20f).center().grow().row();
            t.image().color(Pal.gray).height(3f).fillX().row();
            t.check("@locales.applytoall", applytoall, b -> {
                applytoall = b;
            }).padTop(10f);
        }).pad(10f).grow();

        // property addition
        cont.table(Tex.button, t -> {
            TextField name = t.field("name", s -> {}).maxTextLength(64).width(350f).padTop(10f).get();
            t.row();
            TextField value = t.area("text", s -> {}).maxTextLength(1000).size(350f, 140f).get();
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
            }).padTop(10f).size(350, 50f).fillX().row();
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
                    p.button(Icon.trash, Styles.flati, () -> ui.showConfirm("@confirm", "@locales.deletelocale", () -> {
                        locales.remove(name);

                        selectedLocale = (locales.size != 0 ? locales.keys().next() : Core.settings.getString("locale"));
                        saved = false;
                        buildTables();
                    })).size(50f).row();
                }
            }
        }).row();
        langs.button("@add", Icon.add, this::addLocaleDialog).padTop(10f).width(350f);
    }

    private void buildMain(){
        main.clear();

        StringMap props = locales.get(selectedLocale);
        if(props.size == 0){
            main.add("@empty").center().row();
        }else{
            main.pane(p -> {
                p.defaults().top();

                int cols = (Core.graphics.getWidth() - 380) / ((int)cardWidth + 10);
                Table[] colTables = new Table[cols];
                for(var i = 0; i < cols; i++){
                    colTables[i] = new Table();
                }

                int i = 0;
                // To sort properties in alphabetic order
                Seq<String> keys = props.keys().toSeq().sort();
                for(var key : keys){
                    colTables[i].table(Tex.button, t -> {
                        boolean[] shown = {false};
                        String[] propKey = {key};
                        String[] propValue = {props.get(key)};

                        // collapse button
                        t.button(Icon.downOpen, Styles.emptyTogglei, () -> shown[0] = !shown[0]).update(b -> {
                            b.replaceImage(new Image(shown[0] ? Icon.upOpen : Icon.downOpen));
                            b.setChecked(shown[0]);
                        }).size(35f);

                        // property name field
                        t.field(propKey[0], v -> {
                            if(applytoall){
                                for(var bundle : locales.values()){
                                    if(!bundle.containsKey(v)){
                                        bundle.remove(propKey[0]);
                                        bundle.put(v, propValue[0]);
                                    }
                                }
                            }else{
                                if(!props.containsKey(v)){
                                    props.remove(propKey[0]);
                                    props.put(v, propValue[0]);
                                }
                            }

                            propKey[0] = v;
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
                            propEditDialog(propKey[0], propValue[0]);
                        }).size(35f).row();

                        // property value area
                        t.collapser(c -> c.area(propValue[0], v -> {
                            props.put(propKey[0], v);
                            saved = false;
                        }).maxTextLength(1000).height(140f).growX(), () -> !shown[0]).colspan(4).growX();
                    }).top().width(cardWidth).pad(5f).row();

                    i = ++i % cols;
                }

                p.add(colTables);
            });
        }
    }

    private void propEditDialog(String key, String value){
        BaseDialog dialog = new BaseDialog("@edit");

        dialog.cont.pane(p -> {
            p.margin(10f);
            p.table(Tex.button, in -> {
                in.defaults().size(350f, 60f).left();

                in.button("@locales.addtoother", Icon.add, Styles.flatt, () -> {
                    for(var bundle : locales.values()){
                        if(!bundle.containsKey(key)){
                            bundle.put(key, value);
                        }
                    }

                    saved = false;
                    dialog.hide();
                }).marginLeft(12f).row();
            });
        });

        dialog.addCloseButton();
        dialog.show();
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
}
