package org.eclipse.m2e.pde.target;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

/**
 * The {@link WrapManager} handle the hard part of the target support that is
 * wrapping an existing jar into a bundle that is:
 * <ul>
 * <li>Find all dependencies of an artifact</li>
 * <li>For each dependency check if it also needs wrapping</li>
 * <li>Depending on the target and used instructions, the wrapping might be
 * different</li>
 * <li><As wrapping a bundle is a hard task we actually want to cache the data
 * as much as possible/li>
 * <li>At best the code is generic enough so we can have the exact same
 * implementation at Tycho</li>
 * </ul>
 */
public class WrapManager {

	public WrapManager() {
	}

	// ##### M2E specific code section ####
	// THis section is specific to m2e and can reference m2e classes

	public static Path getWrappedBundle(Artifact artifact, Function<DependencyNode, Properties> instructionsLookup,
			List<RemoteRepository> repositories) throws Exception {
		IMaven maven = MavenPlugin.getMaven();
		IMavenExecutionContext context = IMavenExecutionContext.getThreadContext()
				.orElseGet(maven::createExecutionContext);

		return context.execute(new ICallable<Path>() {

			@Override
			public Path call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
				RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
				RepositorySystemSession repositorySession = context.getRepositorySession();
				try {
					return getWrappedArtifact(artifact, instructionsLookup, repositories, repoSystem,
							repositorySession, context.getComponentLookup().lookup(SyncContextFactory.class));
				} catch (Exception e) {
					throw new CoreException(Status.error("Can't collect dependencies!", e));
				}
			}

		}, null);
	}

	// ##### M2E Neutral code section ####
	// This section should only use Aether API and m2e independent classes, if any
	// bugfix is performed here, it should also performed for Tycho accordingly

	private static Path getWrappedArtifact(Artifact artifact, Function<DependencyNode, Properties> instructionsLookup,
			List<RemoteRepository> repositories, RepositorySystem repoSystem, RepositorySystemSession repositorySession,
			SyncContextFactory syncContextFactory)
			throws Exception {
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, null));
		collectRequest.setRepositories(repositories);
		DependencyNode node = repoSystem.collectDependencies(repositorySession, collectRequest).getRoot();

		DependencyRequest dependencyRequest = new DependencyRequest();
		dependencyRequest.setRoot(node);
		repoSystem.resolveDependencies(repositorySession, dependencyRequest);

		HashMap<DependencyNode, WrappedNode> visited = new HashMap<>();
		WrappedNode wrappedNode = getWrappedNode(node, instructionsLookup, visited);
		for (WrappedNode wrap : visited.values()) {
			wrap.jar.close();
		}
		return wrappedNode.wrappedFile.toPath();
	}

	private static WrappedNode getWrappedNode(DependencyNode node,
			Function<DependencyNode, Properties> instructionsLookup, Map<DependencyNode, WrappedNode> visited)
			throws Exception {
		WrappedNode wrappedNode = visited.get(node);
		if (wrappedNode != null) {
			return wrappedNode;
		}
		System.out.println("----- get wrapped for " + node + " -----------");
		Artifact artifact = node.getArtifact();
		File originalFile = artifact.getFile();
		Jar jar = new Jar(originalFile);
		Manifest originalManifest = jar.getManifest();
		if (originalManifest != null
				&& originalManifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
			// already a bundle!
			visited.put(node, wrappedNode = new WrappedNode(node, List.of(), null, originalFile, jar));
			return wrappedNode;
		}
		List<DependencyNode> children = node.getChildren();
		List<WrappedNode> depends = new ArrayList<>();
		for (DependencyNode child : children) {
			depends.add(getWrappedNode(child, instructionsLookup, visited));
		}
		WrappedNode wrappedNodeAfterVisit = visited.get(node);
		if (wrappedNodeAfterVisit != null) {
			return wrappedNodeAfterVisit;
		}
		Properties instructions = instructionsLookup.apply(node);
		String key = getInstructionsKey(instructions, depends);
		synchronized (WrapManager.class) {
			try (Jar analyzerJar = jar) {
				// now we know the key and the depends we enter the critical section of checking
				// if the data is already there or needs to be refreshed
				// TODO check it!
				File parent = new File(originalFile.getParent(), key);
				File wrapArtifactFile = new File(parent, originalFile.getName());
				wrapArtifactFile.getParentFile().mkdirs();
				try (Analyzer analyzer = new Analyzer(analyzerJar);) {
//					analyzer.setJar(analyzerJar);
//					if (originalManifest != null) {
//						analyzer.mergeManifest(originalManifest);
//					}
					analyzer.setProperty("mvnGroupId", artifact.getGroupId());
					analyzer.setProperty("mvnArtifactId", artifact.getArtifactId());
					analyzer.setProperty("mvnVersion", artifact.getBaseVersion());
					analyzer.setProperty("mvnClassifier", artifact.getClassifier());
					String versionString = TargetBundles.createOSGiVersion(artifact).toString();
					analyzer.setProperty("generatedOSGiVersion", versionString);
					for (String property : instructions.stringPropertyNames()) {
						// See https://github.com/bndtools/bnd/issues/5659
						String trimValue = instructions.getProperty(property).trim();
						analyzer.setProperty(property, trimValue);
					}
//					Version version = Version.parseVersion("9.5.0 ");
//					aQute.bnd.version.Version parseVersion = aQute.bnd.version.Version.parseVersion("9.5.0 ");
					for (WrappedNode dep : depends) {
						analyzer.addClasspath(new Jar(dep.wrappedFile));
//						analyzer.removeClose(dep.jar);
					}
					analyzerJar.setManifest(analyzer.calcManifest());
					analyzerJar.write(wrapArtifactFile);
//					wrappedFile.setLastModified(artifactFile.lastModified());
					analyzerJar.writeManifest(System.out);
					for (String err : analyzer.getErrors()) {
						System.out.println("ERR: " + err);
					}
					for (String warn : analyzer.getWarnings()) {
						System.out.println("WARN: " + warn);
					}
				}
				visited.put(node,
						wrappedNode = new WrappedNode(node, depends, key, wrapArtifactFile, new Jar(wrapArtifactFile)));
				return wrappedNode;
			}
		}
	}

	private static String getInstructionsKey(Properties properties, List<WrappedNode> depends) {
		Stream<String> instructionsStream = properties == null ? Stream.empty()
				: properties.stringPropertyNames().stream().sorted(String.CASE_INSENSITIVE_ORDER)
						.map(key -> key.toLowerCase() + ":" + properties.getProperty(key));
		Stream<String> dependsStream = depends.stream().map(WrappedNode::instructionsKey).filter(Objects::nonNull)
				.sorted(String.CASE_INSENSITIVE_ORDER).distinct();
		String string = Stream.concat(instructionsStream, dependsStream).collect(Collectors.joining("#"));
		return DigestUtils.md5Hex(string);
	}

	private static final record WrappedNode(DependencyNode node, List<WrappedNode> depends, String instructionsKey,
			File wrappedFile, Jar jar) {

	}
}
