package arc.scene.flabel;

import arc.func.*;
import arc.graphics.*;
import arc.scene.flabel.effects.*;
import arc.struct.*;

/** Configuration class that easily allows the user to fine tune the library's functionality. */
public class FConfig{

    /**
     * Whether or not <a href="https://github.com/libgdx/libgdx/wiki/Color-Markup-Language">LibGDX's Color Markup
     * Language</a> should be enabled when parsing a {@link FLabel}. Note that this library doesn't truly handle
     * colors, but simply convert them to the markup format. If markup is disabled, color tokens will be ignored.
     */
    public static boolean forceColorMarkupByDefault = true;

    /** Default time in seconds that an empty {@code WAIT} token should wait for. Default value is {@code 0.250}. */
    public static float defaultWaitValue = 0.250f;

    /** Time in seconds that takes for each char to appear in the default speed. Default value is {@code 0.035}. */
    public static float defaultSpeedPerChar = 0.035f;

    /**
     * Defines how many chars can appear per frame. Use a value less than {@code 1} to disable this limit. Default value
     * is {@code -1}.
     */
    public static int charLimitPerFrame = -1;

    /** Default color for the {@code CLEARCOLOR} token. Can be overriden by {@link FLabel#getClearColor()}. */
    public static Color defaultClearColor = new Color(Color.white);

    /**
     * Returns a map of characters and their respective interval multipliers, of which the interval to the next char
     * should be multiplied for.
     */
    public static ObjectFloatMap<Character> intervalMultipliersByChar = new ObjectFloatMap<>();

    /** Map of global variables that affect all {@link FLabel} instances at once. */
    public static final ObjectMap<String, String> globalVars = new ObjectMap<>();

    /** Map of start tokens and their effect classes. Internal use only. */
    static final ObjectMap<String, Prov<FEffect>> effects = new ObjectMap<>();

    /** Whether or not effect tokens are dirty and need to be recalculated. */
    static boolean dirtyEffectMaps = true;

    /**
     * Registers a new effect to FLabel.
     *
     * @param tokenName Name of the token that starts the effect, such as WAVE.
     */
    public static void registerEffect(String tokenName, Prov<FEffect> effect){
        effects.put(tokenName, effect);
        dirtyEffectMaps = true;
    }

    /**
     * Unregisters an effect from FLabel.
     *
     * @param tokenName Name of the token that starts the effect, such as WAVE.
     */
    public static void unregisterEffect(String tokenName){
        effects.remove(tokenName);
        dirtyEffectMaps = true;
    }

    static{
        // Generate default char intervals
        intervalMultipliersByChar.put(' ', 0.0f);
        intervalMultipliersByChar.put(':', 1.5f);
        intervalMultipliersByChar.put(',', 2.5f);
        intervalMultipliersByChar.put('.', 2.5f);
        intervalMultipliersByChar.put('!', 5.0f);
        intervalMultipliersByChar.put('?', 5.0f);
        intervalMultipliersByChar.put('\n', 20f);

        // Register default tokens
        registerEffect("ease", EaseEffect::new);
        registerEffect("jump", JumpEffect::new);
        registerEffect("shake", ShakeEffect::new);
        registerEffect("sick", SickEffect::new);
        registerEffect("wave", WaveEffect::new);
        registerEffect("wind", WindEffect::new);
        registerEffect("rainbow", RainbowEffect::new);
        registerEffect("gradient", GradientEffect::new);
        registerEffect("fade", FadeEffect::new);
        registerEffect("blink", BlinkEffect::new);
    }

}
