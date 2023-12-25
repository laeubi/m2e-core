package org.eclipse.m2e.pde.ui;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class BndDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	@Override
	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3 ext3) {
			
			//new FastPartitioner(new PropertiesFilePartitionScanner(), IPropertiesFilePartitions.PARTITIONS);
			
			IDocumentPartitioner partitioner = new FastPartitioner(new BndInstructionPartitionScanner(),
					BndDocumentPartitions.PARTITIONS);
			ext3.setDocumentPartitioner(BndDocumentPartitions.BND_INSTRUCTIONS_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}

	private static final class BndInstructionPartitionScanner extends RuleBasedPartitionScanner {

		public BndInstructionPartitionScanner() {
			IPredicateRule[] rules = new IPredicateRule[1];
			// TODO not really but for testing it should suffice...
			rules[0] = new MultiLineRule("<![CDATA[", "]]>", new Token(BndDocumentPartitions.INSTRUCTIONS_CONTENT_TYPE), '\\', true);
//			rules[1] = new XMLTagRule(new Token(XML_TAG));
			setPredicateRules(rules);
		}
	}

}
