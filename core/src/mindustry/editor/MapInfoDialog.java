package mindustry.editor;

import arc.*;
import arc.scene.ui.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapInfoDialog extends BaseDialog{
    private final WaveInfoDialog waveInfo;
    private final MapGenerateDialog generate;
    private final CustomRulesDialog ruleInfo = new CustomRulesDialog();

    public MapInfoDialog(){
        super("@editor.mapinfo");
        this.waveInfo = new WaveInfoDialog();
        this.generate = new MapGenerateDialog(false);

        addCloseButton();

        shown(this::setup);
    }

    private void setup(){
        cont.clear();

        ObjectMap<String, String> tags = editor.tags;
        
        cont.pane(t -> {
            t.add("@editor.mapname").padRight(8).left();
            t.defaults().padTop(15);

            TextField name = t.field(tags.get("name", ""), text -> {
                tags.put("name", text);
            }).size(400, 55f).maxTextLength(50).get();
            name.setMessageText("@unknown");

            t.row();
            t.add("@editor.description").padRight(8).left();

            TextArea description = t.area(tags.get("description", ""), Styles.areaField, text -> {
                tags.put("description", text);
            }).size(400f, 140f).maxTextLength(1000).get();

            t.row();
            t.add("@editor.author").padRight(8).left();

            TextField author = t.field(tags.get("author", Core.settings.getString("mapAuthor", "")), text -> {
                tags.put("author", text);
                Core.settings.put("mapAuthor", text);
            }).size(400, 55f).maxTextLength(50).get();
            author.setMessageText("@unknown");

            t.row();
            t.add("@editor.rules").padRight(8).left();
            t.button("@edit", () -> {
                ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules());
                hide();
            }).left().width(200f);

            t.row();
            t.add("@editor.waves").padRight(8).left();
            t.button("@edit", () -> {
                waveInfo.show();
                hide();
            }).left().width(200f);

            t.row();
            t.add("@editor.generation").padRight(8).left();
            t.button("@edit", () -> {
                //randomize so they're not all the same seed
                var res = maps.readFilters(editor.tags.get("genfilters", ""));
                res.each(GenerateFilter::randomize);

                generate.show(res,
                filters -> {
                    //reset seed to 0 so it is not written
                    filters.each(f -> f.seed = 0);
                    editor.tags.put("genfilters", JsonIO.write(filters));
                });
                hide();
            }).left().width(200f);

            name.change();
            description.change();
            author.change();

            t.margin(16f);
        });
    }
}
