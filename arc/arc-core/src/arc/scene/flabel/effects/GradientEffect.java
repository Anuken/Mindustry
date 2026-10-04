package arc.scene.flabel.effects;

import arc.graphics.*;
import arc.scene.flabel.*;
import arc.util.*;

/** Tints the text in a gradient pattern. */
public class GradientEffect extends FEffect{
    private static final float defaultDistance = 0.975f;
    private static final float defaultFrequency = 2f;

    public Color color1 = new Color(Color.white); // First color of the gradient.
    public Color color2 = new Color(Color.white); // Second color of the gradient.
    public float distance = 1; // How extensive the rainbow effect should be.
    public float frequency = 1; // How frequently the color pattern should move through the text.

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) color1 = Strings.parseColor(params[0], color1);
        if(params.length > 1) color2 = Strings.parseColor(params[1], color2);
        if(params.length > 2) distance = Strings.parseFloat(params[2], 1f);
        if(params.length > 3) frequency = Strings.parseFloat(params[3], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float distanceMod = (1f / distance) * (1f - defaultDistance);
        float frequencyMod = (1f / frequency) * defaultFrequency;
        float progress = calculateProgress(frequencyMod, distanceMod * localIndex, true);

        // Calculate color
        if(glyph.color == null) glyph.color = new Color(Color.white);
        glyph.color.set(color1).lerp(color2, progress);
    }

}
