package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Net;

import java.io.IOException;

import static io.anuke.mindustry.Vars.player;
import static io.anuke.mindustry.Vars.ui;

public class HostDialog extends FloatingDialog{
    float w = 300;

    public HostDialog(){
        super("$hostserver");

        addCloseButton();

        cont.table(t -> {
            t.add("$name").padRight(10);
            t.addField(Core.settings.getString("name"), text -> {
                player.name = text;
                Core.settings.put("name", text);
                Core.settings.save();
                ui.listfrag.rebuild();
            }).grow().pad(8).get().setMaxLength(40);

            ImageButton button = t.addImageButton("white", "clear-full", 40, () -> {
                new ColorPickDialog().show(color -> {
                    player.color.set(color);
                    Core.settings.put("color-0", Color.rgba8888(color));
                    Core.settings.save();
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color);
        }).width(w).height(70f).pad(4).colspan(3);

        cont.row();

        cont.add().width(65f);

        cont.addButton("$host", () -> {
            if(Core.settings.getString("name").trim().isEmpty()){
                ui.showInfo("$noname");
                return;
            }

            ui.loadfrag.show("$hosting");
            Time.runTask(5f, () -> {
                try{
                    Net.host(Vars.port);
                    player.isAdmin = true;
                }catch(IOException e){
                    ui.showError(Core.bundle.format("server.error", Strings.parseException(e, true)));
                }
                ui.loadfrag.hide();
                hide();
            });
        }).width(w).height(70f);

        cont.addButton("?", () -> ui.showInfo("$host.info")).size(65f, 70f).padLeft(6f);
    }
}
