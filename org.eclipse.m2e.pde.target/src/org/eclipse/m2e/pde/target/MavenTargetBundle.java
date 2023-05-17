/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.RepositoryUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.pde.core.target.TargetBundle;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public class MavenTargetBundle extends TargetBundle {

	private static final ILog LOGGER = Platform.getLog(MavenTargetBundle.class);
	private TargetBundle bundle;
	private IStatus status;
	private final BundleInfo bundleInfo;
	private boolean isWrapped;
	private Artifact artifact;

	@Override
	public BundleInfo getBundleInfo() {
		if (bundle == null) {
			return bundleInfo;
		}
		return bundle.getBundleInfo();
	}

	@Override
	public boolean isSourceBundle() {
		return bundle != null && bundle.isSourceBundle();
	}

	@Override
	public BundleInfo getSourceTarget() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourceTarget();
	}

	@Override
	public boolean isFragment() {
		return bundle != null && bundle.isFragment();
	}

	@Override
	public String getSourcePath() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourcePath();
	}

	public MavenTargetBundle(Artifact artifact, BNDInstructions instructions, CacheManager cacheManager,
			MissingMetadataMode metadataMode) {
		this.artifact = artifact;
		File file = artifact.getFile();
		this.bundleInfo = new BundleInfo(artifact.getGroupId() + "." + artifact.getArtifactId(), artifact.getVersion(),
				file != null ? file.toURI() : null, -1, false);
		try {
			bundle = new TargetBundle(file);
		} catch (Exception ex) {
			if (metadataMode == MissingMetadataMode.ERROR) {
				status = Status.error(artifact + " is not a bundle", ex);
				LOGGER.log(status);
			} else if (metadataMode == MissingMetadataMode.GENERATE) {
				try {
					Properties bndInstructions = instructions == null
							? BNDInstructions.getDefaultInstructionProperties()
							: instructions.asProperties();
					bundle = cacheManager.accessArtifactFile(artifact,
							artifactFile -> getWrappedArtifact(artifact, bndInstructions, artifactFile));
					isWrapped = true;
				} catch (Exception e) {
					// not possible then
					String message = artifact + " is not a bundle and cannot be automatically bundled as such ";
					if (e.getMessage() != null) {
						message += " (" + e.getMessage() + ")";
					}
					status = Status.error(message, e);
					LOGGER.log(status);
				}
			} else {
				status = Status.CANCEL_STATUS;
				LOGGER.log(status);
			}
		}
	}

	public Artifact getArtifact() {
		return artifact;
	}

	@SuppressWarnings("removal")
	public static TargetBundle getWrappedArtifact(Artifact artifact, Properties bndInstructions, File wrappedFile)
			throws Exception {
		System.out.println("---- " + artifact + " ----");
		// TODO must be passed in!
//		IMaven maven = MavenPlugin.getMaven();
//		List<ArtifactRepository> repositories = maven.getArtifactRepositories();
//		maven.execute(new ICallable<Void>() {
//
//			@Override
//			public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
//				// TODO Auto-generated method stub
//				CollectRequest collectRequest = new CollectRequest();
//				collectRequest.setRoot(new Dependency(artifact, null));
//				collectRequest.setRepositories(RepositoryUtils.toRepos(repositories));
//				DependencyRequest dependencyRequest = new DependencyRequest();
//				RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
//				DependencyNode node;
//				try {
//					node = repoSystem.collectDependencies(context.getRepositorySession(), collectRequest).getRoot();
//					dependencyRequest.setRoot(node);
//					TreeDependencyVisitor nlg = new TreeDependencyVisitor(new DependencyVisitor() {
//
//						@Override
//						public boolean visitLeave(DependencyNode node) {
//							return true;
//						}
//
//						@Override
//						public boolean visitEnter(DependencyNode node) {
//							System.out.println("enter: " + node);
//							return true;
//						}
//					});
//					node.accept(nlg);
//				} catch (DependencyCollectionException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return null;
//			}
//		}, null);
		// FIXME
		try {
			long start = System.currentTimeMillis();
			Path wrappedBundle = WrapManager.getWrappedBundle(artifact, always -> bndInstructions,
				RepositoryUtils.toRepos(MavenPlugin.getMaven().getArtifactRepositories()));
			TargetBundle targetBundle = new TargetBundle(wrappedBundle.toFile());
			System.out.println("Wrapping takes " + (System.currentTimeMillis() - start) + "ms --> " + wrappedBundle);
			return targetBundle;
		} catch (Exception e) {
			e.printStackTrace();
		}


		File artifactFile = artifact.getFile();
		File instructionsFile = new File(wrappedFile.getParentFile(),
				FilenameUtils.getBaseName(wrappedFile.getName()) + ".xml");
		if (CacheManager.isOutdated(wrappedFile, artifactFile)
				|| propertiesChanged(bndInstructions, instructionsFile)) {
			try (Jar jar = new Jar(artifactFile)) {
				Manifest originalManifest = jar.getManifest();
				try (Analyzer analyzer = new Analyzer();) {
					analyzer.setJar(jar);
					if (originalManifest != null) {
						analyzer.mergeManifest(originalManifest);
					}
					analyzer.setProperty("mvnGroupId", artifact.getGroupId());
					analyzer.setProperty("mvnArtifactId", artifact.getArtifactId());
					analyzer.setProperty("mvnVersion", artifact.getBaseVersion());
					analyzer.setProperty("mvnClassifier", artifact.getClassifier());
					analyzer.setProperty("generatedOSGiVersion", TargetBundles.createOSGiVersion(artifact).toString());
					analyzer.setProperties(bndInstructions);
					jar.setManifest(analyzer.calcManifest());
					jar.write(wrappedFile);
					wrappedFile.setLastModified(artifactFile.lastModified());
				}
			}
			TargetBundle targetBundle = new TargetBundle(wrappedFile);
			try (FileOutputStream os = new FileOutputStream(instructionsFile)) {
				bndInstructions.storeToXML(os, null);
			}
			return targetBundle;
		}
		try {
			return new TargetBundle(wrappedFile);
		} catch (Exception e) {
			// cached file seems invalid/stale...
			FileUtils.forceDelete(wrappedFile);
			return getWrappedArtifact(artifact, bndInstructions, wrappedFile);
		}
	}

	private static boolean propertiesChanged(Properties properties, File file) {
		Properties oldProperties = new Properties();
		if (file.exists()) {
			try {
				try (FileInputStream stream = new FileInputStream(file)) {
					oldProperties.loadFromXML(stream);
				}
				return oldProperties.equals(properties);
			} catch (IOException e) {
				// fall through and assume changed then
			}
		}
		return true;
	}

	public boolean isWrapped() {
		return isWrapped;
	}

	@Override
	public IStatus getStatus() {
		if (bundle == null) {
			if (status == null) {
				return Status.OK_STATUS;
			}
			return status;
		}
		return bundle.getStatus();
	}

	@Override
	public int hashCode() {
		return getBundleInfo().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof MavenTargetBundle other && getBundleInfo().equals(other.getBundleInfo());
	}

}
