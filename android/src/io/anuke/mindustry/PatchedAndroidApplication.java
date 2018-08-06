package io.anuke.mindustry;

import com.badlogic.gdx.backends.android.AndroidApplication;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatchedAndroidApplication extends AndroidApplication {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    protected void onPause () {
        if(useImmersiveMode) {
            exec.submit(() -> {
                try {Thread.sleep(100);} catch (InterruptedException ignored) {}
                graphics.onDrawFrame(null);
            });
        }
        super.onPause();
    }
}