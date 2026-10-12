package arc.scene.flabel.effects;

import arc.scene.flabel.*;
import arc.graphics.*;
import arc.util.*;

/** Tints the text in a rainbow pattern. */
public class RainbowEffect extends FEffect{
    private static final float defaultDistance = 0.975f, defaultFrequency = 2f;

    public float distance = 1; // How extensive the rainbow effect should be.
    public float frequency = 1; // How frequently the color pattern should move through the text.
    public float saturation = 1; // Color saturation
    public float brightness = 1; // Color brightness

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) frequency = Strings.parseFloat(params[1], 1f);
        if(params.length > 2) saturation = Strings.parseFloat(params[2], 1f);
        if(params.length > 3) brightness = Strings.parseFloat(params[3], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float distanceMod = (1f / distance) * (1f - defaultDistance);
        float frequencyMod = (1f / frequency) * defaultFrequency;
        float progress = calculateProgress(frequencyMod, distanceMod * localIndex, false);

        // Calculate color
        if(glyph.color == null){
            glyph.color = new Color(Color.white);
        }
        Color.HSVtoRGB(360f * progress, saturation * 100f, brightness * 100f, glyph.color);
    }

}
