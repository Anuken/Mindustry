package arc.scene.flabel.effects;

import arc.math.*;
import arc.scene.flabel.*;
import arc.util.*;

/** Moves the text vertically in a sine wave pattern. */
public class WaveEffect extends FEffect{
    private static final float defaultFrequency = 15f, defaultDistance = 0.33f, defaultIntensity = 0.5f;

    public float distance = 1; // How much of their height they should move
    public float frequency = 1; // How frequently the wave pattern repeats
    public float intensity = 1; // How fast the glyphs should move

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) frequency = Strings.parseFloat(params[1], 1f);
        if(params.length > 2) intensity = Strings.parseFloat(params[2], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float progressModifier = (1f / intensity) * defaultIntensity;
        float normalFrequency = (1f / frequency) * defaultFrequency;
        float progressOffset = localIndex / normalFrequency;
        float progress = calculateProgress(progressModifier, progressOffset);

        // Calculate offset
        float y = getLineHeight(label) * distance * Interp.sine.apply(-1, 1, progress) * defaultDistance;

        // Calculate fadeout
        float fadeout = calculateFadeout();
        y *= fadeout;

        // Apply changes
        glyph.yoffset += y;
    }

}
