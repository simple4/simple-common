package net.simpleframework.lib.org.jsoup.select;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.simpleframework.lib.org.jsoup.helper.StringUtil;
import net.simpleframework.lib.org.jsoup.helper.Validate;
import net.simpleframework.lib.org.jsoup.parser.TokenQueue;

/**
 * Parses a CSS selector into an Evaluator tree.
 */
class QueryParser {
	private final static String[] combinators = { ",", ">", "+", "~", " " };

	private final TokenQueue tq;
	private final String query;
	private final List<Evaluator> evals = new ArrayList<Evaluator>();

	/**
	 * Create a new QueryParser.
	 * 
	 * @param query
	 *           CSS query
	 */
	private QueryParser(final String query) {
		this.query = query;
		this.tq = new TokenQueue(query);
	}

	/**
	 * Parse a CSS query into an Evaluator.
	 * 
	 * @param query
	 *           CSS query
	 * @return Evaluator
	 */
	public static Evaluator parse(final String query) {
		final QueryParser p = new QueryParser(query);
		return p.parse();
	}

	/**
	 * Parse the query
	 * 
	 * @return Evaluator
	 */
	Evaluator parse() {
		tq.consumeWhitespace();

		if (tq.matchesAny(combinators)) { // if starts with a combinator, use root
														// as elements
			evals.add(new StructuralEvaluator.Root());
			combinator(tq.consume());
		} else {
			findElements();
		}

		while (!tq.isEmpty()) {
			// hierarchy and extras
			final boolean seenWhite = tq.consumeWhitespace();

			if (tq.matchesAny(combinators)) {
				combinator(tq.consume());
			} else if (seenWhite) {
				combinator(' ');
			} else { // E.class, E#id, E[attr] etc. AND
				findElements(); // take next el, #. etc off queue
			}
		}

		if (evals.size() == 1) {
			return evals.get(0);
		}

		return new CombiningEvaluator.And(evals);
	}

	private void combinator(final char combinator) {
		tq.consumeWhitespace();
		final String subQuery = consumeSubQuery(); // support multi > childs

		Evaluator rootEval; // the new topmost evaluator
		Evaluator currentEval; // the evaluator the new eval will be combined to.
										// could be root, or rightmost or.
		final Evaluator newEval = parse(subQuery); // the evaluator to add into
																	// target
		// evaluator
		boolean replaceRightMost = false;

		if (evals.size() == 1) {
			rootEval = currentEval = evals.get(0);
			// make sure OR (,) has precedence:
			if (rootEval instanceof CombiningEvaluator.Or && combinator != ',') {
				currentEval = ((CombiningEvaluator.Or) currentEval).rightMostEvaluator();
				replaceRightMost = true;
			}
		} else {
			rootEval = currentEval = new CombiningEvaluator.And(evals);
		}
		evals.clear();

		// for most combinators: change the current eval into an AND of the
		// current eval and the new eval
		if (combinator == '>') {
			currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.ImmediateParent(
					currentEval));
		} else if (combinator == ' ') {
			currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.Parent(
					currentEval));
		} else if (combinator == '+') {
			currentEval = new CombiningEvaluator.And(newEval,
					new StructuralEvaluator.ImmediatePreviousSibling(currentEval));
		} else if (combinator == '~') {
			currentEval = new CombiningEvaluator.And(newEval, new StructuralEvaluator.PreviousSibling(
					currentEval));
		} else if (combinator == ',') { // group or.
			CombiningEvaluator.Or or;
			if (currentEval instanceof CombiningEvaluator.Or) {
				or = (CombiningEvaluator.Or) currentEval;
				or.add(newEval);
			} else {
				or = new CombiningEvaluator.Or();
				or.add(currentEval);
				or.add(newEval);
			}
			currentEval = or;
		} else {
			throw new Selector.SelectorParseException("Unknown combinator: " + combinator);
		}

		if (replaceRightMost) {
			((CombiningEvaluator.Or) rootEval).replaceRightMostEvaluator(currentEval);
		} else {
			rootEval = currentEval;
		}
		evals.add(rootEval);
	}

	private String consumeSubQuery() {
		final StringBuilder sq = new StringBuilder();
		while (!tq.isEmpty()) {
			if (tq.matches("(")) {
				sq.append("(").append(tq.chompBalanced('(', ')')).append(")");
			} else if (tq.matches("[")) {
				sq.append("[").append(tq.chompBalanced('[', ']')).append("]");
			} else if (tq.matchesAny(combinators)) {
				break;
			} else {
				sq.append(tq.consume());
			}
		}
		return sq.toString();
	}

	private void findElements() {
		if (tq.matchChomp("#")) {
			byId();
		} else if (tq.matchChomp(".")) {
			byClass();
		} else if (tq.matchesWord()) {
			byTag();
		} else if (tq.matches("[")) {
			byAttribute();
		} else if (tq.matchChomp("*")) {
			allElements();
		} else if (tq.matchChomp(":lt(")) {
			indexLessThan();
		} else if (tq.matchChomp(":gt(")) {
			indexGreaterThan();
		} else if (tq.matchChomp(":eq(")) {
			indexEquals();
		} else if (tq.matches(":has(")) {
			has();
		} else if (tq.matches(":contains(")) {
			contains(false);
		} else if (tq.matches(":containsOwn(")) {
			contains(true);
		} else if (tq.matches(":matches(")) {
			matches(false);
		} else if (tq.matches(":matchesOwn(")) {
			matches(true);
		} else if (tq.matches(":not(")) {
			not();
		} else {
			// unhandled
			throw new Selector.SelectorParseException(
					"Could not parse query '%s': unexpected token at '%s'", query, tq.remainder());
		}

	}

	private void byId() {
		final String id = tq.consumeCssIdentifier();
		Validate.notEmpty(id);
		evals.add(new Evaluator.Id(id));
	}

	private void byClass() {
		final String className = tq.consumeCssIdentifier();
		Validate.notEmpty(className);
		evals.add(new Evaluator.Class(className.trim().toLowerCase()));
	}

	private void byTag() {
		String tagName = tq.consumeElementSelector();
		Validate.notEmpty(tagName);

		// namespaces: if element name is "abc:def", selector must be "abc|def",
		// so flip:
		if (tagName.contains("|")) {
			tagName = tagName.replace("|", ":");
		}

		evals.add(new Evaluator.Tag(tagName.trim().toLowerCase()));
	}

	private void byAttribute() {
		final TokenQueue cq = new TokenQueue(tq.chompBalanced('[', ']')); // content
		// queue
		final String key = cq.consumeToAny("=", "!=", "^=", "$=", "*=", "~="); // eq,
		// not,
		// start,
		// end,
		// contain,
		// match,
		// (no
		// val)
		Validate.notEmpty(key);
		cq.consumeWhitespace();

		if (cq.isEmpty()) {
			if (key.startsWith("^")) {
				evals.add(new Evaluator.AttributeStarting(key.substring(1)));
			} else {
				evals.add(new Evaluator.Attribute(key));
			}
		} else {
			if (cq.matchChomp("=")) {
				evals.add(new Evaluator.AttributeWithValue(key, cq.remainder()));
			} else if (cq.matchChomp("!=")) {
				evals.add(new Evaluator.AttributeWithValueNot(key, cq.remainder()));
			} else if (cq.matchChomp("^=")) {
				evals.add(new Evaluator.AttributeWithValueStarting(key, cq.remainder()));
			} else if (cq.matchChomp("$=")) {
				evals.add(new Evaluator.AttributeWithValueEnding(key, cq.remainder()));
			} else if (cq.matchChomp("*=")) {
				evals.add(new Evaluator.AttributeWithValueContaining(key, cq.remainder()));
			} else if (cq.matchChomp("~=")) {
				evals.add(new Evaluator.AttributeWithValueMatching(key, Pattern.compile(cq.remainder())));
			} else {
				throw new Selector.SelectorParseException(
						"Could not parse attribute query '%s': unexpected token at '%s'", query,
						cq.remainder());
			}
		}
	}

	private void allElements() {
		evals.add(new Evaluator.AllElements());
	}

	// pseudo selectors :lt, :gt, :eq
	private void indexLessThan() {
		evals.add(new Evaluator.IndexLessThan(consumeIndex()));
	}

	private void indexGreaterThan() {
		evals.add(new Evaluator.IndexGreaterThan(consumeIndex()));
	}

	private void indexEquals() {
		evals.add(new Evaluator.IndexEquals(consumeIndex()));
	}

	private int consumeIndex() {
		final String indexS = tq.chompTo(")").trim();
		Validate.isTrue(StringUtil.isNumeric(indexS), "Index must be numeric");
		return Integer.parseInt(indexS);
	}

	// pseudo selector :has(el)
	private void has() {
		tq.consume(":has");
		final String subQuery = tq.chompBalanced('(', ')');
		Validate.notEmpty(subQuery, ":has(el) subselect must not be empty");
		evals.add(new StructuralEvaluator.Has(parse(subQuery)));
	}

	// pseudo selector :contains(text), containsOwn(text)
	private void contains(final boolean own) {
		tq.consume(own ? ":containsOwn" : ":contains");
		final String searchText = TokenQueue.unescape(tq.chompBalanced('(', ')'));
		Validate.notEmpty(searchText, ":contains(text) query must not be empty");
		if (own) {
			evals.add(new Evaluator.ContainsOwnText(searchText));
		} else {
			evals.add(new Evaluator.ContainsText(searchText));
		}
	}

	// :matches(regex), matchesOwn(regex)
	private void matches(final boolean own) {
		tq.consume(own ? ":matchesOwn" : ":matches");
		final String regex = tq.chompBalanced('(', ')'); // don't unescape, as
																			// regex
		// bits will be escaped
		Validate.notEmpty(regex, ":matches(regex) query must not be empty");

		if (own) {
			evals.add(new Evaluator.MatchesOwn(Pattern.compile(regex)));
		} else {
			evals.add(new Evaluator.Matches(Pattern.compile(regex)));
		}
	}

	// :not(selector)
	private void not() {
		tq.consume(":not");
		final String subQuery = tq.chompBalanced('(', ')');
		Validate.notEmpty(subQuery, ":not(selector) subselect must not be empty");

		evals.add(new StructuralEvaluator.Not(parse(subQuery)));
	}
}
