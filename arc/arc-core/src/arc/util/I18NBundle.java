package arc.util;

import arc.struct.*;
import arc.files.*;
import arc.util.io.*;

import java.io.*;
import java.util.*;

/**
 * A {@code I18NBundle} provides {@code Locale}-specific resources loaded from property files. A bundle contains a number of named
 * resources, whose names and values are {@code Strings}. A bundle may have a parent bundle, and when a resource is not found in a
 * bundle, the parent bundle is searched for the resource. If the fallback mechanism reaches the base bundle and still can't find
 * the resource it throws a {@code MissingResourceException}.
 *
 * <ul>
 * <li>All bundles for the same group of resources share a common base bundle. This base bundle acts as the root and is the last
 * fallback in case none of its children was able to respond to a request.</li>
 * <li>The first level contains changes between different languages. Only the differences between a language and the language of
 * the base bundle need to be handled by a language-specific {@code I18NBundle}.</li>
 * <li>The second level contains changes between different countries that use the same language. Only the differences between a
 * country and the country of the language bundle need to be handled by a country-specific {@code I18NBundle}.</li>
 * <li>The third level contains changes that don't have a geographic reason (e.g. changes that where made at some point in time
 * like {@code PREEURO} where the currency of come countries changed. The country bundle would return the current currency (Euro)
 * and the {@code PREEURO} variant bundle would return the old currency (e.g. DM for Germany).</li>
 * </ul>
 *
 * <strong>Examples</strong>
 * <ul>
 * <li>BaseName (base bundle)
 * <li>BaseName_de (german language bundle)
 * <li>BaseName_fr (french language bundle)
 * <li>BaseName_de_DE (bundle with Germany specific resources in german)
 * <li>BaseName_de_CH (bundle with Switzerland specific resources in german)
 * <li>BaseName_fr_CH (bundle with Switzerland specific resources in french)
 * <li>BaseName_de_DE_PREEURO (bundle with Germany specific resources in german of the time before the Euro)
 * <li>BaseName_fr_FR_PREEURO (bundle with France specific resources in french of the time before the Euro)
 * </ul>
 * <p>
 * It's also possible to create variants for languages or countries. This can be done by just skipping the country or language
 * abbreviation: BaseName_us__POSIX or BaseName__DE_PREEURO. But it's not allowed to circumvent both language and country:
 * BaseName___VARIANT is illegal.
 * @author davebaol
 * @see PropertiesUtils
 */
public class I18NBundle{
    private static final String DEFAULT_ENCODING = "UTF-8";
    // Locale.ROOT does not exist in Android API level 8
    private static final Locale ROOT_LOCALE = new Locale("", "", "");
    private static boolean simpleFormatter = false;
    /** The parent of this {@code I18NBundle} that is used if this bundle doesn't include the requested resource. */
    private I18NBundle parent;
    /** The locale for this bundle. */
    private Locale locale;
    /** The properties for this bundle. */
    private ObjectMap<String, String> properties;
    /** The formatter used for argument replacement. */
    private TextFormatter formatter;

    /**
     * Returns the flag indicating whether to use the simplified message pattern syntax (default is false).
     */
    public static boolean getSimpleFormatter(){
        return simpleFormatter;
    }

    /**
     * Sets the flag indicating whether to use the simplified message pattern. The flag must be set before calling the factory
     * methods {@code createBundle}.
     */
    public static void setSimpleFormatter(boolean enabled){
        simpleFormatter = enabled;
    }

    /** Returns an empty bundle with no keys. */
    public static I18NBundle createEmptyBundle(){
        I18NBundle bundle = new I18NBundle();
        bundle.locale = ROOT_LOCALE;
        bundle.formatter = new TextFormatter(Locale.ROOT, false);
        bundle.properties = new ObjectMap<>();
        return bundle;
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code>, the default locale and the default encoding "UTF-8".
     * @param baseFileHandle the file handle to the base of the bundle
     * @return a bundle for the given base file handle and the default locale
     * @throws NullPointerException if <code>baseFileHandle</code> is <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    public static I18NBundle createBundle(Fi baseFileHandle){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code> and <code>locale</code>; the default encoding "UTF-8"
     * is used.
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale the locale for which a bundle is desired
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    public static I18NBundle createBundle(Fi baseFileHandle, Locale locale){
        return createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code> and <code>encoding</code>; the default locale is used.
     * @param baseFileHandle the file handle to the base of the bundle
     * @param encoding the charter encoding
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>encoding</code> is <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    public static I18NBundle createBundle(Fi baseFileHandle, String encoding){
        return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding);
    }

    /**
     * Creates a new bundle using the specified <code>baseFileHandle</code>, <code>locale</code> and <code>encoding</code>.
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale the locale for which a bundle is desired
     * @param encoding the charter encoding
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException if <code>baseFileHandle</code>, <code>locale</code> or <code>encoding</code> is
     * <code>null</code>
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    public static I18NBundle createBundle(Fi baseFileHandle, Locale locale, String encoding){
        return createBundleImpl(baseFileHandle, locale, encoding);
    }

    private static I18NBundle createBundleImpl(Fi baseFileHandle, Locale locale, String encoding){
        if(baseFileHandle == null || locale == null || encoding == null) throw new NullPointerException();

        I18NBundle bundle;
        I18NBundle baseBundle = null;
        Locale targetLocale = locale;
        do{
            // Create the candidate locales
            Seq<Locale> candidateLocales = getCandidateLocales(targetLocale);

            // Load the bundle and its parents recursively
            bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle);

            // Check the loaded bundle (if any)
            if(bundle != null){
                Locale bundleLocale = bundle.locale;
                boolean isBaseBundle = bundleLocale.equals(ROOT_LOCALE);

                if(!isBaseBundle || bundleLocale.equals(locale)){
                    // Found the bundle for the requested locale
                    break;
                }
                if(candidateLocales.size == 1 && bundleLocale.equals(candidateLocales.get(0))){
                    // Found the bundle for the only candidate locale
                    break;
                }
                if(baseBundle == null){
                    // Store the base bundle and keep on processing the remaining fallback locales
                    baseBundle = bundle;
                }
            }

            // Set next fallback locale
            targetLocale = getFallbackLocale(targetLocale);

        }while(targetLocale != null);

        if(bundle == null){
            if(baseBundle == null){
                // No bundle found
                throw new MissingResourceException("Can't find bundle for base file handle " + baseFileHandle.path() + ", locale "
                + locale, baseFileHandle + "_" + locale, "");
            }
            // Set the base bundle to be returned
            bundle = baseBundle;
        }

        return bundle;
    }

    /**
     * Returns a <code>List</code> of <code>Locale</code>s as candidate locales for the given <code>locale</code>. This method is
     * called by the <code>createBundle</code> factory method each time the factory method tries finding a resource bundle for a
     * target <code>Locale</code>.
     *
     * <p>
     * The sequence of the candidate locales also corresponds to the runtime resource lookup path (also known as the <I>parent
     * chain</I>), if the corresponding resource bundles for the candidate locales exist and their parents are not defined by
     * loaded resource bundles themselves. The last element of the list is always the {@linkplain Locale#ROOT root locale}, meaning
     * that the base bundle is the terminal of the parent chain.
     *
     * <p>
     * If the given locale is equal to <code>Locale.ROOT</code> (the root locale), a <code>List</code> containing only the root
     * <code>Locale</code> is returned. In this case, the <code>createBundle</code> factory method loads only the base bundle as
     * the resulting resource bundle.
     *
     * <p>
     * This implementation returns a <code>List</code> containing <code>Locale</code>s in the following sequence:
     *
     * <pre>
     *     Locale(language, country, variant)
     *     Locale(language, country)
     *     Locale(language)
     *     Locale.ROOT
     * </pre>
     * <p>
     * where <code>language</code>, <code>country</code> and <code>variant</code> are the language, country and variant values of
     * the given <code>locale</code>, respectively. Locales where the final component values are empty strings are omitted.
     *
     * <p>
     * For example, if the given base name is "Messages" and the given <code>locale</code> is
     * <code>Locale("ja",&nbsp;"",&nbsp;"XX")</code>, then a <code>List</code> of <code>Locale</code>s:
     *
     * <pre>
     *     Locale("ja", "", "XX")
     *     Locale("ja")
     *     Locale.ROOT
     * </pre>
     * <p>
     * is returned. And if the resource bundles for the "ja" and "" <code>Locale</code>s are found, then the runtime resource
     * lookup path (parent chain) is:
     *
     * <pre>
     *     Messages_ja -> Messages
     * </pre>
     * @param locale the locale for which a resource bundle is desired
     * @return a <code>List</code> of candidate <code>Locale</code>s for the given <code>locale</code>
     * @throws NullPointerException if <code>locale</code> is <code>null</code>
     */
    private static Seq<Locale> getCandidateLocales(Locale locale){
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        Seq<Locale> locales = new Seq<>(4);
        if(variant.length() > 0){
            locales.add(locale);
        }
        if(country.length() > 0){
            locales.add(locales.isEmpty() ? locale : new Locale(language, country));
        }
        if(language.length() > 0){
            locales.add(locales.isEmpty() ? locale : new Locale(language));
        }
        locales.add(ROOT_LOCALE);
        return locales;
    }

    /**
     * Returns a <code>Locale</code> to be used as a fallback locale for further bundle searches by the <code>createBundle</code>
     * factory method. This method is called from the factory method every time when no resulting bundle has been found for
     * <code>baseFileHandler</code> and <code>locale</code>, where locale is either the parameter for <code>createBundle</code> or
     * the previous fallback locale returned by this method.
     *
     * <p>
     * This method returns the {@linkplain Locale#getDefault() default <code>Locale</code>} if the given <code>locale</code> isn't
     * the default one. Otherwise, <code>null</code> is returned.
     * @param locale the <code>Locale</code> for which <code>createBundle</code> has been unable to find any resource bundles
     * (except for the base bundle)
     * @return a <code>Locale</code> for the fallback search, or <code>null</code> if no further fallback search is needed.
     * @throws NullPointerException if <code>locale</code> is <code>null</code>
     */
    private static Locale getFallbackLocale(Locale locale){
        Locale defaultLocale = Locale.getDefault();
        return locale.equals(defaultLocale) ? null : defaultLocale;
    }

    private static I18NBundle loadBundleChain(Fi baseFileHandle, String encoding, Seq<Locale> candidateLocales,
                                              int candidateIndex, I18NBundle baseBundle){
        Locale targetLocale = candidateLocales.get(candidateIndex);
        I18NBundle parent = null;
        if(candidateIndex != candidateLocales.size - 1){
            // Load recursively the parent having the next candidate locale
            parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle);
        }else if(baseBundle != null && targetLocale.equals(ROOT_LOCALE)){
            return baseBundle;
        }

        // Load the bundle
        I18NBundle bundle = loadBundle(baseFileHandle, encoding, targetLocale);
        if(bundle != null){
            bundle.parent = parent;
            return bundle;
        }

        return parent;
    }

    // Tries to load the bundle for the given locale.
    private static I18NBundle loadBundle(Fi baseFileHandle, String encoding, Locale targetLocale){
        I18NBundle bundle = null;
        Reader reader = null;
        try{
            Fi fileHandle = toFileHandle(baseFileHandle, targetLocale);
            if(checkFileExistence(fileHandle)){
                // Instantiate the bundle
                bundle = new I18NBundle();

                // Load bundle properties from the stream with the specified encoding
                reader = fileHandle.reader(encoding);
                bundle.load(reader);
            }
        }finally{
            Streams.close(reader);
        }
        if(bundle != null){
            bundle.setLocale(targetLocale);
        }

        return bundle;
    }

    //Fixes some problems with fh.exists(), see #2342 / #2345
    private static boolean checkFileExistence(Fi fh){
        try{
            fh.read().close();
            return true;
        }catch(Exception e){
            return false;
        }
    }

    /**
     * Converts the given <code>baseFileHandle</code> and <code>locale</code> to the corresponding file handle.
     *
     * <p>
     * This implementation returns the <code>baseFileHandle</code>'s sibling with following value:
     *
     * <pre>
     * baseFileHandle.name() + &quot;_&quot; + language + &quot;_&quot; + country + &quot;_&quot; + variant + &quot;.properties&quot;
     * </pre>
     * <p>
     * where <code>language</code>, <code>country</code> and <code>variant</code> are the language, country and variant values of
     * <code>locale</code>, respectively. Final component values that are empty Strings are omitted along with the preceding '_'.
     * If all of the values are empty strings, then <code>baseFileHandle.name()</code> is returned with ".properties" appended.
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale the locale for which a resource bundle should be loaded
     * @return the file handle for the bundle
     * @throws NullPointerException if <code>baseFileHandle</code> or <code>locale</code> is <code>null</code>
     */
    private static Fi toFileHandle(Fi baseFileHandle, Locale locale){
        StringBuilder sb = new StringBuilder(baseFileHandle.name());
        if(!locale.equals(ROOT_LOCALE)){
            String language = locale.getLanguage().replace("in", "id");
            String country = locale.getCountry();
            String variant = locale.getVariant();
            boolean emptyLanguage = "".equals(language);
            boolean emptyCountry = "".equals(country);
            boolean emptyVariant = "".equals(variant);

            if(!(emptyLanguage && emptyCountry && emptyVariant)){
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

    /**
     * Load the properties from the specified reader.
     * @param reader the reader
     */
    private void load(Reader reader){
        properties = new ObjectMap<>();
        PropertiesUtils.load(properties, reader);
    }

    /**
     * Returns the locale of this bundle. This method can be used after a call to <code>createBundle()</code> to determine whether
     * the resource bundle returned really corresponds to the requested locale or is a fallback.
     * @return the locale of this bundle
     */
    public Locale getLocale(){
        return locale;
    }

    /**
     * Sets the bundle locale. This method is private because a bundle can't change the locale during its life.
     */
    private void setLocale(Locale locale){
        this.locale = locale;
        this.formatter = new TextFormatter(locale, !simpleFormatter);
    }

    /**
     * Gets a string for the given key from this bundle or one of its parents.
     * @param key the key for the desired string
     * @return the string for the given key or the key surrounded by {@code ???} if it cannot be found
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    public final String get(String key){
        String result = properties.get(key);
        if(result == null){
            if(parent != null) result = parent.get(key);
            if(result == null){
                return "???" + key + "???";
            }
        }
        return result;
    }

    /** Returns the string for this given key, or def. */
    public String get(String key, String def){
        return has(key) ? get(key) : def;
    }

    public String getOrNull(String key){
        return has(key) ? get(key) : null;
    }

    public String getNotNull(String key){
        String s = getOrNull(key);
        if(s == null){
            throw new MissingResourceException("No key with name \"" + key + "\" found!", this.getClass().getName(), key);
        }
        return s;
    }

    /**Returns all keys in this bundle. Does not check parent bundles.*/
    public Iterable<String> getKeys(){
        return properties.keys();
    }

    /** @return the internal property map. Can be modified. */
    public ObjectMap<String, String> getProperties(){
        return properties;
    }

    public void setProperties(ObjectMap<String, String> properties){
        this.properties = properties;
    }

    /** Checks whether a specified key is present in this bundle. */
    public boolean has(String key){
        if(properties.containsKey(key)){
            return true;
        }

        if(parent != null){
            return parent.has(key);
        }
        return false;
    }

    /**
     * Gets the string with the specified key from this bundle or one of its parent after replacing the given arguments if they
     * occur.
     * @param key the key for the desired string
     * @param args the arguments to be replaced in the string associated to the given key.
     * @return the string for the given key formatted with the given arguments
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     * @throws MissingResourceException if no string for the given key can be found
     */
    public String format(String key, Object... args){
        return formatter.format(get(key), args);
    }

    public String formatString(String string, Object... args){
        return formatter.format(string, args);
    }

    /**Format, but with a number with fixed decimal places.*/
    public String formatFloat(String key, float value, int places){
        return formatter.format(get(key), Strings.fixed(value, places));
    }

    /**
     * Sets the value of all localized strings to String placeholder so hardcoded, unlocalized values can be easily spotted.
     * The I18NBundle won't be able to reset values after calling debug and should only be using during testing.
     */
    public void debug(String placeholder){
        ObjectMap.Keys<String> keys = properties.keys();
        if(keys == null) return;

        for(String s : keys){
            properties.put(s, placeholder);
        }
    }

    /** @return the parent bundle. */
    public I18NBundle getParent(){
        return parent;
    }
}
