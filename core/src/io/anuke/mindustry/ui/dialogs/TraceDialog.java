package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Administration.TraceInfo;

public class TraceDialog extends FloatingDialog{

    public TraceDialog(){
        super("$trace");

        addCloseButton();
        setFillParent(false);
    }

    public void show(Player player, TraceInfo info){
        cont.clear();

        Table table = new Table(Tex.clear);
        table.margin(14);
        table.defaults().pad(1);

        table.defaults().left();
        table.add(Core.bundle.format("trace.playername", player.name));
        table.row();
        table.add(Core.bundle.format("trace.ip", info.ip));
        table.row();
        table.add(Core.bundle.format("trace.id", info.uuid));
        table.row();
        table.add(Core.bundle.format("trace.modclient", info.modded));
        table.row();
        table.add(Core.bundle.format("trace.mobile", info.mobile));
        table.row();

        table.add().pad(5);
        table.row();

        cont.add(table);

        show();
    }
}
