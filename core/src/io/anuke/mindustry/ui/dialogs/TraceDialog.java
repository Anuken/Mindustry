package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class TraceDialog extends FloatingDialog {

    public TraceDialog(){
        super("$text.trace");

        addCloseButton();
    }

    public void show(Player player, TraceInfo info){
        content().clear();

        Table table = new Table("button");
        table.margin(14);
        table.defaults().pad(1);

        table.defaults().left();
        table.add(Bundles.format("text.trace.playername", player.name));
        table.row();
        table.add(Bundles.format("text.trace.ip", info.ip));
        table.row();
        table.add(Bundles.format("text.trace.id", info.uuid));
        table.row();
        table.add(Bundles.format("text.trace.modclient", info.modclient));
        table.row();
        table.add(Bundles.format("text.trace.android", info.android));
        table.row();

        table.add().pad(5);
        table.row();

        table.add(Bundles.format("text.trace.totalblocksbroken", info.totalBlocksBroken));
        table.row();
        table.add(Bundles.format("text.trace.structureblocksbroken", info.structureBlocksBroken));
        table.row();
        table.add(Bundles.format("text.trace.lastblockbroken", info.lastBlockBroken.formalName));
        table.row();

        table.add().pad(5);
        table.row();

        table.add(Bundles.format("text.trace.totalblocksplaced", info.totalBlocksPlaced));
        table.row();
        table.add(Bundles.format("text.trace.lastblockplaced", info.lastBlockPlaced.formalName));
        table.row();

        content().add(table);

        show();
    }
}
