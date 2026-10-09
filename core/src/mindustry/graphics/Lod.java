package mindustry.graphics;

import arc.math.*;

import static arc.Core.*;

public class Lod{
    private static final float threshold1 = 1.4f, threshold2 = 0.8f;
    private static final float fade = 0.2f;

    public static boolean disable = false;

    public static boolean l1 = true;
    public static float alpha1 = 1f;

    public static boolean l2 = true;
    public static float alpha2 = 1f;

    public static void update(){
        if(disable){
            l1 = l2 = true;
            alpha2 = alpha1 = 1f;
            return;
        }
        float scale = graphics.getWidth() / camera.width;

        alpha1 = Mathf.clamp((scale - threshold1) / fade);
        alpha2 = Mathf.clamp((scale - threshold2) / fade);
        l1 = alpha1 >= 1/255f;
        l2 = alpha2 >= 1/255f;
    }
}
