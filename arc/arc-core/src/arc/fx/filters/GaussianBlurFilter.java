package arc.fx.filters;

import arc.fx.util.*;

public class GaussianBlurFilter extends MultipassVfxFilter{
    private BlurType type;
    private float amount = 1f;
    private int passes = 1;

    private float invWidth, invHeight;
    private Convolve2dFilter convolve;

    public GaussianBlurFilter(){
        this(BlurType.gaussian5x5);
    }

    public GaussianBlurFilter(BlurType blurType){
        this.setType(blurType);
    }

    @Override
    public void dispose(){
        convolve.dispose();
    }

    @Override
    public void resize(int width, int height){
        this.invWidth = 1f / (float)width;
        this.invHeight = 1f / (float)height;

        convolve.resize(width, height);
        computeBlurWeightings();
    }

    @Override
    public void setParams(){
        convolve.setParams();
        computeBlurWeightings();
    }

    @Override
    public void render(PingPongBuffer buffer){
        for(int i = 0; i < this.passes; i++){
            convolve.render(buffer);

            if(i < this.passes - 1){
                buffer.swap();
            }
        }
    }

    public BlurType getType(){
        return type;
    }

    public void setType(BlurType type){
        if(type == null){
            throw new IllegalArgumentException("Blur type cannot be null.");
        }
        if(this.type != type){
            this.type = type;

            // Instantiate new matching convolve filter instance.
            if(convolve != null){
                convolve.dispose();
            }
            convolve = new Convolve2dFilter(this.type.tap.radius);

            computeBlurWeightings();
        }
    }

    /** Warning: Not all blur types support custom amounts at this time */
    public float getAmount(){
        return amount;
    }

    /** Warning: Not all blur types support custom amounts at this time */
    public void setAmount(float amount){
        this.amount = amount;
        computeBlurWeightings();
    }

    public int getPasses(){
        return passes;
    }

    public void setPasses(int passes){
        this.passes = passes;
    }

    private void computeBlurWeightings(){
        boolean hasData = true;

        float[] outWeights = convolve.weights;
        float[] outOffsetsH = convolve.offsetsHor;
        float[] outOffsetsV = convolve.offsetsVert;

        float dx = this.invWidth;
        float dy = this.invHeight;

        switch(this.type){
            case gaussian3x3:
            case gaussian5x5:
                computeKernel(this.type.tap.radius, this.amount, outWeights);
                computeOffsets(this.type.tap.radius, this.invWidth, this.invHeight, outOffsetsH, outOffsetsV);
                break;

            case gaussian3x3b:
                // Weights and offsets are computed from a binomial distribution
                // and reduced to be used *only* with bilinearly-filtered texture lookups
                // with radius = 1f

                // Weights
                outWeights[0] = 0.352941f;
                outWeights[1] = 0.294118f;
                outWeights[2] = 0.352941f;

                // Horizontal offsets
                outOffsetsH[0] = -1.33333f;
                outOffsetsH[1] = 0f;
                outOffsetsH[2] = 0f;
                outOffsetsH[3] = 0f;
                outOffsetsH[4] = 1.33333f;
                outOffsetsH[5] = 0f;

                // Vertical offsets
                outOffsetsV[0] = 0f;
                outOffsetsV[1] = -1.33333f;
                outOffsetsV[2] = 0f;
                outOffsetsV[3] = 0f;
                outOffsetsV[4] = 0f;
                outOffsetsV[5] = 1.33333f;

                // Scale offsets from binomial space to screen space
                for(int i = 0; i < convolve.length * 2; i++){
                    outOffsetsH[i] *= dx;
                    outOffsetsV[i] *= dy;
                }

                break;

            case gaussian5x5b:

                // Weights and offsets are computed from a binomial distribution
                // and reduced to be used *only* with bilinearly-filtered texture lookups
                // with radius = 2f

                // weights
                outWeights[0] = 0.0702703f;
                outWeights[1] = 0.316216f;
                outWeights[2] = 0.227027f;
                outWeights[3] = 0.316216f;
                outWeights[4] = 0.0702703f;

                // Horizontal offsets
                outOffsetsH[0] = -3.23077f;
                outOffsetsH[1] = 0f;
                outOffsetsH[2] = -1.38462f;
                outOffsetsH[3] = 0f;
                outOffsetsH[4] = 0f;
                outOffsetsH[5] = 0f;
                outOffsetsH[6] = 1.38462f;
                outOffsetsH[7] = 0f;
                outOffsetsH[8] = 3.23077f;
                outOffsetsH[9] = 0f;

                // Vertical offsets
                outOffsetsV[0] = 0f;
                outOffsetsV[1] = -3.23077f;
                outOffsetsV[2] = 0f;
                outOffsetsV[3] = -1.38462f;
                outOffsetsV[4] = 0f;
                outOffsetsV[5] = 0f;
                outOffsetsV[6] = 0f;
                outOffsetsV[7] = 1.38462f;
                outOffsetsV[8] = 0f;
                outOffsetsV[9] = 3.23077f;

                // Scale offsets from binomial space to screen space
                for(int i = 0; i < convolve.length * 2; i++){
                    outOffsetsH[i] *= dx;
                    outOffsetsV[i] *= dy;
                }

                break;
            default:
                hasData = false;
                break;
        }

        if(hasData){
            convolve.setParams();
        }
    }

    private void computeKernel(int blurRadius, float blurAmount, float[] outKernel){
        int radius = blurRadius;

        // float sigma = (float)radius / amount;
        float sigma = blurAmount;

        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float)Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;
        float distance = 0.0f;
        int index = 0;

        for(int i = -radius; i <= radius; ++i){
            distance = i * i;
            index = i + radius;
            outKernel[index] = (float)Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += outKernel[index];
        }

        int size = (radius * 2) + 1;
        for(int i = 0; i < size; ++i){
            outKernel[i] /= total;
        }
    }

    private void computeOffsets(int blurRadius, float dx, float dy, float[] outOffsetH, float[] outOffsetV){
        int radius = blurRadius;

        final int X = 0, Y = 1;
        for(int i = -radius, j = 0; i <= radius; ++i, j += 2){
            outOffsetH[j + X] = i * dx;
            outOffsetH[j + Y] = 0;

            outOffsetV[j + X] = 0;
            outOffsetV[j + Y] = i * dy;
        }
    }

    private enum Tap{
        tap3x3(1),
        tap5x5(2),
        // Tap7x7(3),
        ;

        public final int radius;

        Tap(int radius){
            this.radius = radius;
        }
    }

    public enum BlurType{
        gaussian3x3(Tap.tap3x3), gaussian3x3b(Tap.tap3x3), // R=5 (11x11, policy "higher-then-discard")
        gaussian5x5(Tap.tap5x5), gaussian5x5b(Tap.tap5x5), // R=9 (19x19, policy "higher-then-discard")
        ;

        public final Tap tap;

        BlurType(Tap tap){
            this.tap = tap;
        }
    }
}
