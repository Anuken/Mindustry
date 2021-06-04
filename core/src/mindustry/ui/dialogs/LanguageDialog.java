package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ui.*;

import java.util.*;

import static mindustry.Vars.*;

public class LanguageDialog extends BaseDialog{
    private Locale lastLocale;
    private ObjectMap<String, String> displayNames = ObjectMap.of(
        "in_ID", "Bahasa Indonesia (Indonesia)",
        "da", "Dansk",
        "de", "Deutsch",
        "et", "Eesti",
        "en", "English",
        "es", "Español",
        "eu", "Euskara",
        "fil", "Filipino",
        "fr", "Français",
        "it", "Italiano",
        "lt", "Lietuvių",
        "hu", "Magyar",
        "nl", "Nederlands",
        "nl_BE", "Nederlands (België)",
        "pl", "Polski",
        "pt_BR", "Português (Brasil)",
        "pt_PT", "Português (Portugal)",
        "ro", "Română",
        "fi", "Suomi",
        "sv", "Svenska",
        "vi", "Tiếng Việt",
        "tk", "Türkmen dili",
        "tr", "Türkçe",
        "cs", "Čeština",
        "be", "Беларуская",
        "bg", "Български",
        "ru", "Русский",
        "uk_UA", "Українська (Україна)",
        "th", "ไทย",
        "zh_CN", "简体中文",
        "zh_TW", "正體中文",
        "ja", "日本語",
        "ko", "한국어",
        "router", "router"
    );

    public LanguageDialog(){
        super("@settings.language");
        addCloseButton();
        setup();
    }

    private void setup(){
        Table langs = new Table();
        langs.marginRight(24f).marginLeft(24f);
        ScrollPane pane = new ScrollPane(langs);
        pane.setScrollingDisabled(true, false);

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        for(Locale loc : locales){
            TextButton button = new TextButton(displayNames.get(loc.toString(), loc.getDisplayName(Locale.ROOT)), Styles.clearTogglet);
            button.clicked(() -> {
                if(getLocale().equals(loc)) return;
                Core.settings.put("locale", loc.toString());
                Log.info("Setting locale: @", loc.toString());
                ui.showInfo("@language.restart");
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
