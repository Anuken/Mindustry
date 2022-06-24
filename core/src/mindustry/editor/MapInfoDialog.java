package mindustry.editor;

import arc.scene.ui.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapInfoDialog extends BaseDialog{
    private final WaveInfoDialog waveInfo;
    private final MapGenerateDialog generate;
    private final CustomRulesDialog ruleInfo = new CustomRulesDialog();
    private final MapObjectivesDialog objectives = new MapObjectivesDialog();

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

            TextField author = t.field(tags.get("author", ""), text -> {
                tags.put("author", text);
            }).size(400, 55f).maxTextLength(50).get();
            author.setMessageText("@unknown");

            t.row();

            t.table(Tex.button, r -> {
                r.defaults().width(230f).height(60f);

                var style = Styles.flatt;

                r.button("@editor.rules", Icon.list, style, () -> {
                    ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules());
                    hide();
                }).marginLeft(10f);

                r.button("@editor.waves", Icon.units, style, () -> {
                    waveInfo.show();
                    hide();
                }).marginLeft(10f);

                r.row();

                r.button("@editor.objectives", Icon.info, style, () -> {
                    objectives.show(state.rules.objectives);
                    hide();
                }).marginLeft(10f);

                r.button("@editor.generation", Icon.terrain, style, () -> {
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
                }).marginLeft(10f);
            }).colspan(2).center();

            name.change();
            description.change();
            author.change();

            t.margin(16f);
        });
    }
}
