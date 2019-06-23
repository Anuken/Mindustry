package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.scene.ui.TextArea;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.ui.dialogs.CustomRulesDialog;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

public class MapInfoDialog extends FloatingDialog{
    private final MapEditor editor;
    private final WaveInfoDialog waveInfo;
    private final CustomRulesDialog ruleInfo = new CustomRulesDialog();

    public MapInfoDialog(MapEditor editor){
        super("$editor.mapinfo");
        this.editor = editor;
        this.waveInfo = new WaveInfoDialog(editor);

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

            TextArea description = t.addArea(tags.get("description", ""), "textarea", text -> {
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
            t.addButton("$edit", () -> ruleInfo.show(Vars.state.rules, () -> Vars.state.rules = new Rules())).left().width(200f);

            t.row();
            t.add("$editor.waves").padRight(8).left();
            t.addButton("$edit", waveInfo::show).left().width(200f);

            name.change();
            description.change();
            author.change();

            Platform.instance.addDialog(name, 50);
            Platform.instance.addDialog(author, 50);
            Platform.instance.addDialog(description, 1000);
            t.margin(16f);
        });
    }
}
