package org.eclipse.m2e.pde.ui.target.provider;

import org.eclipse.jface.text.Document;
import org.eclipse.ui.editors.text.StorageDocumentProvider;

//TODO https://github.com/eclipse-platform/eclipse.platform/issues/885
public class DocumentProvider extends StorageDocumentProvider {

	public DocumentProvider() {
	}

	
	private static final class BndDocument extends Document {
		
	}


}
