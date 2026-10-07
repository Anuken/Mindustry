package arc.input;

import arc.struct.IntSeq;
import arc.util.Time;

/**
 * Queues events that are later passed to the wrapped {@link InputProcessor}.
 * @author Nathan Sweet
 */
public class InputEventQueue implements InputProcessor{
    private static final int SKIP = -1;
    private static final int KEY_DOWN = 0;
    private static final int KEY_UP = 1;
    private static final int KEY_TYPED = 2;
    private static final int TOUCH_DOWN = 3;
    private static final int TOUCH_UP = 4;
    private static final int TOUCH_DRAGGED = 5;
    private static final int MOUSE_MOVED = 6;
    private static final int SCROLLED = 7;
    private final IntSeq queue = new IntSeq();
    private final IntSeq processingQueue = new IntSeq();
    private InputProcessor processor;
    private long currentEventTime;

    public InputEventQueue(){
    }

    public InputEventQueue(InputProcessor processor){
        this.processor = processor;
    }

    public InputProcessor getProcessor(){
        return processor;
    }

    public void setProcessor(InputProcessor processor){
        this.processor = processor;
    }

    public void drain(){
        synchronized(this){
            if(processor == null){
                queue.clear();
                return;
            }
            processingQueue.addAll(queue);
            queue.clear();
        }
        int[] q = processingQueue.items;
        InputProcessor localProcessor = processor;
        for(int i = 0, n = processingQueue.size; i < n; ){
            int type = q[i++];
            currentEventTime = (long)q[i++] << 32 | q[i++] & 0xFFFFFFFFL;
            switch(type){
                case SKIP:
                    i += q[i];
                    break;
                case KEY_DOWN:
                    localProcessor.keyDown(KeyCode.byOrdinal(q[i++]));
                    break;
                case KEY_UP:
                    localProcessor.keyUp(KeyCode.byOrdinal(q[i++]));
                    break;
                case KEY_TYPED:
                    localProcessor.keyTyped((char)q[i++]);
                    break;
                case TOUCH_DOWN:
                    localProcessor.touchDown(q[i++], q[i++], q[i++], KeyCode.byOrdinal(q[i++]));
                    break;
                case TOUCH_UP:
                    localProcessor.touchUp(q[i++], q[i++], q[i++], KeyCode.byOrdinal(q[i++]));
                    break;
                case TOUCH_DRAGGED:
                    localProcessor.touchDragged(q[i++], q[i++], q[i++]);
                    break;
                case MOUSE_MOVED:
                    localProcessor.mouseMoved(q[i++], q[i++]);
                    break;
                case SCROLLED:
                    localProcessor.scrolled(q[i++] / 256f, q[i++] / 256f);
                    break;
                default:
                    throw new RuntimeException();
            }
        }
        processingQueue.clear();
    }

    private synchronized int next(int nextType, int i){
        int[] q = queue.items;
        for(int n = queue.size; i < n; ){
            int type = q[i];
            if(type == nextType) return i;
            i += 3;
            switch(type){
                case SKIP:
                    i += q[i];
                    break;
                case KEY_DOWN:
                    i++;
                    break;
                case KEY_UP:
                    i++;
                    break;
                case KEY_TYPED:
                    i++;
                    break;
                case TOUCH_DOWN:
                    i += 4;
                    break;
                case TOUCH_UP:
                    i += 4;
                    break;
                case TOUCH_DRAGGED:
                    i += 3;
                    break;
                case MOUSE_MOVED:
                    i += 2;
                    break;
                case SCROLLED:
                    i += 2;
                    break;
                default:
                    throw new RuntimeException();
            }
        }
        return -1;
    }

    private void queueTime(){
        long time = Time.nanos();
        queue.add((int)(time >> 32));
        queue.add((int)time);
    }

    @Override
    public synchronized boolean keyDown(KeyCode key){
        queue.add(KEY_DOWN);
        queueTime();
        queue.add(key.ordinal());
        return false;
    }

    @Override
    public synchronized boolean keyUp(KeyCode key){
        queue.add(KEY_UP);
        queueTime();
        queue.add(key.ordinal());
        return false;
    }

    @Override
    public synchronized boolean keyTyped(char character){
        queue.add(KEY_TYPED);
        queueTime();
        queue.add(character);
        return false;
    }

    @Override
    public synchronized boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        queue.add(TOUCH_DOWN);
        queueTime();
        queue.add(screenX);
        queue.add(screenY);
        queue.add(pointer);
        queue.add(button.ordinal());
        return false;
    }

    @Override
    public synchronized boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        queue.add(TOUCH_UP);
        queueTime();
        queue.add(screenX);
        queue.add(screenY);
        queue.add(pointer);
        queue.add(button.ordinal());
        return false;
    }

    @Override
    public synchronized boolean touchDragged(int screenX, int screenY, int pointer){
        // Skip any queued touch dragged events for the same pointer.
        for(int i = next(TOUCH_DRAGGED, 0); i >= 0; i = next(TOUCH_DRAGGED, i + 6)){
            if(queue.get(i + 5) == pointer){
                queue.set(i, SKIP);
                queue.set(i + 3, 3);
            }
        }
        queue.add(TOUCH_DRAGGED);
        queueTime();
        queue.add(screenX);
        queue.add(screenY);
        queue.add(pointer);
        return false;
    }

    @Override
    public synchronized boolean mouseMoved(int screenX, int screenY){
        // Skip any queued mouse moved events.
        for(int i = next(MOUSE_MOVED, 0); i >= 0; i = next(MOUSE_MOVED, i + 5)){
            queue.set(i, SKIP);
            queue.set(i + 3, 2);
        }
        queue.add(MOUSE_MOVED);
        queueTime();
        queue.add(screenX);
        queue.add(screenY);
        return false;
    }

    @Override
    public synchronized boolean scrolled(float amountX, float amountY){
        queue.add(SCROLLED);
        queueTime();
        queue.add((int)(amountX * 256));
        queue.add((int)(amountY * 256));
        return false;
    }

    public long getCurrentEventTime(){
        return currentEventTime;
    }
}
