package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection.MethodInvoker;
import net.minecraft.client.Minecraft;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Compatibility layer for IMBlocker mod (https://github.com/ reserveword/IMBlocker).
 * IMBlocker disables vanilla TextInputManager methods via mixin and manages Windows IME
 * through ImmAssociateContext directly. It also requires focus owners to implement
 * its MinecraftFocusableWidget interface — widgets that don't inherit from EditBox
 * (like our MultiLineTextFieldWidget) are completely invisible to IMBlocker's focus
 * system, so IME stays locked.
 * 
 * This class uses java.lang.reflect.Proxy to dynamically implement
 * MinecraftFocusableWidget and register/unregister focus with IMBlocker's
 * FocusContainer.MINECRAFT. All reflective lookups are lazy and tolerate
 * ClassNotFoundException when IMBlocker is not installed.
 */
public final class IMBlockerCompat {

	private IMBlockerCompat() {}

	private static final Supplier<Class<?>> FOCUS_CONTAINER_CLASS = lazyOptionalClass(
			"io.github.reserveword.imblocker.common.gui.FocusContainer");
	private static final Supplier<Class<?>> FOCUSABLE_WIDGET_CLASS = lazyOptionalClass(
			"io.github.reserveword.imblocker.common.gui.FocusableWidget");
	private static final Supplier<Class<?>> MINECRAFT_FOCUSABLE_WIDGET_CLASS = lazyOptionalClass(
			"io.github.reserveword.imblocker.common.gui.MinecraftFocusableWidget");
	private static final Supplier<Class<?>> RECTANGLE_CLASS = lazyOptionalClass(
			"io.github.reserveword.imblocker.common.gui.Rectangle");
	private static final Supplier<Class<?>> POINT_CLASS = lazyOptionalClass(
			"io.github.reserveword.imblocker.common.gui.Point");

	private static final Supplier<MethodInvoker> FOCUS_CONTAINER_REQUEST_FOCUS = lazyOptionalMethod(
			FOCUS_CONTAINER_CLASS, "requestFocus", MethodType.methodType(void.class, FOCUSABLE_WIDGET_CLASS.get()));
	private static final Supplier<MethodInvoker> FOCUS_CONTAINER_REMOVE_FOCUS = lazyOptionalMethod(
			FOCUS_CONTAINER_CLASS, "removeFocus", MethodType.methodType(void.class, FOCUSABLE_WIDGET_CLASS.get()));

	private static final Supplier<Object> MINECRAFT_CONTAINER = new Supplier<>() {
		private volatile Object value;
		private volatile boolean initialized;
		@Override
		public Object get() {
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						try {
							Field f = FOCUS_CONTAINER_CLASS.get().getField("MINECRAFT");
							value = f.get(null);
						} catch (Throwable ignored) {
							value = null;
						}
						initialized = true;
					}
				}
			}
			return value;
		}
	};

	/** True if IMBlocker classes are available in this environment. */
	public static boolean isPresent() {
		return MINECRAFT_CONTAINER.get() != null;
	}

	/** Returns IMBlocker's FocusContainer.MINECRAFT singleton (null if not installed). */
	public static Object getMinecraftContainer() {
		return MINECRAFT_CONTAINER.get();
	}

	/**
	 * Notify IMBlocker's focus manager that a non-EditBox widget has gained/lost
	 * focus. For MultiLineTextFieldWidget which bypasses Minecraft's widget hierarchy.
	 * When IMBlocker is absent this is a no-op and the caller should fall back to
	 * Minecraft.onTextInputFocusChange().
	 *
	 * @param widgetProxy an InvocationHandler that implements the widget side
	 *                    (getBoundsAbs, getCaretPos, getGuiScale, isRenderable, etc.)
	 * @param focused     true for focus gained, false for focus lost
	 * @return a cached proxy instance (same across calls for the same handler) that
	 *         must be passed back on subsequent calls, or null if IMBlocker absent
	 */
	public static Object notifyFocusChange(InvocationHandler widgetProxy, boolean focused, Object prevProxy) {
		Object container = MINECRAFT_CONTAINER.get();
		if (container == null)
			return null;
		try {
			Class<?> widgetIface = MINECRAFT_FOCUSABLE_WIDGET_CLASS.get();
			ClassLoader cl = IMBlockerCompat.class.getClassLoader();
			Object proxy = (prevProxy != null) ? prevProxy
					: Proxy.newProxyInstance(cl, new Class<?>[] { widgetIface }, widgetProxy);
			if (focused) {
				FOCUS_CONTAINER_REQUEST_FOCUS.get().invoke(container, proxy);
			} else {
				FOCUS_CONTAINER_REMOVE_FOCUS.get().invoke(container, proxy);
			}
			return proxy;
		} catch (Throwable t) {
			// Fall through so caller still does vanilla onTextInputFocusChange
			return null;
		}
	}

	// ---- Helpers for constructing Point / Rectangle arguments without compile dependency ----

	public static Object newPoint(int x, int y) {
		try {
			return RECTANGLE_CLASS == null ? null :
					POINT_CLASS.get().getConstructor(int.class, int.class).newInstance(x, y);
		} catch (Throwable ignored) {
			return null;
		}
	}
	public static Object newPoint(double guiScale, double x, double y) {
		try {
			if (POINT_CLASS.get() == null)
				return null;
			return POINT_CLASS.get().getConstructor(double.class, double.class, double.class).newInstance(guiScale, x, y);
		} catch (Throwable t) {
			return null;
		}
	}
	public static Object newRectangle(int x, int y, int w, int h) {
		try {
			return RECTANGLE_CLASS.get().getConstructor(int.class, int.class, int.class, int.class).newInstance(x, y, w, h);
		} catch (Throwable ignored) {
			return null;
		}
	}
	public static Object newRectangle(double guiScale, double x, double y, double w, double h) {
		try {
			return RECTANGLE_CLASS.get().getConstructor(double.class, double.class, double.class, double.class, double.class)
					.newInstance(guiScale, x, y, w, h);
		} catch (Throwable ignored) {
			return null;
		}
	}
	public static double getGuiScale() {
		return Minecraft.getInstance().getWindow().getGuiScale();
	}

	// ---- lazy suppliers that return null instead of throwing on miss ----

	private static Supplier<Class<?>> lazyOptionalClass(String fqn) {
		return new Supplier<>() {
			private volatile Class<?> value;
			private volatile boolean initialized;
			@Override
			public Class<?> get() {
				if (!initialized) {
					synchronized (this) {
						if (!initialized) {
							try {
								value = Class.forName(fqn);
							} catch (ClassNotFoundException ignored) {
								value = null;
							}
							initialized = true;
						}
					}
				}
				return value;
			}
		};
	}

	private static Supplier<MethodInvoker> lazyOptionalMethod(
			Supplier<Class<?>> clazz, String method, MethodType type) {
		return new Supplier<>() {
			private volatile MethodInvoker value;
			private volatile boolean initialized;
			@Override
			public MethodInvoker get() {
				if (!initialized) {
					synchronized (this) {
						if (!initialized) {
							try {
								Class<?> c = clazz.get();
								if (c == null) { value = null; }
								else { value = Reflection.getMethod(c, method, type); }
							} catch (Throwable ignored) {
								value = null;
							}
							initialized = true;
						}
					}
				}
				return value;
			}
		};
	}
}
