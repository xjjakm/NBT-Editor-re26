package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawable;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.*;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public abstract class Panel<T extends Renderable & GuiEventListener> implements MVDrawable, MVElement, NarratableEntry {
	
	public static record PositionedPanelElement<T extends Renderable & GuiEventListener>(T element, int x, int y) {
	}
	
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	protected int renderPadding; // An area around the panel which elements can draw in, but events aren't passed - useful for borders
	protected boolean scrollable;
	protected int scroll;
	protected final ScrollBarWidget scrollBar;
	
	protected Panel(int x, int y, int width, int height, int renderPadding, boolean scrollable) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.renderPadding = renderPadding;
		
		this.scrollable = scrollable;
		this.scroll = 0;
		this.scrollBar = new ScrollBarWidget(x + width + renderPadding - 8, y, height,
				() -> scroll, scroll -> this.scroll = scroll, this::getMaxScroll);
	}
	private int getPaddedX() {
		return x - renderPadding;
	}
	private int getPaddedY() {
		return y - renderPadding;
	}
	private int getPaddedWidth() {
		return width + renderPadding * 2;
	}
	private int getPaddedHeight() {
		return height + renderPadding * 2;
	}
	
	@Override
	public void extractRenderState(Matrix3x2fStack matrices, int mouseX, int mouseY, float delta) {
		updateMousePos(mouseX, mouseY);
		
		checkOverScroll();
		
		MVDrawableHelper.enableScissor(matrices, getPaddedX(), getPaddedY(), getPaddedWidth(), getPaddedHeight());
		
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			T element = pos.element();
			
			matrices.pushMatrix();
			matrices.translate(pos.x() + x, pos.y() + y + scroll);
			element.extractRenderState(MVDrawableHelper.getDrawContext(matrices), mouseX - pos.x() - x, mouseY - pos.y() - y - scroll, delta);
			matrices.popMatrix();
		}
		
		MVDrawableHelper.disableScissor(matrices);
		
		scrollBar.extractRenderState(matrices, mouseX, mouseY, delta);
	}
	
	private void checkOverScroll() {
		int maxScroll = getMaxScroll();
		if (scroll < maxScroll)
			scroll = maxScroll;
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
	
	protected abstract Iterable<PositionedPanelElement<T>> getPanelElements();
	protected final List<PositionedPanelElement<T>> getPanelElementsSafe() {
		List<PositionedPanelElement<T>> output = new ArrayList<>();
		getPanelElements().forEach(output::add);
		return output;
	}
	/** The child element that currently has focus. All key/char/preedit events
	 *  and drag events (when dragging==true && button==1) are routed only to this child.
	 *  Mirrors ContainerEventHandler.getFocused() semantics. */
	protected T focusedChild;
	/** Whether the user is currently dragging (button 1 / left button).
	 *  Mirrors ContainerEventHandler.isDragging() / setDragging(). */
	protected boolean dragging;
	
	protected boolean continueEvents() {
		return false;
	}
	protected void updateMousePos(double mouseX, double mouseY) {}
	
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		updateMousePos(click.x(), click.y());
		
		if (scrollBar.mouseClicked(click,doubled)) {
			focusedChild = null;
			dragging = false;
			return true;
		}
		
		focusedChild = null;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseClicked(new MouseButtonEvent(click.x() - pos.x() - x, click.y() - pos.y() - y - scroll, new MouseButtonInfo(click.button(),0)),doubled)) {
				focusedChild = pos.element();
				// Mirror ContainerEventHandler: set dragging on button 1
				if (click.button() == 1)
					dragging = true;
				if (!continueEvents())
					break;
			}
		}
		return focusedChild != null;
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		updateMousePos(click.x(), click.y());
		
		// Mirror ContainerEventHandler: only handle button 1 release during a drag
		if (click.button() == 1 && dragging) {
			dragging = false;
			if (focusedChild != null) {
				for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
					if (pos.element() == focusedChild) {
						return pos.element().mouseReleased(new MouseButtonEvent(click.x() - pos.x() - x, click.y() - pos.y() - y - scroll, click.buttonInfo()));
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		updateMousePos(mouseX, mouseY);
		
		for (PositionedPanelElement<T> pos : getPanelElementsSafe())
			pos.element().mouseMoved(mouseX - pos.x() - x, mouseY - pos.y() - y - scroll);
	}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		updateMousePos(click.x(), click.y());
		
		if (scrollBar.mouseDragged(click, deltaX, deltaY))
			return true;
		
		// Mirror ContainerEventHandler: only route to focusedChild when dragging button 1
		if (focusedChild == null || !dragging || click.button() != 1)
			return false;
		
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element() == focusedChild) {
				return pos.element().mouseDragged(new MouseButtonEvent(click.x() - pos.x() - x, click.y() - pos.y() - y - scroll, click.buttonInfo()), deltaX, deltaY);
			}
		}
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		updateMousePos(mouseX, mouseY);
		
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseScrolled(mouseX - pos.x() - x, mouseY - pos.y() - y, xAmount, yAmount)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		if (!success && scrollable)
			success = scrollBar.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
		return success;
	}
	public int getMaxScroll() {
		return Math.min(0, height - getHighestY());
	}
	protected int getHighestY() {
		return StreamSupport.stream(getPanelElements().spliterator(), false)
				.mapToInt(pos -> pos.y() + getPanelElementHeight(pos.element())).max().orElse(0);
	}
	protected int getPanelElementHeight(T element) {
		return 0;
	}
	
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		if (focusedChild == null)
			return false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element() == focusedChild)
				return pos.element().keyPressed(keyInput);
		}
		return false;
	}
	@Override
	public boolean keyReleased(KeyEvent keyInput) {
		if (focusedChild == null)
			return false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element() == focusedChild)
				return pos.element().keyReleased(keyInput);
		}
		return false;
	}
	@Override
	public boolean charTyped(CharacterEvent charInput) {
		if (focusedChild == null)
			return false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element() == focusedChild)
				return pos.element().charTyped(charInput);
		}
		return false;
	}
	@Override
	public boolean preeditUpdated(@Nullable PreeditEvent event) {
		if (focusedChild == null)
			return false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element() == focusedChild)
				return pos.element().preeditUpdated(event);
		}
		return false;
	}
	
}
