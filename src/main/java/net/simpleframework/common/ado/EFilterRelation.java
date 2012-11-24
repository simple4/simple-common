package net.simpleframework.common.ado;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EFilterRelation {
	equal {

		@Override
		public String toString() {
			return "=";
		}
	},

	not_equal {

		@Override
		public String toString() {
			return "<>";
		}
	},

	gt {

		@Override
		public String toString() {
			return ">";
		}
	},

	gt_equal {

		@Override
		public String toString() {
			return ">=";
		}
	},

	lt {

		@Override
		public String toString() {
			return "<";
		}
	},

	lt_equal {

		@Override
		public String toString() {
			return "<=";
		}
	},

	like;

	public static EFilterRelation get(final String key) {
		for (final EFilterRelation relation : EFilterRelation.values()) {
			if (relation.toString().equals(key)) {
				return relation;
			}
		}
		return EFilterRelation.equal;
	}
}
