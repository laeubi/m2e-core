package org.eclipse.m2e.pde.ui;

public interface BndDocumentPartitions {

	String BND_INSTRUCTIONS_PARTITIONING = "___m2e_target_bnd_partitioning";

	String INSTRUCTIONS_CONTENT_TYPE = "__m2e_target_bnd_instructions";

	public static final String[] PARTITIONS = new String[] { INSTRUCTIONS_CONTENT_TYPE };
}
