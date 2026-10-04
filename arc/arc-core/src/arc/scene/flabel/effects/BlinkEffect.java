package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.graphics.*;
import arc.util.*;

/** Blinks the entire text in two different colors at once, without interpolation. */
public class BlinkEffect extends FEffect{
    private static final float defaultFrequency = 1f;

    public Color color1 = new Color(Color.white); // First color of the effect.
    public Color color2 = new Color(Color.white); // Second color of the effect.
    public float frequency = 1; // How frequently the color pattern should move through the text.
    public float threshold = 0.5f; // Point to switch colors.

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) color1 = Strings.parseColor(params[0], color1);
        if(params.length > 1) color2 = Strings.parseColor(params[1], color2);
        if(params.length > 2) frequency = Strings.parseFloat(params[2], 1f);
        if(params.length > 3) threshold = Strings.parseFloat(params[3], 0.5f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float frequencyMod = (1f / frequency) * defaultFrequency;
        float progress = calculateProgress(frequencyMod);

        // Calculate color
        if(glyph.color == null) glyph.color = new Color(Color.white);
        glyph.color.set(progress <= threshold ? color1 : color2);
    }

}
