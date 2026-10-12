package arc.util;

import arc.math.geom.Vec2;

/**
 * Various scaling types for fitting one rectangle into another.
 * @author Nathan Sweet
 */
public enum Scaling{
    /**
     * Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller than the
     * target in one direction.
     */
    fit,
    /**
     * Scales the source to fit the target if it is larger, otherwise does not scale.
     */
    bounded,
    /**
     * Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger than the
     * target in one direction.
     */
    fill,
    /**
     * Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the y direction.
     */
    fillX,
    /**
     * Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the source to be
     * smaller or larger than the target in the x direction.
     */
    fillY,
    /** Scales the source to fill the target. This may cause the source to not keep the same aspect ratio. */
    stretch,
    /**
     * Scales the source to fill the target in the x direction, without changing the y direction. This may cause the source to not
     * keep the same aspect ratio.
     */
    stretchX,
    /**
     * Scales the source to fill the target in the y direction, without changing the x direction. This may cause the source to not
     * keep the same aspect ratio.
     */
    stretchY,
    /** The source is not scaled. */
    none;

    private static final Vec2 temp = new Vec2();

    /**
     * Returns the size of the source scaled to the target. Note the same Vec2 instance is always returned and should never be
     * cached.
     */
    public Vec2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight){
        switch(this){
            case fit:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fill:{
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio < sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fillX:{
                float scale = targetWidth / sourceWidth;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case fillY:{
                float scale = targetHeight / sourceHeight;
                temp.x = sourceWidth * scale;
                temp.y = sourceHeight * scale;
                break;
            }
            case stretch:
                temp.x = targetWidth;
                temp.y = targetHeight;
                break;
            case stretchX:
                temp.x = targetWidth;
                temp.y = sourceHeight;
                break;
            case stretchY:
                temp.x = sourceWidth;
                temp.y = targetHeight;
                break;
            case bounded:
                if(sourceHeight > targetHeight || sourceWidth > targetWidth){
                    return fit.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }else{
                    return none.apply(sourceWidth, sourceHeight, targetWidth, targetHeight);
                }
            case none:
                temp.x = sourceWidth;
                temp.y = sourceHeight;
                break;
        }
        return temp;
    }
}
