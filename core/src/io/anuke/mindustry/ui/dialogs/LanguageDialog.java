package io.anuke.mindustry.ui.dialogs;

import java.util.Locale;

public class LanguageDialog extends FloatingDialog{
    private Locale[] locales = {Locale.ENGLISH, Locale.FRENCH, new Locale("es", "LA"), new Locale("pt", "BR")};

    public LanguageDialog(){
        super("$text.language");
        addCloseButton();
    }
}
