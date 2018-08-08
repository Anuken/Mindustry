package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.TextArea;
import io.anuke.ucore.scene.ui.TextField;

public class MapInfoDialog extends FloatingDialog{
    private final MapEditor editor;

    private TextArea description;
    private TextField author;
    private TextField name;

    public MapInfoDialog(MapEditor editor){
        super("$text.editor.mapinfo");
        this.editor = editor;

        addCloseButton();

        shown(this::setup);

        hidden(() -> {

        });
    }

    private void setup(){
        content().clear();

        ObjectMap<String, String> tags = editor.getTags();

        content().add("$text.editor.name").padRight(8).left();

        content().defaults().padTop(15);

        name = content().addField(tags.get("name", ""), text -> {
            tags.put("name", text);
        }).size(400, 55f).get();
        name.setMessageText("$text.unknown");

        content().row();

        content().add("$text.editor.description").padRight(8).left();

        description = content().addArea(tags.get("description", ""), "textarea", text -> {
            tags.put("description", text);
        }).size(400f, 140f).get();

        content().row();

        content().add("$text.editor.author").padRight(8).left();

        author = content().addField(tags.get("author", Settings.getString("mapAuthor", "")), text -> {
            tags.put("author", text);
            Settings.putString("mapAuthor", text);
            Settings.save();
        }).size(400, 55f).get();
        author.setMessageText("$text.unknown");

        content().row();

        content().add().padRight(8).left();
        content().addCheck("$text.editor.oregen", enabled -> {
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
