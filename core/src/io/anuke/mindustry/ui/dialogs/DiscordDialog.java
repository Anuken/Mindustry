package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.scene.ui.Dialog;

public class DiscordDialog extends Dialog {

    public DiscordDialog(){
        super("Discord", "dialog");
        content().margin(12f);
        content().add("$text.discord");
        content().row();
        content().add("[orange]"+ Vars.discordURL);
        buttons().defaults().size(200f, 50);
        buttons().addButton("$text.openlink", () -> Mindustry.platforms.openLink(Vars.discordURL));
        buttons().addButton("$text.back", this::hide);
    }
}
