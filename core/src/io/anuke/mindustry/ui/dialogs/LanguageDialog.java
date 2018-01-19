package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.Vars;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;

import java.util.Locale;

public class LanguageDialog extends FloatingDialog{
    private Locale[] locales = {Locale.ENGLISH, new Locale("fr", "FR"),
            new Locale("es", "LA"), new Locale("pt", "BR"), new Locale("ko")};

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
            TextButton button = new TextButton(loc.getDisplayName(loc), "toggle");
            UCore.log(loc.getDisplayName(loc));
            button.setChecked(Vars.ui.getLocale().equals(loc));
            button.clicked(() -> {
                if(Vars.ui.getLocale().equals(loc)) return;
                Settings.putString("locale", loc.toString());
                Settings.save();
                UCore.log("Setting locale: " + loc.toString());
                Vars.ui.showInfo("$text.language.restart");
            });
            langs.add(button).group(group).update(t -> {
                t.setChecked(loc.equals(Vars.ui.getLocale()));
            }).size(400f, 60f).row();
        }

        content().add(pane);
    }
}
