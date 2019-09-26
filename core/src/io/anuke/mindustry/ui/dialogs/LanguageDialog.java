package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.ui.*;

import java.util.Locale;

import static io.anuke.mindustry.Vars.locales;
import static io.anuke.mindustry.Vars.ui;

public class LanguageDialog extends FloatingDialog{
    private Locale lastLocale;

    public LanguageDialog(){
        super("$settings.language");
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
            TextButton button = new TextButton(Strings.capitalize(loc.getDisplayName(loc)), Styles.clearTogglet);
            button.clicked(() -> {
                if(getLocale().equals(loc)) return;
                Core.settings.put("locale", loc.toString());
                Core.settings.save();
                Log.info("Setting locale: {0}", loc.toString());
                ui.showInfo("$language.restart");
            });
            langs.add(button).group(group).update(t -> t.setChecked(loc.equals(getLocale()))).size(400f, 50f).row();
        }

        cont.add(pane);
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
                Core.settings.put("locale", l.toString());
                return;
            }
        }

        //find by language
        for(Locale l : locales){
            if(l.getLanguage().equals(Locale.getDefault().getLanguage())){
                Core.settings.put("locale", l.toString());
                return;
            }
        }

        Core.settings.put("locale", new Locale("en").toString());
    }
}
