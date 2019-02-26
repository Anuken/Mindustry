package io.anuke.mindustry.ui.dialogs;

public class TraceDialog extends FloatingDialog{

    public TraceDialog(){
        super("$trace");

        addCloseButton();
    }
/*
    public void show(Player player, SessionInfo info){
        cont.clear();

        Table table = new Table("clear");
        table.margin(14);
        table.defaults().pad(1);

        /*
        table.defaults().left();
        table.add(Core.bundle.format("trace.playername", player.name));
        table.row();
        table.add(Core.bundle.format("trace.ip", info.ip));
        table.row();
        table.add(Core.bundle.format("trace.id", info.uuid));
        table.row();
        table.add(Core.bundle.format("trace.modclient", info.modclient));
        table.row();
        table.add(Core.bundle.format("trace.android", info.android));
        table.row();

        table.add().pad(5);
        table.row();

        //disabled until further notice
/*
        table.add(Core.bundle.format("trace.totalblocksbroken", info.totalBlocksBroken));
        table.row();
        table.add(Core.bundle.format("trace.structureblocksbroken", info.structureBlocksBroken));
        table.row();
        table.add(Core.bundle.format("trace.lastblockbroken", info.lastBlockBroken.localizedName));
        table.row();

        table.add().pad(5);
        table.row();

        table.add(Core.bundle.format("trace.totalblocksplaced", info.totalBlocksPlaced));
        table.row();
        table.add(Core.bundle.format("trace.lastblockplaced", info.lastBlockPlaced.localizedName));
        table.row();

        cont.add(table);

        show();
    }*/
}
