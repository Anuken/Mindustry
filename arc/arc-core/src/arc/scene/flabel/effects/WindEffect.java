package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.util.*;
import arc.util.noise.*;

/** Moves the text in a wind pattern. */
public class WindEffect extends FEffect{
    private static final float defaultSpacing = 10f, defaultDistance = 0.33f, defaultIntensity = 0.375f, distanceXRatio = 1.5f, distanceYRatio = 1.0f;

    private float noiseCursorX = 0;
    private float noiseCursorY = 0;

    public float distanceX = 1; // How much of their line height glyphs should move in the X axis
    public float distanceY = 1; // How much of their line height glyphs should move in the Y axis
    public float spacing = 1; // How much space there should be between waves
    public float intensity = 1; // How strong the wind should be

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distanceX = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) distanceY = Strings.parseFloat(params[1], 1f);
        if(params.length > 2) spacing = Strings.parseFloat(params[2], 1f);
        if(params.length > 3) intensity = Strings.parseFloat(params[3], 1f);
    }

    @Override
    public void update(float delta){
        super.update(delta);

        // Update noise cursor
        noiseCursorX += 0.1f * intensity * defaultIntensity;
        noiseCursorY += 0.1f * intensity * defaultIntensity;
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float progressModifier = (1f / intensity) * defaultIntensity;
        float normalSpacing = (1f / spacing) * defaultSpacing;
        float progressOffset = localIndex / normalSpacing;
        float progress = calculateProgress(progressModifier, progressOffset);

        // Calculate noise
        float indexOffset = localIndex * 0.05f * spacing;
        float noiseX = Simplex.noise2d(1, 6, 0, 1f, noiseCursorX + indexOffset, 0);
        float noiseY = Simplex.noise2d(1, 6, 0, 1f, noiseCursorY + indexOffset, 0);

        // Calculate offset
        float lineHeight = getLineHeight(label);
        float x = lineHeight * noiseX * progress * distanceX * distanceXRatio * defaultDistance;
        float y = lineHeight * noiseY * progress * distanceY * distanceYRatio * defaultDistance;

        // Calculate fadeout
        float fadeout = calculateFadeout();
        x *= fadeout;
        y *= fadeout;

        // Add flag effect to X offset
        x = Math.abs(x) * -Math.signum(distanceX);

        // Apply changes
        glyph.xoffset += x;
        glyph.yoffset += y;
    }

}
