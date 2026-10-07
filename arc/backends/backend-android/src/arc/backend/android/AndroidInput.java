package arc.backend.android;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.text.*;
import android.text.InputFilter.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.*;
import android.widget.*;
import arc.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.Log;
import arc.util.pooling.*;

import java.util.*;

/**
 * An implementation of the {@link Input} interface for Android.
 * @author mzechner
 */
//uses legacy APIs for vibration and key input that have no good equivalent
@SuppressWarnings("deprecation")
public class AndroidInput extends Input implements OnKeyListener, OnTouchListener, OnGenericMotionListener{
    static final int maxTouches = 20;

    protected final float[] accelerometerValues = new float[3];
    protected final float[] gyroscopeValues = new float[3];
    protected final Vibrator vibrator;
    protected final float[] magneticFieldValues = new float[3];
    protected final float[] rotationVectorValues = new float[3];
    protected final Orientation nativeOrientation;
    final boolean hasMultitouch;
    final AndroidApplication app;
    final Context context;
    final float[] R = new float[9];
    final float[] orientation = new float[3];
    private final AndroidApplicationConfiguration config;
    boolean accelerometerAvailable = false;
    boolean gyroscopeAvailable = false;
    ArrayList<OnKeyListener> keyListeners = new ArrayList<>();
    ArrayList<KeyEvent> keyEvents = new ArrayList<>();
    ArrayList<TouchEvent> touchEvents = new ArrayList<>();
    int[] touchX = new int[maxTouches];
    int[] touchY = new int[maxTouches];
    int[] deltaX = new int[maxTouches];
    int[] deltaY = new int[maxTouches];
    boolean[] touched = new boolean[maxTouches];
    int[] button = new int[maxTouches];
    int[] realId = new int[maxTouches];
    float[] pressure = new float[maxTouches];
    boolean keyboardAvailable;
    Pool<KeyEvent> usedKeyEvents = new Pool<KeyEvent>(16, 1000){
        @Override
        protected KeyEvent newObject(){
            return new KeyEvent();
        }
    };
    Pool<TouchEvent> usedTouchEvents = new Pool<TouchEvent>(16, 1000){
        @Override
        protected TouchEvent newObject(){
            return new TouchEvent();
        }
    };
    boolean requestFocus = true;
    private SensorManager manager;
    private Handler handle;
    private boolean compassAvailable = false;
    private boolean rotationVectorAvailable = false;
    private float azimuth = 0;
    private float pitch = 0;
    private float roll = 0;
    private int mouseLastX = 0;
    private int mouseLastY = 0;
    private boolean justTouched = false;
    private boolean showingTextInput;
    private long currentEventTimeStamp = System.nanoTime();
    private Vec3 accel = new Vec3(), gyro = new Vec3(), orient = new Vec3();
    private SensorEventListener accelerometerListener;
    private SensorEventListener gyroscopeListener;
    private SensorEventListener compassListener;
    private SensorEventListener rotationVectorListener;

    public AndroidInput(AndroidApplication activity, Context context, View view, AndroidApplicationConfiguration config){
        view.setOnKeyListener(this);
        view.setOnTouchListener(this);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnGenericMotionListener(this);
        view.requestFocus();
        this.config = config;
        Arrays.fill(realId, -1);
        handle = new Handler();
        this.app = activity;
        this.context = context;
        hasMultitouch = activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen.multitouch");

        vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

        int rotation = getRotation();
        DisplayMetrics mode = new DisplayMetrics();
        app.getWindowManager().getDefaultDisplay().getMetrics(mode);

        if(((rotation == 0 || rotation == 180) && (mode.widthPixels >= mode.heightPixels))
        || ((rotation == 90 || rotation == 270) && (mode.widthPixels <= mode.heightPixels))){
            nativeOrientation = Orientation.landscape;
        }else{
            nativeOrientation = Orientation.portrait;
        }
    }

    @Override
    public Vec3 getAccelerometer(){
        return accel.set(accelerometerValues);
    }

    @Override
    public Vec3 getGyroscope(){
        return gyro.set(gyroscopeValues);
    }

    @Override
    public Vec3 getOrientation(){
        if(!compassAvailable && !rotationVectorAvailable) return Vec3.Zero;

        updateOrientation();

        return orient.set(pitch, roll, azimuth);
    }

    @Override
    public void getTextInput(TextInput info){
        handle.post(() -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            if(!info.message.isEmpty()) alert.setMessage(info.message);
            //alert.setTitle(info.title);
            final EditText input = new EditText(context);
            input.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
            input.setText(info.text);
            if(info.numeric){
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            if(info.maxLength != -1){
                input.setFilters(new InputFilter[]{new LengthFilter(info.maxLength)});
            }
            if(!info.multiline) input.setSingleLine();
            //haha yes
            try{
                input.setSelection(info.text.length());
            }catch(Exception ignored){}
            if(info.title != null && info.title.length() > 0){
                alert.setTitle(info.title);
            }
            alert.setView(input);
            alert.setPositiveButton(context.getString(android.R.string.ok), (dialog, whichButton) -> Core.app.post(() -> info.accepted.get(input.getText().toString())));
            alert.setNegativeButton(context.getString(android.R.string.cancel), (dialog, whichButton) -> Core.app.post(() -> info.canceled.run()));
            alert.setOnCancelListener(a -> Core.app.post(() -> info.canceled.run()));
            alert.setOnDismissListener(a -> showingTextInput = false);

            showingTextInput = true;
            AlertDialog dialog = alert.show();

            input.addTextChangedListener(new TextWatcher(){
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2){

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2){

                }

                @Override
                public void afterTextChanged(Editable editable){
                    if(!info.allowEmpty){
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!input.getText().toString().trim().isEmpty());
                    }
                }
            });

            if(!info.allowEmpty){
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!input.getText().toString().trim().isEmpty());
            }

            input.requestFocus();

            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
    }

    @Override
    public boolean isShowingTextInput(){
        return showingTextInput;
    }

    @Override
    public int mouseX(){
        synchronized(this){
            return touchX[0];
        }
    }

    @Override
    public int mouseY(){
        synchronized(this){
            return touchY[0];
        }
    }

    @Override
    public int mouseX(int pointer){
        synchronized(this){
            return touchX[pointer];
        }
    }

    @Override
    public int mouseY(int pointer){
        synchronized(this){
            return touchY[pointer];
        }
    }

    @Override
    public boolean isTouched(int pointer){
        synchronized(this){
            return touched[pointer];
        }
    }

    @Override
    public float getPressure(){
        return getPressure(0);
    }

    @Override
    public float getPressure(int pointer){
        return pressure[pointer];
    }

    @Override
    public boolean isTouched(){
        synchronized(this){
            if(hasMultitouch){
                for(int pointer = 0; pointer < maxTouches; pointer++){
                    if(touched[pointer]){
                        return true;
                    }
                }
            }
            return touched[0];
        }
    }

    void processDevices(){
        keyboard.postUpdate();
    }

    void processEvents(){
        synchronized(this){
            justTouched = false;

            final InputProcessor processor = this.inputMultiplexer;

            int len = keyEvents.size();
            for(int i = 0; i < len; i++){
                KeyEvent e = keyEvents.get(i);
                currentEventTimeStamp = e.timeStamp;
                switch(e.type){
                    case KeyEvent.KEY_DOWN:
                        processor.keyDown(e.keyCode);
                        break;
                    case KeyEvent.KEY_UP:
                        processor.keyUp(e.keyCode);
                        break;
                    case KeyEvent.KEY_TYPED:
                        processor.keyTyped(e.keyChar);
                }
                usedKeyEvents.free(e);
            }

            len = touchEvents.size();
            for(int i = 0; i < len; i++){
                TouchEvent e = touchEvents.get(i);
                currentEventTimeStamp = e.timeStamp;
                switch(e.type){
                    case TouchEvent.TOUCH_DOWN:
                        processor.touchDown(e.x, e.y, e.pointer, e.button);
                        justTouched = true;
                        break;
                    case TouchEvent.TOUCH_UP:
                        processor.touchUp(e.x, e.y, e.pointer, e.button);
                        break;
                    case TouchEvent.TOUCH_DRAGGED:
                        processor.touchDragged(e.x, e.y, e.pointer);
                        break;
                    case TouchEvent.TOUCH_MOVED:
                        processor.mouseMoved(e.x, e.y);
                        break;
                    case TouchEvent.TOUCH_SCROLLED:
                        processor.scrolled(e.scrollAmountX, e.scrollAmountY);
                }
                usedTouchEvents.free(e);
            }


            if(touchEvents.isEmpty()){
                for(int i = 0; i < deltaX.length; i++){
                    deltaX[0] = 0;
                    deltaY[0] = 0;
                }
            }

            keyEvents.clear();
            touchEvents.clear();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event){
        if(requestFocus && view != null){
            view.setFocusableInTouchMode(true);
            view.requestFocus();
            requestFocus = false;
        }

        // synchronized in handler.postTouchEvent()
        handleTouch(event, this);

        return true;
    }

    public void handleTouch(MotionEvent event, AndroidInput input){
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = event.getPointerId(pointerIndex);

        int x, y;
        int realPointerIndex;
        KeyCode button;

        long timeStamp = System.nanoTime();
        synchronized(input){
            switch(action){
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    realPointerIndex = input.getFreePointerIndex(); // get a free pointer index as reported by Input.getX() etc.
                    if(realPointerIndex >= AndroidInput.maxTouches) break;
                    input.realId[realPointerIndex] = pointerId;
                    x = (int)event.getX(pointerIndex);
                    y = (int)event.getY(pointerIndex);
                    button = toButton(event.getButtonState());
                    if(button != KeyCode.unknown)
                        postTouchEvent(input, TouchEvent.TOUCH_DOWN, x, y, realPointerIndex, button, timeStamp);
                    input.touchX[realPointerIndex] = x;
                    input.touchY[realPointerIndex] = Core.graphics.getHeight() - 1 - y;
                    input.deltaX[realPointerIndex] = 0;
                    input.deltaY[realPointerIndex] = 0;
                    input.touched[realPointerIndex] = (button != KeyCode.unknown);
                    input.button[realPointerIndex] = button.ordinal();
                    input.pressure[realPointerIndex] = event.getPressure(pointerIndex);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_OUTSIDE:
                    realPointerIndex = input.lookUpPointerIndex(pointerId);
                    if(realPointerIndex == -1) break;
                    if(realPointerIndex >= AndroidInput.maxTouches) break;
                    input.realId[realPointerIndex] = -1;
                    x = (int)event.getX(pointerIndex);
                    y = (int)event.getY(pointerIndex);
                    button = KeyCode.byOrdinal(input.button[realPointerIndex]);
                    if(button != KeyCode.unknown)
                        postTouchEvent(input, TouchEvent.TOUCH_UP, x, y, realPointerIndex, button, timeStamp);
                    input.touchX[realPointerIndex] = x;
                    input.touchY[realPointerIndex] = Core.graphics.getHeight() - 1 - y;
                    input.deltaX[realPointerIndex] = 0;
                    input.deltaY[realPointerIndex] = 0;
                    input.touched[realPointerIndex] = false;
                    input.button[realPointerIndex] = 0;
                    input.pressure[realPointerIndex] = 0;
                    break;

                case MotionEvent.ACTION_CANCEL:
                    for(int i = 0; i < input.realId.length; i++){
                        input.realId[i] = -1;
                        input.touchX[i] = 0;
                        input.touchY[i] = 0;
                        input.deltaX[i] = 0;
                        input.deltaY[i] = 0;
                        input.touched[i] = false;
                        input.button[i] = 0;
                        input.pressure[i] = 0;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    int pointerCount = event.getPointerCount();
                    for(int i = 0; i < pointerCount; i++){
                        pointerIndex = i;
                        pointerId = event.getPointerId(pointerIndex);
                        x = (int)event.getX(pointerIndex);
                        y = (int)event.getY(pointerIndex);
                        realPointerIndex = input.lookUpPointerIndex(pointerId);
                        if(realPointerIndex == -1) continue;
                        if(realPointerIndex >= AndroidInput.maxTouches) break;
                        button = KeyCode.byOrdinal(input.button[realPointerIndex]);
                        if(button != KeyCode.unknown)
                            postTouchEvent(input, TouchEvent.TOUCH_DRAGGED, x, y, realPointerIndex, button, timeStamp);
                        else
                            postTouchEvent(input, TouchEvent.TOUCH_MOVED, x, y, realPointerIndex, KeyCode.mouseLeft, timeStamp);
                        input.deltaX[realPointerIndex] = x - input.touchX[realPointerIndex];
                        input.deltaY[realPointerIndex] = -(y - input.touchY[realPointerIndex]);
                        input.touchX[realPointerIndex] = x;
                        input.touchY[realPointerIndex] = Core.graphics.getHeight() - 1 - y;
                        input.pressure[realPointerIndex] = event.getPressure(pointerIndex);
                    }
                    break;
            }
        }
        Core.graphics.requestRendering();
    }

    private KeyCode toButton(int button){
        if(button == 0 || button == 1) return KeyCode.mouseLeft;
        if(button == 2) return KeyCode.mouseRight;
        if(button == 4) return KeyCode.mouseMiddle;
        if(button == 8) return KeyCode.mouseBack;
        if(button == 16) return KeyCode.mouseForward;
        return KeyCode.unknown;
    }

    private void postTouchEvent(AndroidInput input, int type, int x, int y, int pointer, KeyCode button, long timeStamp){
        TouchEvent event = input.usedTouchEvents.obtain();
        event.timeStamp = timeStamp;
        event.pointer = pointer;
        event.x = x;
        event.y = Core.graphics.getHeight() - y - 1;
        event.type = type;
        event.button = button;
        input.touchEvents.add(event);
    }

    public void onTap(int x, int y){
        postTap(x, y);
    }

    public void onDrop(int x, int y){
        postTap(x, y);
    }

    protected void postTap(int x, int y){
        synchronized(this){
            TouchEvent event = usedTouchEvents.obtain();
            event.timeStamp = System.nanoTime();
            event.pointer = 0;
            event.x = x;
            event.y = app.graphics.getHeight() - y - 1;
            event.type = TouchEvent.TOUCH_DOWN;
            touchEvents.add(event);

            event = usedTouchEvents.obtain();
            event.timeStamp = System.nanoTime();
            event.pointer = 0;
            event.x = x;
            event.y = app.graphics.getHeight() - y - 1;
            event.type = TouchEvent.TOUCH_UP;
            touchEvents.add(event);
        }
        Core.graphics.requestRendering();
    }

    @Override
    public boolean onKey(View v, int keyCode, android.view.KeyEvent e){
        for(int i = 0, n = keyListeners.size(); i < n; i++)
            if(keyListeners.get(i).onKey(v, keyCode, e)) return true;

        // If the key is held sufficiently long that it repeats, then the initial down is followed
        // additional key events with ACTION_DOWN and a non-zero value for getRepeatCount().
        // We are only interested in the first key down event here and must ignore all others
        if(e.getAction() == android.view.KeyEvent.ACTION_DOWN && e.getRepeatCount() > 0)
            return caughtKeys.contains(AndroidInputMap.getKeyCode(keyCode).ordinal());

        synchronized(this){
            KeyEvent event;

            if(e.getKeyCode() == android.view.KeyEvent.KEYCODE_UNKNOWN && e.getAction() == android.view.KeyEvent.ACTION_MULTIPLE){
                String chars = e.getCharacters();
                for(int i = 0; i < chars.length(); i++){
                    event = usedKeyEvents.obtain();
                    event.timeStamp = System.nanoTime();
                    event.keyCode = KeyCode.unknown;
                    event.keyChar = chars.charAt(i);
                    event.type = KeyEvent.KEY_TYPED;
                    keyEvents.add(event);
                }
                return false;
            }

            char character = (char)e.getUnicodeChar();
            // Android doesn't report a unicode char for back space. hrm...
            if(keyCode == 67) character = '\b';
            if(e.getKeyCode() < 0){
                return false;
            }

            KeyCode code = AndroidInputMap.getKeyCode(e.getKeyCode());

            switch(e.getAction()){
                case android.view.KeyEvent.ACTION_DOWN:
                    event = usedKeyEvents.obtain();
                    event.timeStamp = System.nanoTime();
                    event.keyChar = 0;
                    event.keyCode = code;
                    event.type = KeyEvent.KEY_DOWN;

                    // Xperia hack for circle key. gah...
                    if(keyCode == android.view.KeyEvent.KEYCODE_BACK && e.isAltPressed()){
                        keyCode = 255;
                        event.keyCode = KeyCode.buttonCircle;
                    }

                    keyEvents.add(event);
                    break;
                case android.view.KeyEvent.ACTION_UP:
                    long timeStamp = System.nanoTime();
                    event = usedKeyEvents.obtain();
                    event.timeStamp = timeStamp;
                    event.keyChar = 0;
                    event.keyCode = code;
                    event.type = KeyEvent.KEY_UP;
                    // Xperia hack for circle key. gah...
                    if(keyCode == android.view.KeyEvent.KEYCODE_BACK && e.isAltPressed()){
                        keyCode = 255;
                        event.keyCode = KeyCode.buttonCircle;
                    }
                    keyEvents.add(event);

                    event = usedKeyEvents.obtain();
                    event.timeStamp = timeStamp;
                    event.keyChar = character;
                    event.keyCode = KeyCode.unknown;
                    event.type = KeyEvent.KEY_TYPED;
                    keyEvents.add(event);
            }
            Core.graphics.requestRendering();
        }

        // circle button on Xperia Play shouldn't need catchBack == true
        if(keyCode == 255) return true;
        return caughtKeys.contains(AndroidInputMap.getKeyCode(keyCode).ordinal());
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible){

        handle.post(() -> {
            InputMethodManager manager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(visible){
                View view = ((AndroidGraphics)Core.graphics).getView();
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                manager.showSoftInput(((AndroidGraphics)Core.graphics).getView(), 0);
            }else{
                manager.hideSoftInputFromWindow(((AndroidGraphics)Core.graphics).getView().getWindowToken(), 0);
            }
        });
    }

    @Override
    public void vibrate(int milliseconds){
        vibrator.vibrate(milliseconds);
    }

    @Override
    public void vibrate(long[] pattern, int repeat){
        vibrator.vibrate(pattern, repeat);
    }

    @Override
    public void cancelVibrate(){
        vibrator.cancel();
    }

    @Override
    public boolean justTouched(){
        return justTouched;
    }

    private void updateOrientation(){
        if(rotationVectorAvailable){
            SensorManager.getRotationMatrixFromVector(R, rotationVectorValues);
        }else if(!SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues)){
            return; // compass + accelerometer in free fall
        }
        SensorManager.getOrientation(R, orientation);
        azimuth = (float)Math.toDegrees(orientation[0]);
        pitch = (float)Math.toDegrees(orientation[1]);
        roll = (float)Math.toDegrees(orientation[2]);
    }

    /**
     * Returns the rotation matrix describing the devices rotation as per <a href=
     * "http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])"
     * >SensorManager#getRotationMatrix(float[], float[], float[], float[])</a>. Does not manipulate the matrix if the platform
     * does not have an accelerometer and compass, or a rotation vector sensor.
     */
    @Override
    public void getRotationMatrix(float[] matrix){
        if(rotationVectorAvailable)
            SensorManager.getRotationMatrixFromVector(matrix, rotationVectorValues);
        else // compass + accelerometer
            SensorManager.getRotationMatrix(matrix, null, accelerometerValues, magneticFieldValues);
    }

    void registerSensorListeners(){
        if(config.useAccelerometer){
            manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            if(manager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty()){
                accelerometerAvailable = false;
            }else{
                Sensor accelerometer = manager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
                accelerometerListener = new SensorListener();
                accelerometerAvailable = manager.registerListener(accelerometerListener, accelerometer,
                config.sensorDelay);
            }
        }else
            accelerometerAvailable = false;

        if(config.useGyroscope){
            manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            if(manager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty()){
                gyroscopeAvailable = false;
            }else{
                Sensor gyroscope = manager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
                gyroscopeListener = new SensorListener();
                gyroscopeAvailable = manager.registerListener(gyroscopeListener, gyroscope,
                config.sensorDelay);
            }
        }else
            gyroscopeAvailable = false;

        rotationVectorAvailable = false;
        if(config.useRotationVectorSensor){
            if(manager == null) manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> rotationVectorSensors = manager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
            if(!rotationVectorSensors.isEmpty()){
                rotationVectorListener = new SensorListener();
                for(Sensor sensor : rotationVectorSensors){ // favor AOSP sensor
                    if(sensor.getVendor().equals("Google Inc.") && sensor.getVersion() == 3){
                        rotationVectorAvailable = manager.registerListener(rotationVectorListener, sensor,
                        config.sensorDelay);
                        break;
                    }
                }
                if(!rotationVectorAvailable)
                    rotationVectorAvailable = manager.registerListener(rotationVectorListener, rotationVectorSensors.get(0),
                    config.sensorDelay);
            }
        }

        if(config.useCompass && !rotationVectorAvailable){
            if(manager == null) manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if(sensor != null){
                compassAvailable = accelerometerAvailable;
                if(compassAvailable){
                    compassListener = new SensorListener();
                    compassAvailable = manager.registerListener(compassListener, sensor, config.sensorDelay);
                }
            }else{
                compassAvailable = false;
            }
        }else
            compassAvailable = false;
    }

    void unregisterSensorListeners(){
        if(manager != null){
            if(accelerometerListener != null){
                manager.unregisterListener(accelerometerListener);
                accelerometerListener = null;
            }
            if(gyroscopeListener != null){
                manager.unregisterListener(gyroscopeListener);
                gyroscopeListener = null;
            }
            if(rotationVectorListener != null){
                manager.unregisterListener(rotationVectorListener);
                rotationVectorListener = null;
            }
            if(compassListener != null){
                manager.unregisterListener(compassListener);
                compassListener = null;
            }
            manager = null;
        }
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral){
        if(peripheral == Peripheral.accelerometer) return accelerometerAvailable;
        if(peripheral == Peripheral.gyroscope) return gyroscopeAvailable;
        if(peripheral == Peripheral.compass) return compassAvailable;
        if(peripheral == Peripheral.hardwareKeyboard) return keyboardAvailable;
        if(peripheral == Peripheral.onscreenKeyboard) return true;
        if(peripheral == Peripheral.vibrator) return vibrator != null && vibrator.hasVibrator();
        if(peripheral == Peripheral.multitouchScreen) return hasMultitouch;
        if(peripheral == Peripheral.rotationVector) return rotationVectorAvailable;
        return peripheral == Peripheral.pressure;
    }

    public int getFreePointerIndex(){
        int len = realId.length;
        for(int i = 0; i < len; i++){
            if(realId[i] == -1) return i;
        }

        realId = resize(realId);
        touchX = resize(touchX);
        touchY = resize(touchY);
        deltaX = resize(deltaX);
        deltaY = resize(deltaY);
        touched = resize(touched);
        button = resize(button);

        return len;
    }

    private int[] resize(int[] orig){
        int[] tmp = new int[orig.length + 2];
        System.arraycopy(orig, 0, tmp, 0, orig.length);
        return tmp;
    }

    private boolean[] resize(boolean[] orig){
        boolean[] tmp = new boolean[orig.length + 2];
        System.arraycopy(orig, 0, tmp, 0, orig.length);
        return tmp;
    }

    public int lookUpPointerIndex(int pointerId){
        int len = realId.length;
        for(int i = 0; i < len; i++){
            if(realId[i] == pointerId) return i;
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; i++){
            sb.append(i).append(":").append(realId[i]).append(" ");
        }
        Log.err("AndroidInput: Pointer ID lookup failed: " + pointerId + ", " + sb.toString());
        return -1;
    }

    @Override
    public int getRotation(){
        int orientation;

        if(context instanceof Activity){
            orientation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        }else{
            orientation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        }

        switch(orientation){
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: return 0;
        }
    }

    @Override
    public Orientation getNativeOrientation(){
        return nativeOrientation;
    }

    @Override
    public int deltaX(){
        return deltaX[0];
    }

    @Override
    public int deltaX(int pointer){
        return deltaX[pointer];
    }

    @Override
    public int deltaY(){
        return deltaY[0];
    }

    @Override
    public int deltaY(int pointer){
        return deltaY[pointer];
    }

    @Override
    public long getCurrentEventTime(){
        return currentEventTimeStamp;
    }

    public void addKeyListener(OnKeyListener listener){
        keyListeners.add(listener);
    }

    public void onPause(){
        unregisterSensorListeners();

        // erase pointer ids. this sucks donkeyballs...
        Arrays.fill(realId, -1);

        // erase touched state. this also sucks donkeyballs...
        Arrays.fill(touched, false);
    }

    public void onResume(){
        registerSensorListeners();
    }

    static class KeyEvent{
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;

        long timeStamp;
        int type;
        KeyCode keyCode;
        char keyChar;
    }

    static class TouchEvent{
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_SCROLLED = 3;
        static final int TOUCH_MOVED = 4;

        long timeStamp;
        int type;
        int x;
        int y;
        int scrollAmountX;
        int scrollAmountY;
        KeyCode button;
        int pointer;
    }

    /**
     * Our implementation of SensorEventListener. Because Android doesn't like it when we register more than one Sensor to a single
     * SensorEventListener, we add one of these for each Sensor. Could use an anonymous class, but I don't see any harm in
     * explicitly defining it here. Correct me if I am wrong.
     */
    private class SensorListener implements SensorEventListener{

        public SensorListener(){

        }

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1){

        }

        @Override
        public void onSensorChanged(SensorEvent event){
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                if(nativeOrientation == Orientation.portrait){
                    System.arraycopy(event.values, 0, accelerometerValues, 0, accelerometerValues.length);
                }else{
                    accelerometerValues[0] = event.values[1];
                    accelerometerValues[1] = -event.values[0];
                    accelerometerValues[2] = event.values[2];
                }
            }
            if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                System.arraycopy(event.values, 0, magneticFieldValues, 0, magneticFieldValues.length);
            }
            if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                if(nativeOrientation == Orientation.portrait){
                    System.arraycopy(event.values, 0, gyroscopeValues, 0, gyroscopeValues.length);
                }else{
                    gyroscopeValues[0] = event.values[1];
                    gyroscopeValues[1] = -event.values[0];
                    gyroscopeValues[2] = event.values[2];
                }
            }
            if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                if(nativeOrientation == Orientation.portrait){
                    System.arraycopy(event.values, 0, rotationVectorValues, 0, rotationVectorValues.length);
                }else{
                    rotationVectorValues[0] = event.values[1];
                    rotationVectorValues[1] = -event.values[0];
                    rotationVectorValues[2] = event.values[2];
                }
            }
        }
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event){
        return handleGenericMotion(event);
    }

    public boolean handleGenericMotion(MotionEvent event){
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int x, y, scrollAmountX, scrollAmountY;
        int pointer = 0;

        long timeStamp = System.nanoTime();
        synchronized(this){
            switch(action){
                case MotionEvent.ACTION_HOVER_MOVE:
                    x = (int)event.getX();
                    y = (int)event.getY();
                    if((x != mouseLastX) || (y != mouseLastY)){ // Avoid garbage events
                        postTouchEvent(TouchEvent.TOUCH_MOVED, x, y, 0, 0, timeStamp);

                        touchX[pointer] = x;
                        touchY[pointer] = Core.graphics.getHeight() - 1 - y;
                        deltaX[pointer] = x - mouseLastX;
                        deltaY[pointer] = -(y - mouseLastY);

                        mouseLastX = x;
                        mouseLastY = y;
                    }
                    break;

                case MotionEvent.ACTION_SCROLL:
                    scrollAmountY = (int)-Math.signum(event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                    scrollAmountX = (int)-Math.signum(event.getAxisValue(MotionEvent.AXIS_HSCROLL));
                    postTouchEvent(TouchEvent.TOUCH_SCROLLED, 0, 0, scrollAmountX, scrollAmountY, timeStamp);

            }
        }
        Core.graphics.requestRendering();
        return true;
    }

    private void postTouchEvent(int type, int x, int y, int scrollAmountX, int scrollAmountY, long timeStamp){
        TouchEvent event = usedTouchEvents.obtain();
        event.timeStamp = timeStamp;
        event.x = x;
        event.y = Core.graphics.getHeight() - y - 1;
        event.type = type;
        event.scrollAmountX = scrollAmountX;
        event.scrollAmountY = scrollAmountY;
        touchEvents.add(event);
    }
}
