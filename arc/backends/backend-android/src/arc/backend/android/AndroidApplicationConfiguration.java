package arc.backend.android;

import android.hardware.*;
import arc.*;

/**
 * Class defining the configuration of an {@link AndroidApplication}. Allows you to disable the use of the accelerometer to save
 * battery among other things.
 * @author mzechner
 */
public class AndroidApplicationConfiguration{
    /** number of bits per color channel **/
    public int r = 8, g = 8, b = 8, a = 0;

    /** number of bits for depth and stencil buffer **/
    public int depth = 16, stencil = 0;

    /** number of samples for CSAA/MSAA, 2 is a good value **/
    public int numSamples = 0;

    /** whether to use the accelerometer. default: false **/
    public boolean useAccelerometer = false;

    /** whether to use the gyroscope. default: false **/
    public boolean useGyroscope = false;

    /**
     * Whether to use the compass. The compass enables {@link Input#getRotationMatrix(float[])}, if {@link #useAccelerometer} is also true.
     * <p>
     * If {@link #useRotationVectorSensor} is true and the rotation vector sensor is available, the compass will not be used.
     * <p>
     * Default: false
     **/
    public boolean useCompass = false;

    /**
     * Whether to use Android's rotation vector software sensor, which provides cleaner data than that of {@link #useCompass} for
     * {@link Input#getRotationMatrix(float[])}
     * The rotation vector sensor uses a combination of physical sensors, and it pre-filters and smoothes the data. If true,
     * {@link #useAccelerometer} is not required to enable rotation data.
     * <p>
     * If true and the rotation vector sensor is available, the compass will not be used, regardless of {@link #useCompass}.
     * <p>
     * Default: false
     */
    public boolean useRotationVectorSensor = false;

    /**
     * The requested sensor sampling rate in microseconds or one of the {@code SENSOR_DELAY_*} constants in {@link SensorManager}.
     * <p>
     * Default: {@link SensorManager#SENSOR_DELAY_GAME} (20 ms updates).
     */
    public int sensorDelay = SensorManager.SENSOR_DELAY_GAME;

    /** whether to keep the screen on and at full brightness or not while running the application. default: false. Uses FLAG_KEEP_SCREEN_ON under the hood. */
    public boolean useWakelock = false;

    /**
     * hide status bar buttons on Android 4.x and higher (API 14+). default: true
     **/
    public boolean hideStatusBar = true;

    /** whether to disable Android audio support. default: false */
    public boolean disableAudio = false;

    /** set this to true to enable Android 4.4 KitKat's 'Immersive mode' **/
    public boolean useImmersiveMode = true;
}
