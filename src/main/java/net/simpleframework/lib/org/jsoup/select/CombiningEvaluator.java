package net.simpleframework.lib.org.jsoup.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.simpleframework.lib.org.jsoup.helper.StringUtil;
import net.simpleframework.lib.org.jsoup.nodes.Element;

/**
 * Base combining (and, or) evaluator.
 */
abstract class CombiningEvaluator extends Evaluator {
	final List<Evaluator> evaluators;

	CombiningEvaluator() {
		super();
		evaluators = new ArrayList<Evaluator>();
	}

	CombiningEvaluator(final Collection<Evaluator> evaluators) {
		this();
		this.evaluators.addAll(evaluators);
	}

	Evaluator rightMostEvaluator() {
		return evaluators.size() > 0 ? evaluators.get(evaluators.size() - 1) : null;
	}

	void replaceRightMostEvaluator(final Evaluator replacement) {
		evaluators.set(evaluators.size() - 1, replacement);
	}

	static final class And extends CombiningEvaluator {
		And(final Collection<Evaluator> evaluators) {
			super(evaluators);
		}

		And(final Evaluator... evaluators) {
			this(Arrays.asList(evaluators));
		}

		@Override
		public boolean matches(final Element root, final Element node) {
			for (int i = 0; i < evaluators.size(); i++) {
				final Evaluator s = evaluators.get(i);
				if (!s.matches(root, node)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			return StringUtil.join(evaluators, " ");
		}
	}

	static final class Or extends CombiningEvaluator {
		/**
		 * Create a new Or evaluator. The initial evaluators are ANDed together
		 * and used as the first clause of the OR.
		 * 
		 * @param evaluators
		 *           initial OR clause (these are wrapped into an AND evaluator).
		 */
		Or(final Collection<Evaluator> evaluators) {
			super();
			if (evaluators.size() > 1) {
				this.evaluators.add(new And(evaluators));
			} else {
				// 0 or 1
				this.evaluators.addAll(evaluators);
			}
		}

		Or() {
			super();
		}

		public void add(final Evaluator e) {
			evaluators.add(e);
		}

		@Override
		public boolean matches(final Element root, final Element node) {
			for (int i = 0; i < evaluators.size(); i++) {
				final Evaluator s = evaluators.get(i);
				if (s.matches(root, node)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return String.format(":or%s", evaluators);
		}
	}
}
