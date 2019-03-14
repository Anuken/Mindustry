package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.scene.ui.TextArea;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

public class MapInfoDialog extends FloatingDialog{
    private final MapEditor editor;
    private final WaveInfoDialog waveinfo;

    public MapInfoDialog(MapEditor editor){
        super("$editor.mapinfo");
        this.editor = editor;
        this.waveinfo = new WaveInfoDialog(editor);

        addCloseButton();

        shown(this::setup);
    }

    private void setup(){
        cont.clear();

        ObjectMap<String, String> tags = editor.getTags();

        cont.add("$editor.name").padRight(8).left();

        cont.defaults().padTop(15);

        TextField name = cont.addField(tags.get("name", ""), text -> {
            tags.put("name", text);
        }).size(400, 55f).get();
        name.setMessageText("$unknown");

        cont.row();

        cont.add("$editor.description").padRight(8).left();

        TextArea description = cont.addArea(tags.get("description", ""), "textarea", text -> {
            tags.put("description", text);
        }).size(400f, 140f).get();

        cont.row();

        cont.add("$editor.author").padRight(8).left();

        TextField author = cont.addField(tags.get("author", Core.settings.getString("mapAuthor", "")), text -> {
            tags.put("author", text);
            Core.settings.put("mapAuthor", text);
            Core.settings.save();
        }).size(400, 55f).get();
        author.setMessageText("$unknown");

        cont.row();
        cont.add("$editor.waves").padRight(8).left();
        cont.table(t -> {
            t.add().growX();
            t.label(() -> tags.containsKey("waves") ? "" : Core.bundle.get("editor.default")).left();
            t.add().growX();
            t.addButton("$edit", waveinfo::show).growY().width(200f);
        }).size(400, 55f);

        name.change();
        description.change();
        author.change();

        Platform.instance.addDialog(name, 50);
        Platform.instance.addDialog(author, 50);
        Platform.instance.addDialog(description, 1000);
    }
}
