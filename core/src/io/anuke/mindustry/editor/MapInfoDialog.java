package io.anuke.mindustry.editor;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.dialogs.*;

public class MapInfoDialog extends FloatingDialog{
    private final MapEditor editor;
    private final WaveInfoDialog waveInfo;
    private final MapGenerateDialog generate;
    private final CustomRulesDialog ruleInfo = new CustomRulesDialog();

    public MapInfoDialog(MapEditor editor){
        super("$editor.mapinfo");
        this.editor = editor;
        this.waveInfo = new WaveInfoDialog(editor);
        this.generate = new MapGenerateDialog(editor, false);

        addCloseButton();

        shown(this::setup);
    }

    private void setup(){
        cont.clear();

        ObjectMap<String, String> tags = editor.getTags();
        
        cont.pane(t -> {
            t.add("$editor.name").padRight(8).left();
            t.defaults().padTop(15);

            TextField name = t.addField(tags.get("name", ""), text -> {
                tags.put("name", text);
            }).size(400, 55f).get();
            name.setMessageText("$unknown");

            t.row();
            t.add("$editor.description").padRight(8).left();

            TextArea description = t.addArea(tags.get("description", ""), Styles.areaField, text -> {
                tags.put("description", text);
            }).size(400f, 140f).get();

            t.row();
            t.add("$editor.author").padRight(8).left();

            TextField author = t.addField(tags.get("author", Core.settings.getString("mapAuthor", "")), text -> {
                tags.put("author", text);
                Core.settings.put("mapAuthor", text);
                Core.settings.save();
            }).size(400, 55f).get();
            author.setMessageText("$unknown");

            t.row();
            t.add("$editor.rules").padRight(8).left();
            t.addButton("$edit", () -> {
                ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules());
                hide();
            }).left().width(200f);

            t.row();
            t.add("$editor.waves").padRight(8).left();
            t.addButton("$edit", () -> {
                waveInfo.show();
                hide();
            }).left().width(200f);

            t.row();
            t.add("$editor.generation").padRight(8).left();
            t.addButton("$edit", () -> {
                generate.show(Vars.maps.readFilters(editor.getTags().get("genfilters", "")),
                filters -> editor.getTags().put("genfilters", JsonIO.write(filters)));
                hide();
            }).left().width(200f);

            name.change();
            description.change();
            author.change();

            Vars.platform.addDialog(name, 50);
            Vars.platform.addDialog(author, 50);
            Vars.platform.addDialog(description, 1000);
            t.margin(16f);
        });
    }
}
