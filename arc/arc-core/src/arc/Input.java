package arc;

import arc.func.*;
import arc.input.*;
import arc.input.KeyBind.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

/**
 * <p>
 * Interface to the input facilities. This allows polling the state of the keyboard, the touch screen and the accelerometer. On
 * some backends (desktop) the touch screen is replaced by mouse input. The accelerometer is of course not available on
 * all backends.
 * </p>
 *
 * <p>
 * The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen
 * keyboards, and cursor capture. Support for simple input dialogs is also provided.
 * </p>
 * @author mzechner
 */
public abstract class Input{
    /** The default input device (keyboard) */
    protected KeyboardDevice keyboard = new KeyboardDevice();
    /** An input multiplexer to handle events. */
    protected InputMultiplexer inputMultiplexer = new InputMultiplexer(keyboard);
    /** List of caught keys for Android. */
    protected IntSet caughtKeys = new IntSet();
    /** Return Vec2 value for various functions. */
    protected Vec2 mouseReturn = new Vec2();
    /** Whether to use keyboard controls on Android. */
    protected boolean useKeyboard;

    /**Returns the unprojected mouse position (screen -> world).*/
    public Vec2 mouseWorld(float x, float y){
        return Core.camera.unproject(mouseReturn.set(x, y));
    }

    /**Returns the projected mouse position (world -> screen).*/
    public Vec2 mouseScreen(float x, float y){
        return Core.camera.project(mouseReturn.set(x, y));
    }

    /** @return the unprojected mouse position in the world.*/
    public float mouseWorldX(){
        return Core.camera.unproject(mouse()).x;
    }

    /** @return the unprojected mouse position in the world.*/
    public float mouseWorldY(){
        return Core.camera.unproject(mouse()).y;
    }

    /**Returns the unprojected mouse position in the world.*/
    public Vec2 mouseWorld(){
        return Core.camera.unproject(mouse());
    }

    /**Returns the mouse position as a Vec2.*/
    public Vec2 mouse(){
        return mouseReturn.set(mouseX(), mouseY());
    }

    public void setUseKeyboard(boolean useKeyboard){
        this.useKeyboard = useKeyboard;
    }

    /** @return whether the keyboard should be preferred for mobile devices - used in text fields. */
    public boolean useKeyboard(){
        return useKeyboard;
    }

    /**
     * @return The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the top left corner.
     */
    public abstract int mouseX();

    /**
     * Returns the x coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * @param pointer the pointer id.
     * @return the x coordinate
     */
    public abstract int mouseX(int pointer);

    /** @return the different between the current pointer location and the last pointer location on the x-axis. */
    public abstract int deltaX();

    /** @return the different between the current pointer location and the last pointer location on the x-axis. */
    public abstract int deltaX(int pointer);

    /**
     * @return The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first
     * pointer in screen coordinates. The screen origin is the bottom left corner.
     */
    public abstract int mouseY();

    /**
     * Returns the y coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id
     * identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * @param pointer the pointer id.
     * @return the y coordinate
     */
    public abstract int mouseY(int pointer);

    /** @return the different between the current pointer location and the last pointer location on the y-axis. */
    public abstract int deltaY();

    /** @return the different between the current pointer location and the last pointer location on the y-axis. */
    public abstract int deltaY(int pointer);

    /** @return whether the screen is currently touched. */
    public abstract boolean isTouched();

    /** @return whether a new touch down event just occurred. */
    public abstract boolean justTouched();

    public int getTouches(){
        int sum = 0;
        for(int i = 0; i < 10; i++){
            if(isTouched(i)) sum ++;
        }
        return sum;
    }

    /**
     * Whether the screen is currently touched by the pointer with the given index. Pointers are indexed from 0 to n. The pointer
     * id identifies the order in which the fingers went down on the screen, e.g. 0 is the first finger, 1 is the second and so on.
     * When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on
     * the touch screen the first free index will be used.
     * @param pointer the pointer
     * @return whether the screen is touched by the pointer
     */
    public abstract boolean isTouched(int pointer);

    /** @return the pressure of the first pointer */
    public float getPressure(){
        return getPressure(0);
    }

    /**
     * Returns the pressure of the given pointer, where 0 is untouched. On Android it should be
     * up to 1.0, but it can go above that slightly and its not consistent between devices. On iOS 1.0 is the normal touch
     * and significantly more of hard touch. Check relevant manufacturer documentation for details.
     * Check availability with {@link Input#isPeripheralAvailable(Peripheral)}. If not supported, returns 1.0 when touched.
     * @param pointer the pointer id.
     * @return the pressure
     */
    public float getPressure(int pointer){
        return isTouched(pointer) ? 1f : 0f;
    }

    /** Returns whether one of the two shift keys is currently pressed.*/
    public boolean shift(){
        return keyDown(KeyCode.shiftLeft) || keyDown(KeyCode.shiftRight);
    }

    /** Returns whether one of the two control keys is currently pressed - or, on Macs, the cmd key.*/
    public boolean ctrl(){
        return OS.isMac ? keyDown(KeyCode.sym) : keyDown(KeyCode.controlLeft) || keyDown(KeyCode.controlRight);
    }

    /** Returns whether one of the two alt keys is pressed.*/
    public boolean alt(){
        return keyDown(KeyCode.altLeft) || keyDown(KeyCode.altRight);
    }

    /** Returns whether the key is pressed. */
    public boolean keyDown(KeyCode key){
        return keyboard.isPressed(key);
    }

    /** Returns whether the key has just been pressed. */
    public boolean keyTap(KeyCode key){
        return keyboard.isTapped(key);
    }

    /** Returns whether the key has just been released. */
    public boolean keyRelease(KeyCode key){
        return keyboard.isReleased(key);
    }

    /** Returns the [-1, 1] axis value of a key. */
    public float axis(KeyCode key){
        return keyboard.getAxis(key);
    }

    /** Returns whether the keybind is pressed. */
    public boolean keyDown(KeyBind key){
        return key.value.key != null && keyboard.isPressed(key.value.key);
    }

    /** Returns whether the key has just been pressed. */
    public boolean keyTap(KeyBind key){
        return key.value.key != null && keyboard.isTapped(key.value.key);
    }

    /** Returns whether the key has just been released. */
    public boolean keyRelease(KeyBind key){
        return key.value.key != null && keyboard.isReleased(key.value.key);
    }

    /** Returns the [-1, 1] axis value of a key. */
    public float axis(KeyBind key){
        Axis axis = key.value;
        if(axis.key != null){
            return keyboard.getAxis(axis.key);
        }else{
            return keyboard.isPressed(axis.min) && keyboard.isPressed(axis.max) ? 0 :
                    keyboard.isPressed(axis.min) ? -1 : keyboard.isPressed(axis.max) ? 1 : 0;
        }
    }

    /** Returns the [-1, 1] axis value of a key.
     * In the case of keyboard-based axes, this will only return a value if one of the axes was just pressed. */
    public float axisTap(KeyBind key){
        Axis axis = key.value;
        if(axis.key != null){
            return keyboard.getAxis(axis.key);
        }else{
            return keyboard.isTapped(axis.min) ? -1 : keyboard.isTapped(axis.max) ? 1 : 0;
        }
    }

    /**
     * System dependent method to input a string of text. A dialog box will be created with the given title and the given text as a
     * message for the user. Once the dialog has been closed the consumer be called on the rendering thread.
     */
    public void getTextInput(TextInput input){
    }

    /** @return on mobile, whether text input is currently being fetched. Not implemented on other platforms. */
    public boolean isShowingTextInput(){
        return false;
    }

    /**
     * Sets the on-screen keyboard visible if available. Only applicable on mobile.
     * @param visible visible or not
     */
    public void setOnscreenKeyboardVisible(boolean visible){
    }

    /**
     * Vibrates for the given amount of time. Note that you'll need the permission
     * <code> <uses-permission android:name="android.permission.VIBRATE" /></code> in your manifest file in order for this to work.
     * @param milliseconds the number of milliseconds to vibrate.
     */
    public void vibrate(int milliseconds){
    }

    /**
     * Vibrate with a given pattern. Pass in an array of ints that are the times at which to turn on or off the vibrator. The first
     * one is how long to wait before turning it on, and then after that it alternates. If you want to repeat, pass the index into
     * the pattern at which to start the repeat.
     * @param pattern an array of longs of times to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if you don't want to repeat.
     */
    public void vibrate(long[] pattern, int repeat){
    }

    /** Stops the vibrator */
    public void cancelVibrate(){
    }

    /** @return The acceleration force in m/s^2 applied to the device, including the force of gravity */
    public Vec3 getAccelerometer(){
        return Vec3.Zero;
    }

    /** @return The rate of rotation in rad/s. */
    public Vec3 getGyroscope(){
        return Vec3.Zero;
    }

    /** @return the device's orientation in degrees in the format (pitch, roll, azimuth) corresponding to x,y,z. */
    public Vec3 getOrientation(){
        return Vec3.Zero;
    }

    /**
     * Returns the rotation matrix describing the devices rotation as per <a href=
     * "http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])"
     * >SensorManager#getRotationMatrix(float[], float[], float[], float[])</a>. Does not manipulate the matrix if the platform
     * does not have an accelerometer.
     */
    public void getRotationMatrix(float[] matrix){
    }

    /** @return the time of the event currently reported to the {@link InputProcessor}. */
    public abstract long getCurrentEventTime();

    /**
     * Sets whether the specified button on Android should be caught. This will prevent the app from processing the key. Will have no effect
     * on the desktop.
     * @param c whether to catch the button
     */
    public void setCatch(KeyCode code, boolean c){
        if(c){
            caughtKeys.add(code.ordinal());
        }else{
            caughtKeys.remove(code.ordinal());
        }
    }

    /** @return whether the back button is currently being caught */
    public boolean isCatch(KeyCode code){
        return caughtKeys.contains(code.ordinal());
    }

    /**
     * Adds a {@link InputProcessor} that will receive all touch and key input events. It will be called before the
     * {@link ApplicationListener#update()} method each frame.
     * @param processor the InputProcessor
     * */
    public void addProcessor(InputProcessor processor){
        inputMultiplexer.addProcessor(processor);
    }

    /**Removes a {@link InputProcessor} from the chain.*/
    public void removeProcessor(InputProcessor processor){
        inputMultiplexer.removeProcessor(processor);
    }

    /** @return the currently set {@link InputProcessor} or null. */
    public Seq<InputProcessor> getInputProcessors(){
        return inputMultiplexer.getProcessors();
    }

    public InputMultiplexer getInputMultiplexer(){
        return inputMultiplexer;
    }

    /** Returns the default input device (keyboard). */
    public KeyboardDevice getKeyboard(){
        return keyboard;
    }

    /**
     * Queries whether a {@link Peripheral} is currently available. In case of Android and the {@link Peripheral#hardwareKeyboard}
     * this returns the whether the keyboard is currently slid out or not.
     * @param peripheral the {@link Peripheral}
     * @return whether the peripheral is available or not.
     */
    public boolean isPeripheralAvailable(Peripheral peripheral){
        return peripheral == Peripheral.hardwareKeyboard;
    }

    /** @return the rotation of the device with respect to its native orientation. */
    public int getRotation(){
        return 0;
    }

    /** @return the native orientation of the device. */
    public Orientation getNativeOrientation(){
        return Orientation.landscape;
    }

    /** Internal method for looking up a key's name based on keyboard layout. Internal use only - use {@link KeyCode#getName()} instead. */
    public String getKeyName(KeyCode code){
        return code.value;
    }

    public enum Orientation{
        landscape, portrait
    }

    /** Enumeration of potentially available peripherals. Use with {@link Input#isPeripheralAvailable(Peripheral)}. */
    public enum Peripheral{
        hardwareKeyboard, onscreenKeyboard, multitouchScreen, accelerometer, compass, vibrator, gyroscope, rotationVector, pressure
    }

    /** Parameters for text input. */
    public static class TextInput{
        public boolean multiline = false;
        public boolean allowEmpty = true;
        public String title = "";
        public String text = "";
        public String message = "";
        public boolean numeric;
        public Cons<String> accepted = s -> { };
        public Runnable canceled = () -> { };
        public int maxLength = -1;
    }
}
