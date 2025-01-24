package mindustry.world.blocks;

import arc.audio.*;

public interface LaunchAnimator{

    void drawLanding();

    void beginLaunch(boolean launching);

    void endLaunch();

    void updateLaunching();

    float landDuration();

    Music landMusic();

    Music launchMusic();

    float zoomLaunching();
}
