package arc.graphics.g2d;

import arc.graphics.g2d.SpriteBatch.*;

import java.util.concurrent.*;

public class ForkJoinHolder{
    public final ForkJoinPool pool = ForkJoinPool.commonPool();
    public PopulateTask populateTask = new PopulateTask();
}
