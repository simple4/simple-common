package net.simpleframework.common;

import static net.simpleframework.common.I18n.$m;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ThrowableUtils {
	public static Throwable getCause(final Class<? extends Throwable> clazz,
			final Throwable throwable) {
		Throwable cause = throwable;
		while ((cause = cause.getCause()) != null && clazz.isAssignableFrom(cause.getClass())) {
			break;
		}
		return cause == null ? throwable : cause;
	}

	public static Throwable convertThrowable(Throwable th) {
		if (th instanceof ServletException) {
			Throwable throwable = th;
			while (throwable != null && ServletException.class.isAssignableFrom(throwable.getClass())) {
				throwable = ((ServletException) throwable).getRootCause();
				if (throwable != null) {
					th = throwable;
				}
			}
		} else if (th instanceof UndeclaredThrowableException) {
			final Throwable throwable = ((UndeclaredThrowableException) th).getUndeclaredThrowable();
			if (throwable != null) {
				th = throwable;
			}
		} else if (th instanceof InvocationTargetException) {
			th = ((InvocationTargetException) th).getTargetException();
		}
		return th;
	}

	public static String getThrowableMessage(Throwable th) {
		th = convertThrowable(th);
		String message = throwableMessages.get(th.getClass());
		if (message != null) {
			return message;
		}
		while (th != null) {
			message = th.getMessage();
			if (!StringUtils.hasText(message)) {
				final Throwable th0 = th.getCause();
				if (th0 != null) {
					th = th0;
				} else {
					return (message = throwableMessages.get(th.getClass())) != null ? message
							: StringUtils.substring(Convert.toString(th), 80, true);
				}
			} else {
				return message;
			}
		}
		return null;
	}

	private static Map<Class<? extends Throwable>, String> throwableMessages = new HashMap<Class<? extends Throwable>, String>();

	static {
		registMessageHandle(new IThrowableMessageHandle() {
			@Override
			public Map<Class<? extends Throwable>, String> messages() {
				final HashMap<Class<? extends Throwable>, String> messages = new HashMap<Class<? extends Throwable>, String>();
				messages.put(NullPointerException.class, $m("SimpleRuntimeException.0"));
				return messages;
			}
		});
	}

	public static void registMessageHandle(final IThrowableMessageHandle handle) {
		Map<Class<? extends Throwable>, String> messages;
		if (handle != null && (messages = handle.messages()) != null) {
			throwableMessages.putAll(messages);
		}
	}

	public static interface IThrowableMessageHandle {

		/**
		 * 转换抛出错误的信息
		 * 
		 * @return
		 */
		Map<Class<? extends Throwable>, String> messages();
	}
}
