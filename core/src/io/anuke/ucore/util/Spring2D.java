package io.anuke.ucore.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** @author xSke*/
public class Spring2D {
    public Vector2 value = new Vector2();
    public Vector2 target = new Vector2();
    public Vector2 velocity = new Vector2();

    public float damping;
    public float frequency;

    public Spring2D(float damping, float frequency) {
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

        {
            float detX = f * value.x + deltaTime * velocity.x + hhoo * target.x;
            float detV = velocity.x + hoo * (target.x - value.x);
            value.x = detX * detInv;
            velocity.x = detV * detInv;
        }

        {
            float detX = f * value.y + deltaTime * velocity.y + hhoo * target.y;
            float detV = velocity.y + hoo * (target.y - value.y);
            value.y = detX * detInv;
            velocity.y = detV * detInv;
        }
    }
}
