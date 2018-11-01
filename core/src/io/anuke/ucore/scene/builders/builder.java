package io.anuke.ucore.scene.builders;

import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;

public abstract class builder<T extends builder, N extends Element>{
	public Cell<N> cell;
	public N element;
	
	protected Table context(){
		return build.getTable();
	}
	
	public T margin(float margin){
		if(element instanceof Table){
			((Table)element).margin(margin);
		}
		return (T)this;
	}

	public T marginTop(float margin){
		if(element instanceof Table){
			((Table)element).marginTop(margin);
		}
		return (T)this;
	}

	public T marginBottom(float margin){
		if(element instanceof Table){
			((Table)element).marginBottom(margin);
		}
		return (T)this;
	}

	public T marginLeft(float margin){
		if(element instanceof Table){
			((Table)element).marginLeft(margin);
		}
		return (T)this;
	}

	public T marginRight(float margin){
		if(element instanceof Table){
			((Table)element).marginRight(margin);
		}
		return (T)this;
	}
	
	public T visible(BooleanProvider vis){
		element.setVisible(vis);
		return (T)this;
	}
	
	public T touchable(Touchable t){
		element.setTouchable(t);
		return (T)this;
	}

	public T touchable(Supplier<Touchable> t){
		element.setTouchable(t);
		return (T)this;
	}
	
	public T update(Consumer<N> l){
		element.update(()->{
			l.accept(element);
		});
		return (T)this;
	}
	
	/**Returns the element being built.*/
	public N get(){
		return element;
	}
	
	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
	public T size (float size) {
		cell.size((size));
		return (T)this;
	}

	/** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
	public T size (float width, float height) {
		cell.size((width), (height));
		return (T)this;
	}

	/** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
	public T width (float width) {
		cell.width((width));
		return (T)this;
	}

	/** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
	public T height (float height) {
		cell.height((height));
		return (T)this;
	}

	/** Sets the minWidth and minHeight to the specified value. */
	public T minSize (float size) {
		cell.minSize((size));
		return (T)this;
	}

	/** Sets the minWidth and minHeight to the specified values. */
	public T minSize (float width, float height) {
		cell.minSize((width), (height));
		return (T)this;
	}

	public T minWidth (float minWidth) {
		cell.minWidth(minWidth);
		return (T)this;
	}

	public T minHeight (float minHeight) {
		cell.minHeight(minHeight);
		return (T)this;
	}

	/** Sets the prefWidth and prefHeight to the specified value. */
	public T prefSize (float width, float height) {
		cell.prefSize((width), (height));
		return (T)this;
	}

	/** Sets the prefWidth and prefHeight to the specified values. */
	public T prefSize (float size) {
		cell.prefSize((size));
		return (T)this;
	}

	/** Sets the maxWidth and maxHeight to the specified value. */
	public T maxSize (float size) {
		cell.maxSize((size));
		return (T)this;
	}

	/** Sets the maxWidth and maxHeight to the specified values. */
	public T maxSize (float width, float height) {
		cell.maxSize((width), (height));
		return (T)this;
	}

	public T maxWidth (float maxWidth) {
		cell.maxWidth(maxWidth);
		return (T)this;
	}

	public T maxHeight (float maxHeight) {
		cell.maxHeight(maxHeight);
		return (T)this;
	}

	/** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
	public T space (float space) {
		cell.space((space));
		return (T)this;
	}

	public T spaceTop (float spaceTop) {
		cell.spaceTop(spaceTop);
		return (T)this;
	}

	public T spaceLeft (float spaceLeft) {
		cell.spaceLeft(spaceLeft);
		return (T)this;
	}

	public T spaceBottom (float spaceBottom) {
		cell.spaceBottom(spaceBottom);
		return (T)this;
	}

	public T spaceRight (float spaceRight) {
		cell.spaceRight(spaceRight);
		return (T)this;
	}

	/** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
	public T pad (float pad) {
		cell.pad((pad));
		return (T)this;
	}

	public T pad (float top, float left, float bottom, float right) {
		cell.pad((top), (left), (bottom), (right));
		return (T)this;
	}

	public T padTop (float padTop) {
		cell.padTop(padTop);
		return (T)this;
	}

	public T padLeft (float padLeft) {
		cell.padLeft(padLeft);
		return (T)this;
	}

	public T padBottom (float padBottom) {
		cell.padBottom(padBottom);
		return (T)this;
	}

	public T padRight (float padRight) {
		cell.padRight(padRight);
		return (T)this;
	}

	/** Sets fillX and fillY to 1. */
	public T fill () {
		cell.fill();
		return (T)this;
	}

	/** Sets fillX to 1. */
	public T fillX () {
		cell.fillX();
		return (T)this;
	}

	/** Sets fillY to 1. */
	public T fillY () {
		cell.fillY();
		return (T)this;
	}

	public T fill (float x, float y) {
		cell.fill(x, y);
		return (T)this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public T fill (boolean x, boolean y) {
		cell.fill(x, y);
		return (T)this;
	}

	/** Sets fillX and fillY to 1 if true, 0 if false. */
	public T fill (boolean fill) {
		cell.fill(fill);
		return (T)this;
	}

	/** Sets the alignment of the actor within the cell. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom},
	 * {@link Align#left}, {@link Align#right}, or any combination of those. */
	public T align (int align) {
		cell.align(align);
		return (T)this;
	}

	/** Sets the alignment of the actor within the cell to {@link Align#center}. This clears any other alignment. */
	public T center () {
		cell.center();
		return (T)this;
	}

	/** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the cell. */
	public T top () {
		cell.top();
		return (T)this;
	}

	/** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the cell. */
	public T left () {
		cell.left();
		return (T)this;
	}

	/** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the cell. */
	public T bottom () {
		cell.bottom();
		return (T)this;
	}

	/** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the cell. */
	public T right () {
		cell.right();
		return (T)this;
	}

	/** Sets expandX, expandY, fillX, and fillY to 1. */
	public T grow () {
		cell.grow();
		return (T)this;
	}

	/** Sets expandX and fillX to 1. */
	public T growX () {
		cell.growX();
		return (T)this;
	}

	/** Sets expandY and fillY to 1. */
	public T growY () {
		cell.growY();
		return (T)this;
	}

	/** Sets expandX and expandY to 1. */
	public T expand () {
		cell.expand();
		return (T)this;
	}

	/** Sets expandX to 1. */
	public T expandX () {
		cell.expandX();
		return (T)this;
	}

	/** Sets expandY to 1. */
	public T expandY () {
		cell.expandY();
		return (T)this;
	}

	/** Sets expandX and expandY to 1 if true, 0 if false. */
	public T expand (boolean x, boolean y) {
		cell.expand(x, y);
		return (T)this;
	}

	public T colspan (int colspan) {
		cell.colspan(colspan);
		return (T)this;
	}

	/** Sets uniformX and uniformY to true. */
	public T uniform () {
		cell.uniform();
		return (T)this;
	}

	/** Sets uniformX to true. */
	public T uniformX () {
		cell.uniformX();
		return (T)this;
	}

	/** Sets uniformY to true. */
	public T uniformY () {
		cell.uniformY();
		return (T)this;
	}

	public T uniform (boolean x, boolean y) {
		cell.uniform(x, y);
		return (T)this;
	}
}
