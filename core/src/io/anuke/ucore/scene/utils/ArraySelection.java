
package io.anuke.ucore.scene.utils;

import java.util.Iterator;

import com.badlogic.gdx.utils.Array;

/** A selection that supports range selection by knowing about the array of items being selected.
 * @author Nathan Sweet */
public class ArraySelection<T> extends Selection<T> { 
	private Array<T> array;
	private boolean rangeSelect = true;
	private int rangeStart;

	public ArraySelection (Array<T> array) {
		this.array = array;
	}

	public void choose (T item) {
		if (item == null) throw new IllegalArgumentException("item cannot be null.");
		if (isDisabled) return;
		int index = array.indexOf(item, false);
		if (selected.size > 0 && rangeSelect && multiple && UIUtils.shift()) {
			int oldRangeState = rangeStart;
			snapshot();
			// Select new range.
			int start = rangeStart, end = index;
			if (start > end) {
				int temp = end;
				end = start;
				start = temp;
			}
			if (!UIUtils.ctrl()) selected.clear();
			for (int i = start; i <= end; i++)
				selected.add(array.get(i));
			if (fireChangeEvent()) {
				rangeStart = oldRangeState;
				revert();
			}
			cleanup();
			return;
		} else
			rangeStart = index;
		super.choose(item);
	}

	public boolean getRangeSelect () {
		return rangeSelect;
	}

	public void setRangeSelect (boolean rangeSelect) {
		this.rangeSelect = rangeSelect;
	}

	/** Removes objects from the selection that are no longer in the items array. If {@link #getRequired()} is true and there is no
	 * selected item, the first item is selected. */
	public void validate () {
		Array<T> array = this.array;
		if (array.size == 0) {
			clear();
			return;
		}
		for (Iterator<T> iter = items().iterator(); iter.hasNext();) {
			T selected = iter.next();
			if (!array.contains(selected, false)) iter.remove();
		}
		if (required && selected.size == 0) set(array.first());
	}
}
