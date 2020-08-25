package mindustry.ui;

import arc.struct.Seq;
import arc.scene.ui.layout.Stack;
import arc.util.Time;

public class MultiReqImage extends Stack{
    private Seq<ReqImage> displays = new Seq<>();
    private float time;

    public void add(ReqImage display){
        displays.add(display);
        super.add(display);
    }

    @Override
    public void act(float delta){
        super.act(delta);

        time += Time.delta / 60f;

        displays.each(req -> req.visible = false);

        ReqImage valid = displays.find(ReqImage::valid);
        if(valid != null){
            valid.visible = true;
        }else{
            if(displays.size > 0){
                displays.get((int)time % displays.size).visible = true;
            }
        }
    }
}
