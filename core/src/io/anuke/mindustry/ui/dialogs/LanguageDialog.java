package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Settings;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Log;

import java.util.Locale;

import static io.anuke.mindustry.Vars.locales;
import static io.anuke.mindustry.Vars.ui;

public class LanguageDialog extends FloatingDialog{
    private Locale lastLocale;

    public LanguageDialog(){
        super("$text.settings.language");
        addCloseButton();
        setup();
    }

    private void setup(){
        Table langs = new Table();
        langs.marginRight(24f).marginLeft(24f);
        ScrollPane pane = new ScrollPane(langs);
        pane.setFadeScrollBars(false);

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        for(Locale loc : locales){
            TextButton button = new TextButton(loc.getDisplayName(loc), "toggle");
            button.clicked(() -> {
                if(getLocale().equals(loc)) return;
                Core.settings.putString("locale", loc.toString());
                Core.settings.save();
                Log.info("Setting locale: {0}", loc.toString());
                ui.showInfo("$text.language.restart");
            });
            langs.add(button).group(group).update(t -> t.setChecked(loc.equals(getLocale()))).size(400f, 50f).pad(2).row();
        }

        content().add(pane);
    }

    public Locale getLocale(){
        String loc = Core.settings.getString("locale");

        if(loc.equals("default")){
            findClosestLocale();
        }

        if(lastLocale == null || !lastLocale.toString().equals(loc)){
            if(loc.contains("_")){
                String[] split = loc.split("_");
                lastLocale = new Locale(split[0], split[1]);
            }else{
                lastLocale = new Locale(loc);
            }
        }

        return lastLocale;
    }

    void findClosestLocale(){
        //check exact locale
        for(Locale l : locales){
            if(l.equals(Locale.getDefault())){
                Core.settings.putString("locale", l.toString());
                return;
            }
        }

        //find by language
        for(Locale l : locales){
            if(l.getLanguage().equals(Locale.getDefault().getLanguage())){
                Core.settings.putString("locale", l.toString());
                return;
            }
        }

        Core.settings.putString("locale", new Locale("en").toString());
    }
}
