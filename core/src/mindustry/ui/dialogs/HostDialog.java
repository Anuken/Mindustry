package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.ui.*;

import java.io.*;

import static mindustry.Vars.*;

public class HostDialog extends BaseDialog{
    float w = 300;
    TextField portField;

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

            ImageButton button = t.button(Tex.whiteui, Styles.clearFulli, 40, () -> {
                new PaletteDialog().show(color -> {
                    player.color().set(color);
                    Core.settings.put("color-0", color.rgba());
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color());
        }).width(w).height(70f).pad(4).colspan(3);

        cont.row();

        cont.table(t -> {
            t.add("@server.port").padRight(10);
            portField = t.field(String.valueOf(Core.settings.getInt("port", port)), text -> {
                Core.settings.put("port", Integer.parseInt(text));
            }).pad(8).grow().get();
            portField.setMaxLength(5);
            portField.setValidator(text -> {
                try {
                    int port = Integer.parseInt(text);
                    return port >= 1 && port <= 65535;
                } catch(NumberFormatException e){
                    return false;
                }
            });
        }).width(w).height(70f).pad(4).colspan(3);

        cont.row();

        cont.add().width(65f);

        cont.button("@host", () -> {
            if(Core.settings.getString("name").trim().isEmpty()){
                ui.showInfo("@noname");
                return;
            }

            runHost();
        }).width(w).height(70f).disabled(b -> !portField.isValid());

        cont.button("?", () -> ui.showInfo("@host.info")).size(65f, 70f).padLeft(6f);

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
                player.admin(true);

                if(steam){
                    Core.app.post(() -> Core.settings.getBoolOnce("steampublic2", () -> {
                        ui.showCustomConfirm("@setting.publichost.name", "@public.confirm", "@yes", "@no", () -> {
                            Core.settings.put("publichost", true);
                            platform.updateLobby();
                        }, () -> {
                            Core.settings.put("publichost", false);
                            platform.updateLobby();
                        });
                    }));
                }

                if(Version.modifier.contains("beta")){
                    Core.settings.put("publichost", false);
                    platform.updateLobby();
                    Core.settings.getBoolOnce("betapublic", () -> ui.showInfo("@public.beta"));
                }
            }catch(IOException e){
                ui.showException("@server.error", e);
            }
            ui.loadfrag.hide();
            hide();
        });
    }
}
