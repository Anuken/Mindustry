package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.arc.scene.ui.Dialog;

import static io.anuke.mindustry.Vars.discordURL;
import static io.anuke.mindustry.Vars.ui;

public class DiscordDialog extends Dialog{

    public DiscordDialog(){
        super("", "dialog");

        float h = 70f;

        content().margin(12f);

        Color color = Color.valueOf("7289da");

        content().table(t -> {
            t.background("button").margin(0);

            t.table(img -> {
                img.addImage("white").height(h - 5).width(40f).color(color);
                img.row();
                img.addImage("white").height(5).width(40f).color(color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            t.table(i -> {
                i.background("button");
                i.addImage("icon-discord").size(14 * 3);
            }).size(h).left();

            t.add("$discord").color(Palette.accent).growX().padLeft(10f);
        }).size(470f, h).pad(10f);

        buttons().defaults().size(170f, 50);

        buttons().addButton("$back", this::hide);
        buttons().addButton("$copylink", () -> {
            Core.app.getClipboard().setContents(discordURL);
        });
        buttons().addButton("$openlink", () -> {
            if(!Core.net.openURI(discordURL)){
                ui.showError("$linkfail");
                Core.app.getClipboard().setContents(discordURL);
            }
        });
    }
}
