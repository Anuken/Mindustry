package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class TraceDialog extends BaseDialog{

    public TraceDialog(){
        super("@trace");

        addCloseButton();
    }

    public void show(Player player, TraceInfo info){
        cont.clear();

        Table table = new Table(Tex.clear);
        table.margin(14);
        table.defaults().pad(1);

        table.defaults().left();

        var style = Styles.emptyi;
        float s = 28f;
        
        table.table(c -> {
            c.left().defaults().left();
            c.button(Icon.copySmall, style, () -> copy(player.name)).size(s).padRight(4f);
            c.add(Core.bundle.format("trace.playername", player.name)).row();
            c.button(Icon.copySmall, style, () -> copy(info.ip)).size(s).padRight(4f);
            c.add(Core.bundle.format("trace.ip", info.ip)).row();
            c.button(Icon.copySmall, style, () -> copy(info.uuid)).size(s).padRight(4f);
            c.add(Core.bundle.format("trace.id", info.uuid)).row();
            c.button(Icon.copySmall, style, () -> copy(player.locale)).size(s).padRight(4f);
            c.add(Core.bundle.format("trace.language", player.locale)).row();
        }).row();

        table.add(Core.bundle.format("trace.modclient", info.modded)).row();
        table.add(Core.bundle.format("trace.mobile", info.mobile)).row();
        table.add(Core.bundle.format("trace.times.joined", info.timesJoined)).row();
        table.add(Core.bundle.format("trace.times.kicked", info.timesKicked)).row();

        for(int i = 0; i < 2; i++){
            table.add(i == 0 ? "@trace.ips" : "@trace.names").row();
            String[] list = i == 0 ? info.ips : info.names;

            table.pane(t -> {
                t.left();
                for(String val : list){
                    t.add("[lightgray]" + val).left().row();
                }
            }).padLeft(20f).fill().left().row();
        }

        table.add().pad(5);
        table.row();

        cont.add(table);

        show();
    }

    private void copy(String content){
        Core.app.setClipboardText(content);
        ui.showInfoFade("@copied");
    }
}
