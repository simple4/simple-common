package net.simpleframework.common.html.element;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class InputElement extends AbstractInputElement<InputElement> {

	public InputElement() {
	}

	public InputElement(final String id) {
		this(id, null);
	}

	public InputElement(final String id, final EInputType inputType) {
		setId(id);
		setName(id);
		setInputType(inputType);
	}
}
