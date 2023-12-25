package org.eclipse.m2e.pde.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class ContentAssistProcessor1 implements IContentAssistProcessor {


	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		System.out.println("ContentAssistProcessor1.computeCompletionProposals()");
		IDocument document = viewer.getDocument();
		if (document instanceof IDocumentExtension3 ext3) {
			String[] partitionings = ext3.getPartitionings();
			for (String part : partitionings) {
				try {
					String contentType = ext3.getContentType(part, offset, false);
					ITypedRegion region = ext3.getPartition(part, offset, false);
					System.out.println(part + " CT " + contentType + " region : " + region);
				} catch (BadLocationException | BadPartitioningException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return new ICompletionProposal[0];
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return new IContextInformation[0];
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
