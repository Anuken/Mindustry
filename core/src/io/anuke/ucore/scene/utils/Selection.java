
package io.anuke.ucore.scene.utils;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.ChangeListener.ChangeEvent;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedSet;
import com.badlogic.gdx.utils.Pools;

import java.util.Iterator;

/** Manages selected objects. Optionally fires a {@link ChangeEvent} on an actor. Selection changes can be vetoed via
 * {@link ChangeEvent#cancel()}.
 * @author Nathan Sweet */
public class Selection<T> implements Disableable, Iterable<T> {
	private Element actor;
	final OrderedSet<T> selected = new OrderedSet();
	private final OrderedSet<T> old = new OrderedSet();
	boolean isDisabled;
	private boolean toggle;
	boolean multiple;
	boolean required;
	private boolean programmaticChangeEvents = true;
	T lastSelected;

	/** @param actor An actor to fire {@link ChangeEvent} on when the selection changes, or null. */
	public void setActor (Element actor) {
		this.actor = actor;
	}

	/** Selects or deselects the specified item based on how the selection is configured, whether ctrl is currently pressed, etc.
	 * This is typically invoked by user interaction. */
	public void choose (T item) {
		if (item == null) throw new IllegalArgumentException("item cannot be null.");
		if (isDisabled) return;
		snapshot();
		try {
			if ((toggle || (!required && selected.size == 1) || UIUtils.ctrl()) && selected.contains(item)) {
				if (required && selected.size == 1) return;
				selected.remove(item);
				lastSelected = null;
			} else {
				boolean modified = false;
				if (!multiple || (!toggle && !UIUtils.ctrl())) {
					if (selected.size == 1 && selected.contains(item)) return;
					modified = selected.size > 0;
					selected.clear();
				}
				if (!selected.add(item) && !modified) return;
				lastSelected = item;
			}
			if (fireChangeEvent())
				revert();
			else
				changed();
		} finally {
			cleanup();
		}
	}

	public boolean hasItems () {
		return selected.size > 0;
	}

	public boolean isEmpty () {
		return selected.size == 0;
	}

	public int size () {
		return selected.size;
	}

	public OrderedSet<T> items () {
		return selected;
	}

	/** Returns the first selected item, or null. */
	public T first () {
		return selected.size == 0 ? null : selected.first();
	}

	void snapshot () {
		old.clear();
		old.addAll(selected);
	}

	void revert () {
		selected.clear();
		selected.addAll(old);
	}

	void cleanup () {
		old.clear(32);
	}

	/** Sets the selection to only the specified item. */
	public void set (T item) {
		if (item == null) throw new IllegalArgumentException("item cannot be null.");
		if (selected.size == 1 && selected.first() == item) return;
		snapshot();
		selected.clear();
		selected.add(item);
		if (programmaticChangeEvents && fireChangeEvent())
			revert();
		else {
			lastSelected = item;
			changed();
		}
		cleanup();
	}

	public void setAll (Array<T> items) {
		boolean added = false;
		snapshot();
		lastSelected = null;
		selected.clear();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (item == null) throw new IllegalArgumentException("item cannot be null.");
			if (selected.add(item)) added = true;
		}
		if (added) {
			if (programmaticChangeEvents && fireChangeEvent())
				revert();
			else if (items.size > 0) {
				lastSelected = items.peek();
				changed();
			}
		}
		cleanup();
	}

	/** Adds the item to the selection. */
	public void add (T item) {
		if (item == null) throw new IllegalArgumentException("item cannot be null.");
		if (!selected.add(item)) return;
		if (programmaticChangeEvents && fireChangeEvent())
			selected.remove(item);
		else {
			lastSelected = item;
			changed();
		}
	}

	public void addAll (Array<T> items) {
		boolean added = false;
		snapshot();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (item == null) throw new IllegalArgumentException("item cannot be null.");
			if (selected.add(item)) added = true;
		}
		if (added) {
			if (programmaticChangeEvents && fireChangeEvent())
				revert();
			else {
				lastSelected = items.peek();
				changed();
			}
		}
		cleanup();
	}

	public void remove (T item) {
		if (item == null) throw new IllegalArgumentException("item cannot be null.");
		if (!selected.remove(item)) return;
		if (programmaticChangeEvents && fireChangeEvent())
			selected.add(item);
		else {
			lastSelected = null;
			changed();
		}
	}

	public void removeAll (Array<T> items) {
		boolean removed = false;
		snapshot();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (item == null) throw new IllegalArgumentException("item cannot be null.");
			if (selected.remove(item)) removed = true;
		}
		if (removed) {
			if (programmaticChangeEvents && fireChangeEvent())
				revert();
			else {
				lastSelected = null;
				changed();
			}
		}
		cleanup();
	}

	public void clear () {
		if (selected.size == 0) return;
		snapshot();
		selected.clear();
		if (programmaticChangeEvents && fireChangeEvent())
			revert();
		else {
			lastSelected = null;
			changed();
		}
		cleanup();
	}

	/** Called after the selection changes. The default implementation does nothing. */
	protected void changed () {
	}

	/** Fires a change event on the selection's actor, if any. Called internally when the selection changes, depending on
	 * {@link #setProgrammaticChangeEvents(boolean)}.
	 * @return true if the change should be undone. */
	public boolean fireChangeEvent () {
		if (actor == null) return false;
		ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
		try {
			return actor.fire(changeEvent);
		} finally {
			Pools.free(changeEvent);
		}
	}

	public boolean contains (T item) {
		if (item == null) return false;
		return selected.contains(item);
	}

	/** Makes a best effort to return the last item selected, else returns an arbitrary item or null if the selection is empty. */
	public T getLastSelected () {
		if (lastSelected != null) {
			return lastSelected;
		} else if (selected.size > 0) {
			return selected.first();
		}
		return null;
	}

	public Iterator<T> iterator () {
		return selected.iterator();
	}

	public Array<T> toArray () {
		return selected.iterator().toArray();
	}

	public Array<T> toArray (Array<T> array) {
		return selected.iterator().toArray(array);
	}

	/** If true, prevents {@link #choose(Object)} from changing the selection. Default is false. */
	public void setDisabled (boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public boolean isDisabled () {
		return isDisabled;
	}

	public boolean getToggle () {
		return toggle;
	}

	/** If true, prevents {@link #choose(Object)} from clearing the selection. Default is false. */
	public void setToggle (boolean toggle) {
		this.toggle = toggle;
	}

	public boolean getMultiple () {
		return multiple;
	}

	/** If true, allows {@link #choose(Object)} to select multiple items. Default is false. */
	public void setMultiple (boolean multiple) {
		this.multiple = multiple;
	}

	public boolean getRequired () {
		return required;
	}

	/** If true, prevents {@link #choose(Object)} from selecting none. Default is false. */
	public void setRequired (boolean required) {
		this.required = required;
	}

	/** If false, only {@link #choose(Object)} will fire a change event. Default is true. */
	public void setProgrammaticChangeEvents (boolean programmaticChangeEvents) {
		this.programmaticChangeEvents = programmaticChangeEvents;
	}

	public String toString () {
		return selected.toString();
	}
}
