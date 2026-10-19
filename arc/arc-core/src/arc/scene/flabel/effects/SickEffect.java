package arc.scene.flabel.effects;

import arc.math.*;
import arc.scene.flabel.*;
import arc.struct.*;
import arc.util.*;

/** Drips the text in a random pattern. */
public class SickEffect extends FEffect{
    private static final float defaultFrequency = 50f, defaultDistance = .125f, defaultIntensity = 1f;

    public float distance = 1; // How far the glyphs should move
    public float intensity = 1; // How fast the glyphs should move

    private IntSeq indices = new IntSeq();

    @Override
    public void applyParams(String[] params){
        if(params.length > 0) distance = Strings.parseFloat(params[0], 1f);
        if(params.length > 1) intensity = Strings.parseFloat(params[1], 1f);
    }

    @Override
    protected void onApply(FLabel label, FGlyph glyph, int localIndex, float delta){
        // Calculate progress
        float progressModifier = (1f / intensity) * defaultIntensity;
        float progressOffset = localIndex / defaultFrequency;
        float progress = calculateProgress(progressModifier, -progressOffset, false);

        if(progress < .01f && Math.random() > .25f && !indices.contains(localIndex))
            indices.add(localIndex);
        if(progress > .95f)
            indices.removeValue(localIndex);

        if(!indices.contains(localIndex) &&
                !indices.contains(localIndex - 1) &&
                !indices.contains(localIndex - 2) &&
                !indices.contains(localIndex + 2) &&
                !indices.contains(localIndex + 1))
            return;

        // Calculate offset
        float interpolation = 0;
        float split = 0.5f;
        if(progress < split){
            interpolation = Interp.pow2Out.apply(0, 1, progress / split);
        }else{
            interpolation = Interp.pow2In.apply(1, 0, (progress - split) / (1f - split));
        }
        float y = getLineHeight(label) * distance * interpolation * defaultDistance;

        if(indices.contains(localIndex))
            y *= 2.15f;
        if(indices.contains(localIndex - 1) || indices.contains(localIndex + 1))
            y *= 1.35f;

        // Calculate fadeout
        float fadeout = calculateFadeout();
        y *= fadeout;

        // Apply changes
        glyph.yoffset -= y;
    }

}
