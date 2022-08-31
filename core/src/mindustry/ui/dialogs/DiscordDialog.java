package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class DiscordDialog extends Dialog{

    public DiscordDialog(){
        super("");

        float h = 70f;

        cont.margin(12f);

        Color color = Color.valueOf("7289da");

        cont.table(t -> {
            t.background(Tex.button).margin(0);

            t.table(img -> {
                img.image().height(h - 5).width(40f).color(color);
                img.row();
                img.image().height(5).width(40f).color(color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            t.table(i -> {
                i.image(Icon.discord);
            }).size(h).left();

            t.add("@discord").color(Pal.accent).growX().padLeft(10f);
        }).size(520f, h).pad(10f);

        buttons.defaults().size(170f, 50);

        buttons.button("@back", Icon.left, this::hide);
        buttons.button("@copylink", Icon.copy, () -> {
            Core.app.setClipboardText(discordURL);
        });
        buttons.button("@openlink", Icon.discord, () -> {
            if(!Core.app.openURI(discordURL)){
                ui.showErrorMessage("@linkfail");
                Core.app.setClipboardText(discordURL);
            }
        });
    }
}
