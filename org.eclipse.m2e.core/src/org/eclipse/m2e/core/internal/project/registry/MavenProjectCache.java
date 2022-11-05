/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectRealmCache;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.project.IManagedCache;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * MavenProjectCache supports caching of {@link MavenProject} instances in an memory efficient way
 */
public class MavenProjectCache {

  private static final Logger log = LoggerFactory.getLogger(MavenProjectCache.class);

  private static final String CTX_MAVENPROJECTS = MavenProjectCache.class.getName() + "/mavenProjects";

  private LoadingCache<ArtifactKey, CacheLine> loadingCache;

  private AtomicLong projectsReadWithoutCache = new AtomicLong();

  public MavenProjectCache(int maxCacheSize) {

    this.loadingCache = CacheBuilder.newBuilder() //
        .recordStats().maximumSize(maxCacheSize) //
        .removalListener((RemovalNotification<ArtifactKey, CacheLine> removed) -> {
          log.debug("Removing " + removed.getKey() + " form cache because of " + removed.getCause());
          Map<IMavenProjectFacade, MavenProject> contextProjects = getContextProjectMap();
          removed.getValue().projects.values().forEach(mavenProject -> {
            if(!contextProjects.containsValue(mavenProject)) {
              flushMavenCaches(mavenProject.getFile(), removed.getKey(), false,
                  MavenPluginActivator.getDefault().getContainerManager().getComponentLookup(mavenProject.getFile()));
            }
          });
        }).build(new CacheLoader<ArtifactKey, CacheLine>() {

          @Override
          public CacheLine load(ArtifactKey key) throws Exception {
            return new CacheLine();
          }

        });
  }

  /**
   * @param facade
   */
  public void invalidateFacade(IMavenProjectFacade facade) {
    CacheLine cacheLine = loadingCache.getIfPresent(facade.getArtifactKey());
    if(cacheLine != null) {
    cacheLine.remove(facade.getPomFile());
    }
  }

  /**
   * @param fac
   * @param object
   * @return
   */
  public MavenProject getMavenProjectForFacade(IMavenProjectFacade facade,
      Function<IMavenProjectFacade, MavenProject> loader) {
    ArtifactKey artifactKey = facade.getArtifactKey();
    CacheLine cacheLine = loadingCache.getUnchecked(artifactKey);
    return cacheLine.getProject(facade, loader);
  }

  /**
   * @param newFacade
   * @param mavenProject
   */
  public void putMavenProject(IMavenProjectFacade facade, MavenProject mavenProject) {
    ArtifactKey artifactKey = facade.getArtifactKey();
    CacheLine cacheLine = loadingCache.getUnchecked(artifactKey);
    cacheLine.updateProject(facade, mavenProject);
  }

  void cleanUp() {
    // TODO Auto-generated method cleanUp
    log.debug("Perform cache cleanup tasks...");
    log.debug("There are " + loadingCache.size() + " items in the cache.. searching fo duplicates...");
    log.debug("Cache statistics: " + loadingCache.stats().toString());
    ConcurrentMap<ArtifactKey, CacheLine> map = loadingCache.asMap();
    Set<String> uniqueFacadeSet = new HashSet<>();
    Set<String> uniqueProjects = new HashSet<>();
    IdentityHashMap<MavenProject, Boolean> allProjects = new IdentityHashMap<>();
    for(var entry : map.entrySet()) {
      entry.getValue().projects.values().forEach(mavenProject -> {
        addProjectToSet(mavenProject, uniqueProjects, allProjects);
      });
    }
    //TODO we should do here:
    //- String deduplication
    //- Project deduplication
    //- ... other objects deduplication (dependencies objects?)
    log.debug("There are " + uniqueFacadeSet.size() + " unique facades in the cache (according to GAV)");
    log.debug("There are " + uniqueProjects.size() + " unique projects in the cache (according to GAV) and a total of "
        + allProjects.size());
    log.debug("Projects read without caching: " + projectsReadWithoutCache.get());
  }

  private final class CacheLine {

    private ConcurrentMap<File, MavenProject> projects = new ConcurrentHashMap<>(1);

    void remove(File pomFile) {
      projects.remove(pomFile);
    }

    /**
     * @param facade
     * @param mavenProject
     */
    public void updateProject(IMavenProjectFacade facade, MavenProject mavenProject) {
      File pomFile = facade.getPomFile();
      projects.compute(pomFile, (key, current) -> {
        distributeProjectToCache(mavenProject);
        return mavenProject;
      });

    }

    /**
     * @param facade
     * @param loader
     * @param projectsReadWithoutCache
     * @return
     */
    public MavenProject getProject(IMavenProjectFacade facade, Function<IMavenProjectFacade, MavenProject> loader) {
      File pomFile = facade.getPomFile();
      if(loader == null) {
        return projects.get(pomFile);
      }
      return projects.computeIfAbsent(pomFile, f -> {
        projectsReadWithoutCache.incrementAndGet();
        MavenProject mavenProject = loader.apply(facade);
        distributeProjectToCache(mavenProject);
        return mavenProject;
      });
    }

  }

  private void distributeProjectToCache(MavenProject mavenProject) {
    ArtifactKey distributionKey = new ArtifactKey(mavenProject.getArtifact());
    //collect all unique parents
    Set<MavenProject> collected = new HashSet<>();
    collectMavenProjects(mavenProject, collected);
    collected.remove(mavenProject);
    //distribute to cache...
    for(MavenProject project : collected) {
      ArtifactKey projectKey = new ArtifactKey(project.getArtifact());
      if(!projectKey.equals(distributionKey)) {
        File file = project.getFile();
        if(file != null) {
          loadingCache.getUnchecked(projectKey).projects.putIfAbsent(file, project);
        }
      }
    }
  }

  static void addProjectToSet(MavenProject value, Set<String> uniqueProjects,
      IdentityHashMap<MavenProject, Boolean> allProjects) {
    if(value == null) {
      return;
    }
    allProjects.put(value, true);
    uniqueProjects.add(value.getGroupId() + ":" + value.getArtifactId() + ":" + value.getVersion());
    addProjectToSet(value.getParent(), uniqueProjects, allProjects);
  }

  private static void collectMavenProjects(MavenProject mavenProject, Set<MavenProject> collected) {
    if(mavenProject == null) {
      return;
    }
    if(collected.add(mavenProject)) {
      collectMavenProjects(mavenProject.getParent(), collected);
    }
  }

  static Map<IMavenProjectFacade, MavenProject> getContextProjectMap() {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext(false);
    if(context != null) {
      Map<IMavenProjectFacade, MavenProject> projects = context.getValue(CTX_MAVENPROJECTS);
      if(projects == null) {
        projects = new IdentityHashMap<>();
        context.setValue(CTX_MAVENPROJECTS, projects);
      }
      return projects;
    }
    return new IdentityHashMap<>();
  }

  static Set<File> flushMavenCaches(File baseDir, ArtifactKey key, boolean force, IComponentLookup componentLookup) {
    Set<File> affected = new HashSet<>();
    affected.addAll(flushMavenCache(ProjectRealmCache.class, baseDir, key, force, componentLookup));
    affected.addAll(flushMavenCache(ExtensionRealmCache.class, baseDir, key, force, componentLookup));
    affected.addAll(flushMavenCache(PluginRealmCache.class, baseDir, key, force, componentLookup));
    affected.addAll(flushMavenCache(MavenMetadataCache.class, baseDir, key, force, componentLookup));
    affected.addAll(flushMavenCache(PluginArtifactsCache.class, baseDir, key, force, componentLookup));
    return affected;
  }

  private static Set<File> flushMavenCache(Class<?> clazz, File pom, ArtifactKey key, boolean force,
      IComponentLookup componentLookup) {
    try {
      Object lookup = componentLookup.lookup(clazz);
      if(lookup instanceof IManagedCache cache) {
        return cache.removeProject(pom, key, force);
      }
    } catch(CoreException ex) {
      // can't do anything here...
    }
    return Collections.emptySet();
  }

}
