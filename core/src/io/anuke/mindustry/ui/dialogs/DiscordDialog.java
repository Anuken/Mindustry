package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import io.anuke.ucore.scene.ui.Dialog;

import static io.anuke.mindustry.Vars.discordURL;

public class DiscordDialog extends Dialog {

    public DiscordDialog(){
        super("Discord", "dialog");
        content().margin(12f);
        content().add("$text.discord");
        content().row();
        content().add("[orange]"+ discordURL);
        buttons().defaults().size(200f, 50);
        buttons().addButton("$text.openlink", () -> Gdx.net.openURI(discordURL));
        buttons().addButton("$text.back", this::hide);
    }
}
