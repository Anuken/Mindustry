package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.Platform;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Log;

import java.util.Locale;

import static io.anuke.mindustry.Vars.locales;
import static io.anuke.mindustry.Vars.ui;

public class LanguageDialog extends FloatingDialog{

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
                Log.info("Setting locale: {0}", loc.toString());
                ui.showInfo("$text.language.restart");
            });
            langs.add(button).group(group).update(t -> {
                t.setChecked(loc.equals(ui.getLocale()));
            }).size(400f, 60f).row();
        }

        content().add(pane);
    }
}
