package org.eclipse.m2e.pde.ui;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation.TargetPlatformPresentationReconciler;
import org.eclipse.ui.PlatformUI;

public class PresentationReconciler1 extends TargetPlatformPresentationReconciler {
	
//	TargetPlatformPresentationReconciler delegate;
	public PresentationReconciler1() {
		RuleBasedScanner scanner = new RuleBasedScanner();

//		RuleBasedScanner scanner = new RuleBasedScanner();
		ColorRegistry manager = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		IRule[] rules = new IRule[1];
//		rules[0] = new SingleLineRule("<?", "?>", //$NON-NLS-1$ //$NON-NLS-2$
//				new Token(new TextAttribute(manager.get(IGETEColorConstants.P_HEADER))));
//		rules[1] = new SingleLineRule("\"", "\"", //$NON-NLS-1$ //$NON-NLS-2$
//				new Token(new TextAttribute(manager.get(IGETEColorConstants.P_QUOTE))));
		rules[0] = new MultiLineRule("<![CDATA[", "]]>", //$NON-NLS-1$ //$NON-NLS-2$
				new Token(new TextAttribute(
						manager.get("org.eclipse.pde.genericeditor.extension.editor.color.comment"))));
//		rules[3] = new TargetPlatformTagRule();
//		rules[4] = new TargetPlatformAttributeRule();
//		rules[5] = new GeneralTagRule();
//		scanner.setRules(rules);
//		dr.updateTokenScanner(scanner);

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		this.setDamager(dr, BndDocumentPartitions.INSTRUCTIONS_CONTENT_TYPE);
		this.setRepairer(dr, BndDocumentPartitions.INSTRUCTIONS_CONTENT_TYPE);
//		delegate = new TargetPlatformPresentationReconciler();
	}

//	@Override
//	protected void setDocumentToDamagers(IDocument document) {
//		// TODO Auto-generated method stub
//		super.setDocumentToDamagers(document);
//	}
//
//	@Override
//	protected void setDocumentToRepairers(IDocument document) {
//		// TODO Auto-generated method stub
//		super.setDocumentToRepairers(document);
//	}
//
	@Override
	public IPresentationDamager getDamager(String contentType) {
		IPresentationDamager damager = super.getDamager(contentType);
		return damager;
	}
//
	@Override
	public IPresentationRepairer getRepairer(String contentType) {
		IPresentationRepairer repairer = super.getRepairer(contentType);
		System.out.println("PresentationReconciler1.getRepairer() " + contentType);
		return repairer;
	}
//
//	@Override
//	public void install(ITextViewer viewer) {
//		super.install(viewer);
//		delegate.install(viewer);
//	}
//
//	@Override
//	public void uninstall() {
//		delegate.uninstall();
//		super.uninstall();
//	}

}
