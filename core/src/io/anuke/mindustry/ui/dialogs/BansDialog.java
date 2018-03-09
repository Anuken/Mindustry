package io.anuke.mindustry.ui.dialogs;

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

        for(String ip : netServer.admins.getBanned()){
            Table res = new Table("button");
            res.margin(14f);

            res.labelWrap("IP: [LIGHT_GRAY]" + ip + "\n[]Name: [LIGHT_GRAY]" + netServer.admins.getLastName(ip)).width(w - h - 24f);
            res.add().growX();
            res.addImageButton("icon-cancel", 14*3, () -> {
                ui.showConfirm("$text.confirm", "$text.confirmunban", () -> {
                    netServer.admins.unbanPlayerIP(ip);
                    setup();
                });
            }).size(h).pad(-14f);

            table.add(res).width(w).height(h);
            table.row();
        }

        content().add(pane);
    }
}
