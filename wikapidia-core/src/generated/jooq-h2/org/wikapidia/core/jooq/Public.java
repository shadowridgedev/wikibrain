/**
 * This class is generated by jOOQ
 */
package org.wikapidia.core.jooq;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = {"http://www.jooq.org", "3.0.0"},
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked" })
public class Public extends org.jooq.impl.SchemaImpl {

	private static final long serialVersionUID = 1548699597;

	/**
	 * The singleton instance of <code>PUBLIC</code>
	 */
	public static final Public PUBLIC = new Public();

	/**
	 * No further instances allowed
	 */
	private Public() {
		super("PUBLIC");
	}

	@Override
	public final java.util.List<org.jooq.Sequence<?>> getSequences() {
		java.util.List result = new java.util.ArrayList();
		result.addAll(getSequences0());
		return result;
	}

	private final java.util.List<org.jooq.Sequence<?>> getSequences0() {
		return java.util.Arrays.<org.jooq.Sequence<?>>asList(
			org.wikapidia.core.jooq.Sequences.SYSTEM_SEQUENCE_07AE83D4_631F_4831_A6D6_23B3AEDB72F2,
			org.wikapidia.core.jooq.Sequences.SYSTEM_SEQUENCE_24CF6177_79C2_4A58_9BCF_EE31011AE406);
	}

	@Override
	public final java.util.List<org.jooq.Table<?>> getTables() {
		java.util.List result = new java.util.ArrayList();
		result.addAll(getTables0());
		return result;
	}

	private final java.util.List<org.jooq.Table<?>> getTables0() {
		return java.util.Arrays.<org.jooq.Table<?>>asList(
			org.wikapidia.core.jooq.tables.LocalPage.LOCAL_PAGE,
			org.wikapidia.core.jooq.tables.LocalLink.LOCAL_LINK,
			org.wikapidia.core.jooq.tables.UniversalPage.UNIVERSAL_PAGE);
	}
}
