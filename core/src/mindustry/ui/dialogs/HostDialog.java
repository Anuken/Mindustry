package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.io.*;

import static mindustry.Vars.*;

public class HostDialog extends BaseDialog{
    float w = 300;

    public HostDialog(){
        super("@hostserver");

        addCloseButton();

        cont.table(t -> {
            t.add("@name").padRight(10);
            t.field(Core.settings.getString("name"), text -> {
                player.name(text);
                Core.settings.put("name", text);
                ui.listfrag.rebuild();
            }).grow().pad(8).get().setMaxLength(40);

            ImageButton button = t.button(Tex.whiteui, Styles.squarei, 40, () -> {
                new PaletteDialog().show(color -> {
                    player.color.set(color);
                    Core.settings.put("color-0", color.rgba());
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color());
        }).width(w).height(70f).pad(4).colspan(3);

        if(steam){
            cont.row();

            cont.add().width(65f);

            cont.check("@steam.friendsonly", !Core.settings.getBool("steampublichost"), val -> Core.settings.put("steampublichost", !val)).colspan(2).left()
                .with(c -> ui.addDescTooltip(c, "@steam.friendsonly.tooltip")).padBottom(15f).row();
        }

        cont.row();

        cont.add().width(65f);

        cont.button("@host", () -> {
            if(Core.settings.getString("name").trim().isEmpty()){
                ui.showInfo("@noname");
                return;
            }

            runHost();
        }).width(w).height(70f);

        if(!steam){
            cont.button("?", () -> ui.showInfo("@host.info")).size(65f, 70f).padLeft(6f);
        }else{
            cont.add().size(65f, 70f).padLeft(6f);
        }

        shown(() -> {
            if(!steam){
                Core.app.post(() -> Core.settings.getBoolOnce("hostinfo", () -> ui.showInfo("@host.info")));
            }
        });
    }

    public void runHost(){
        ui.loadfrag.show("@hosting");
        Time.runTask(5f, () -> {
            try{
                net.host(Vars.port);
                player.admin = true;
                Events.fire(new HostEvent());

                if(steam && Core.settings.getBool("steampublichost")){
                    if(Version.modifier.contains("beta") || Version.modifier.contains("alpha")){
                        Core.settings.put("steampublichost", false);
                        platform.updateLobby();
                        Core.settings.getBoolOnce("betapublic", () -> ui.showInfo("@public.beta"));
                    }
                }


            }catch(IOException e){
                ui.showException("@server.error", e);
            }
            ui.loadfrag.hide();
            hide();
        });
    }
}
