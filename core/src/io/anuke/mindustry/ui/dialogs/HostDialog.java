package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.ui.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

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

            ImageButton button = t.addImageButton(Tex.whiteui, Styles.clearFulli, 40, () -> {
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

            runHost();
        }).width(w).height(70f);

        cont.addButton("?", () -> ui.showInfo("$host.info")).size(65f, 70f).padLeft(6f);

        shown(() -> {
            if(!steam){
                Core.app.post(() -> Core.settings.getBoolOnce("hostinfo", () -> ui.showInfo("$host.info")));
            }
        });
    }

    public void runHost(){
        ui.loadfrag.show("$hosting");
        Time.runTask(5f, () -> {
            try{
                net.host(Vars.port);
                player.isAdmin = true;

                if(steam){
                    Core.app.post(() -> Core.settings.getBoolOnce("steampublic", () -> {
                        ui.showCustomConfirm("$setting.publichost.name", "$public.confirm", "$yes", "$no", () -> {
                            Core.settings.putSave("publichost", true);
                            platform.updateLobby();
                        });
                    }));
                }

                if(Version.modifier.contains("beta")){
                    Core.settings.putSave("publichost", false);
                    platform.updateLobby();
                    Core.settings.getBoolOnce("betapublic", () -> ui.showInfo("$public.beta"));
                }
            }catch(IOException e){
                ui.showException("$server.error", e);
            }
            ui.loadfrag.hide();
            hide();
        });
    }
}
