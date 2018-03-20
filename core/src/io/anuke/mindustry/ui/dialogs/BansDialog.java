package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.*;

public class BansDialog extends FloatingDialog {

    public BansDialog(){
        super("$text.server.bans");

        addCloseButton();

        setup();

        shown(this::setup);
    }

    private void setup(){
        content().clear();

        if(gwt) return;

        float w = 400f, h = 80f;

        Table table = new Table();

        ScrollPane pane = new ScrollPane(table, "clear");
        pane.setFadeScrollBars(false);

        if(netServer.admins.getBanned().size == 0){
            table.add("$text.server.bans.none");
        }

        for(PlayerInfo info : netServer.admins.getBanned()){
            Table res = new Table("button");
            res.margin(14f);

            res.labelWrap("IP: [LIGHT_GRAY]" + info.lastIP + "\n[]Name: [LIGHT_GRAY]" + info.lastName).width(w - h - 24f);
            res.add().growX();
            res.addImageButton("icon-cancel", 14*3, () -> {
                ui.showConfirm("$text.confirm", "$text.confirmunban", () -> {
                    netServer.admins.unbanPlayerID(info.id);
                    setup();
                });
            }).size(h).pad(-14f);

            table.add(res).width(w).height(h);
            table.row();
        }

        content().add(pane);
    }
}
