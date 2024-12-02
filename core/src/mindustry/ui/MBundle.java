package mindustry.ui;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

import java.io.*;
import java.util.*;

public class MBundle extends I18NBundle{
    private static final Locale ROOT_LOCALE = new Locale("", "", "");

    public static MBundle createEmptyBundle(){
        MBundle bundle = new MBundle();
        Reflect.set(I18NBundle.class, bundle, "locale", ROOT_LOCALE);
        ObjectMap<String, String> properties = new ObjectMap<>();
        Reflect.set(I18NBundle.class, bundle, "properties", properties);
        return bundle;
    }

    public static MBundle createBundle(Fi baseFileHandle){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), "UTF-8");
    }

    public static MBundle createBundle(Fi baseFileHandle, Locale locale){
        return createBundleImpl(baseFileHandle, locale, "UTF-8");
    }

    public static MBundle createBundle(Fi baseFileHandle, String encoding){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding);
    }

    public static MBundle createBundle(Fi baseFileHandle, Locale locale, String encoding){
        return createBundleImpl(baseFileHandle, locale, encoding);
    }

    private static MBundle createBundleImpl(Fi baseFileHandle, Locale locale, String encoding){
        if(baseFileHandle != null && locale != null && encoding != null){
            MBundle baseBundle = null;
            Locale targetLocale = locale;

            MBundle bundle;
            do{
                Seq<Locale> candidateLocales = getCandidateLocales(targetLocale);
                bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle);
                if(bundle != null){
                    Locale bundleLocale = bundle.locale;
                    boolean isBaseBundle = bundleLocale.equals(ROOT_LOCALE);
                    if(!isBaseBundle || bundleLocale.equals(locale) || candidateLocales.size == 1 && bundleLocale.equals(candidateLocales.get(0))){
                        break;
                    }

                    if(baseBundle == null){
                        baseBundle = bundle;
                    }
                }

                targetLocale = getFallbackLocale(targetLocale);
            }while(targetLocale != null);

            if(bundle == null){
                if(baseBundle == null){
                    throw new MissingResourceException("Can't find bundle for base file handle " + baseFileHandle.path() + ", locale " + locale, baseFileHandle + "_" + locale, "");
                }

                bundle = baseBundle;
            }

            return bundle;
        }else{
            throw new NullPointerException();
        }
    }

    private static Seq<Locale> getCandidateLocales(Locale locale){
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        Seq<Locale> locales = new Seq(4);
        if(!variant.isEmpty()){
            locales.add(locale);
        }

        if(!country.isEmpty()){
            locales.add(locales.isEmpty() ? locale : new Locale(language, country));
        }

        if(!language.isEmpty()){
            locales.add(locales.isEmpty() ? locale : new Locale(language));
        }

        locales.add(ROOT_LOCALE);
        return locales;
    }

    private static Locale getFallbackLocale(Locale locale){
        Locale defaultLocale = Locale.getDefault();
        return locale.equals(defaultLocale) ? null : defaultLocale;
    }

    private static MBundle loadBundleChain(Fi baseFileHandle, String encoding, Seq<Locale> candidateLocales, int candidateIndex, MBundle baseBundle){
        Locale targetLocale = (Locale)candidateLocales.get(candidateIndex);
        MBundle parent = null;
        if(candidateIndex != candidateLocales.size - 1){
            parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle);
        }else if(baseBundle != null && targetLocale.equals(ROOT_LOCALE)){
            return baseBundle;
        }

        MBundle bundle = loadBundle(baseFileHandle, encoding, targetLocale);
        if(bundle != null){
            Reflect.set(I18NBundle.class, bundle, "parent", parent);
            return bundle;
        }else{
            return parent;
        }
    }

    private static MBundle loadBundle(Fi baseFileHandle, String encoding, Locale targetLocale){
        MBundle bundle = null;
        Reader reader = null;

        try{
            Fi fileHandle = toFileHandle(baseFileHandle, targetLocale);
            if(checkFileExistence(fileHandle)){
                bundle = new MBundle();
                reader = fileHandle.reader(encoding);
                bundle.load(reader);
            }
        }finally{
            Streams.close(reader);
        }

        if(bundle != null){
            Reflect.invoke(I18NBundle.class, bundle, "setLocale", new Object[]{targetLocale});
        }

        return bundle;
    }

    private static boolean checkFileExistence(Fi fh){
        try{
            fh.read().close();
            return true;
        }catch(Exception var2){
            return false;
        }
    }

    private static Fi toFileHandle(Fi baseFileHandle, Locale locale){
        StringBuilder sb = new StringBuilder(baseFileHandle.name());
        if(!locale.equals(ROOT_LOCALE)){
            String language = locale.getLanguage().replace("in", "id");
            String country = locale.getCountry();
            String variant = locale.getVariant();
            boolean emptyLanguage = "".equals(language);
            boolean emptyCountry = "".equals(country);
            boolean emptyVariant = "".equals(variant);
            if(!emptyLanguage || !emptyCountry || !emptyVariant){
                sb.append('_');
                if(!emptyVariant){
                    sb.append(language).append('_').append(country).append('_').append(variant);
                }else if(!emptyCountry){
                    sb.append(language).append('_').append(country);
                }else{
                    sb.append(language);
                }
            }
        }

        return baseFileHandle.sibling(sb.append(".properties").toString());
    }
}
