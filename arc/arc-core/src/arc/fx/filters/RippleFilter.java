package arc.fx.filters;

import arc.*;
import arc.fx.*;

public class RippleFilter extends FxFilter{
    public float amount, speed;

    public RippleFilter(float amount, float speed){
        super(compileShader(
            Core.files.internal("vfxshaders/screenspace.vert"),
            Core.files.internal("vfxshaders/ripple.frag")));
        this.amount = amount;
        this.speed = speed;
        rebind();
    }

    @Override
    public void setParams(){
        shader.setUniformi("u_texture0", u_texture0);
        shader.setUniformf("u_amount", amount);
        shader.setUniformf("u_speed", speed);
        shader.setUniformf("u_time", time);
    }
}

