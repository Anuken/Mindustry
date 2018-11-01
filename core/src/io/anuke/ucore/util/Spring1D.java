package io.anuke.ucore.util;

import com.badlogic.gdx.math.MathUtils;

/** @author xSke*/
public class Spring1D {
    public float value;
    public float target;
    public float velocity;

    public float damping;
    public float frequency;

    public Spring1D(float damping, float frequency) {
        this.damping = damping;
        this.frequency = frequency;
    }

    public void update(float deltaTime) {
        float angularFrequency = frequency;
        angularFrequency *= MathUtils.PI2;

        float f = 1.0f + 2.0f * deltaTime * damping * angularFrequency;
        float oo = angularFrequency * angularFrequency;
        float hoo = deltaTime * oo;
        float hhoo = deltaTime * hoo;
        float detInv = 1.0f / (f + hhoo);
        float detX = f * value + deltaTime * velocity + hhoo * target;
        float detV = velocity + hoo * (target - value);
        value = detX * detInv;
        velocity = detV * detInv;
    }
}
