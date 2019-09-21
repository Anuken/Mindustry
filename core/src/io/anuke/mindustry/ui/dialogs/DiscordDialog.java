package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.Dialog;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.Pal;

import static io.anuke.mindustry.Vars.*;

public class DiscordDialog extends Dialog{

    public DiscordDialog(){
        super("");

        float h = 70f;

        cont.margin(12f);

        Color color = Color.valueOf("7289da");

        cont.table(t -> {
            t.background(Tex.button).margin(0);

            t.table(img -> {
                img.addImage().height(h - 5).width(40f).color(color);
                img.row();
                img.addImage().height(5).width(40f).color(color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            t.table(i -> {
                i.background(Tex.button);
                i.addImage(Icon.discord);
            }).size(h).left();

            t.add("$discord").color(Pal.accent).growX().padLeft(10f);
        }).size(440f, h).pad(10f);

        buttons.defaults().size(150f, 50);

        buttons.addButton("$back", this::hide);
        buttons.addButton("$copylink", () -> {
            Core.app.setClipboardText(discordURL);
        });
        buttons.addButton("$openlink", () -> {
            if(!Core.net.openURI(discordURL)){
                ui.showErrorMessage("$linkfail");
                Core.app.setClipboardText(discordURL);
            }
        });
    }
}
