package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.scene.ui.TextArea;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

public class MapInfoDialog extends FloatingDialog{
    private final MapEditor editor;

    private TextArea description;
    private TextField author;
    private TextField name;

    public MapInfoDialog(MapEditor editor){
        super("$editor.mapinfo");
        this.editor = editor;

        addCloseButton();

        shown(this::setup);

        hidden(() -> {

        });
    }

    private void setup(){
        cont.clear();

        ObjectMap<String, String> tags = editor.getTags();

        cont.add("$editor.name").padRight(8).left();

        cont.defaults().padTop(15);

        name = cont.addField(tags.get("name", ""), text -> {
            tags.put("name", text);
        }).size(400, 55f).get();
        name.setMessageText("$unknown");

        cont.row();

        cont.add("$editor.description").padRight(8).left();

        description = cont.addArea(tags.get("description", ""), "textarea", text -> {
            tags.put("description", text);
        }).size(400f, 140f).get();

        cont.row();

        cont.add("$editor.author").padRight(8).left();

        author = cont.addField(tags.get("author", Core.settings.getString("mapAuthor", "")), text -> {
            tags.put("author", text);
            Core.settings.put("mapAuthor", text);
            Core.settings.save();
        }).size(400, 55f).get();
        author.setMessageText("$unknown");

        cont.row();

        cont.add().padRight(8).left();
        cont.addCheck("$editor.oregen", enabled -> {
            tags.put("oregen", enabled ? "1" : "0");
        }).update(c -> c.setChecked(!tags.get("oregen", "0").equals("0"))).left();

        name.change();
        description.change();
        author.change();

        Platform.instance.addDialog(name, 50);
        Platform.instance.addDialog(author, 50);
        Platform.instance.addDialog(description, 1000);
    }
}
