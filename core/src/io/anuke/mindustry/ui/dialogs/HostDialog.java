package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

import static io.anuke.mindustry.Vars.player;
import static io.anuke.mindustry.Vars.ui;

public class HostDialog extends FloatingDialog{
    float w = 300;

    public HostDialog(){
        super("$text.hostserver");

        addCloseButton();

        content().table(t -> {
            t.add("$text.name").padRight(10);
            t.addField(Settings.getString("name"), text -> {
                if(text.isEmpty()) return;
                Vars.player.name = text;
                Settings.put("name", text);
                Settings.save();
                ui.listfrag.rebuild();
            }).grow().pad(8).get().setMaxLength(40);

            ImageButton button = t.addImageButton("white", 40, () -> {
                new ColorPickDialog().show(color -> {
                    player.color.set(color);
                    Settings.putInt("color", Color.rgba8888(color));
                    Settings.save();
                });
            }).size(50f, 54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.getColor());
        }).width(w).height(70f).pad(4).colspan(3);

        content().row();

        content().add().width(65f);

        content().addButton("$text.host", () -> {
            ui.loadfrag.show("$text.hosting");
            Timers.runTask(5f, () -> {
                try{
                    Net.host(Vars.port);
                    player.isAdmin = true;
                }catch (IOException e){
                    ui.showError(Bundles.format("text.server.error", Strings.parseException(e, false)));
                }
                ui.loadfrag.hide();
                hide();
            });
        }).width(w).height(70f);

        content().addButton("?", () -> ui.showInfo("$text.host.info")).size(65f, 70f).padLeft(6f);
    }
}
