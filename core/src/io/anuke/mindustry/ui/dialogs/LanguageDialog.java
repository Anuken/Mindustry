package io.anuke.mindustry.ui.dialogs;

import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.io.Platform;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;

import java.util.Locale;

public class LanguageDialog extends FloatingDialog{
    private Locale[] locales = {Locale.ENGLISH, new Locale("fr", "FR"),
            new Locale("es", "LA"), new Locale("pt", "BR"), new Locale("ko"), new Locale("in", "ID")};

    public LanguageDialog(){
        super("$text.settings.language");
        addCloseButton();
        setup();
    }

    private void setup(){
        Table langs = new Table();
        langs.marginRight(24f).marginLeft(24f);
        ScrollPane pane = new ScrollPane(langs, "clear");
        pane.setFadeScrollBars(false);

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        for(Locale loc : locales){
            TextButton button = new TextButton(Platform.instance.getLocaleName(loc), "toggle");
            button.setChecked(ui.getLocale().equals(loc));
            button.clicked(() -> {
                if(ui.getLocale().equals(loc)) return;
                Settings.putString("locale", loc.toString());
                Settings.save();
                UCore.log("Setting locale: " + loc.toString());
                ui.showInfo("$text.language.restart");
            });
            langs.add(button).group(group).update(t -> {
                t.setChecked(loc.equals(ui.getLocale()));
            }).size(400f, 60f).row();
        }

        content().add(pane);
    }
}
