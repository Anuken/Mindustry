package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.util.*;

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

        TextField[] portField = {null};

        cont.table(t -> {
            t.add("@server.port").padRight(10);
            portField[0] = t.field(String.valueOf(Core.settings.getInt("port", port)), text -> Core.settings.put("port", Strings.parseInt(text, 6567)))
            .pad(8).grow().maxTextLength(5).valid(text -> {
                int port = Strings.parseInt(text);
                return port >= 1 && port <= 65535;
            }).get();
        }).width(w).height(70f).pad(4).colspan(3);

        cont.row();

        cont.add().width(65f);

        cont.button("@host", () -> {
            if(Core.settings.getString("name").trim().isEmpty()){
                ui.showInfo("@noname");
                return;
            }

            runHost();
        }).width(w).height(70f).disabled(b -> !portField[0].isValid());

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
                net.host(Core.settings.getInt("port", port));
                player.admin = true;
                Events.fire(new HostEvent());

                if(steam && Core.settings.getBool("steampublichost")){
                    if(Version.modifier.contains("beta") || Version.modifier.contains("alpha")){
                        Core.settings.put("steampublichost", false);
                        platform.updateLobby();
                        Core.settings.getBoolOnce("betapublic", () -> ui.showInfo("@public.beta"));
                    }
                }


            }catch(Exception e){
                ui.showException(e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("address already in use") ? "@server.error.addressinuse" : "@server.error", e);
            }
            ui.loadfrag.hide();
            hide();
        });
    }
}
