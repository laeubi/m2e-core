/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;


/**
 * EclipseArtifactFilterManager
 *
 * @author igor
 */
public class EclipseClassRealmManagerDelegate implements ClassRealmManagerDelegate {

  public static final String ROLE_HINT = EclipseClassRealmManagerDelegate.class.getName();

  private static final String PLEXUSBUILDCONTEXT_PROPERTIES = "/org/sonatype/plexus/build/incremental/version.properties"; //$NON-NLS-1$

  private final PlexusContainer plexus;

  private static final ArtifactVersion legacytBuildApiVersion;

  static {
    Properties props = new Properties();
    try (InputStream is = EclipseClassRealmManagerDelegate.class.getResourceAsStream(PLEXUSBUILDCONTEXT_PROPERTIES)) {
      if(is != null) {
        props.load(is);
      }
    } catch(IOException e) {
      // TODO log
    }
    legacytBuildApiVersion = new DefaultArtifactVersion(props.getProperty("api.version", "0.0.5")); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Inject
  public EclipseClassRealmManagerDelegate(PlexusContainer plexus) {
    this.plexus = plexus;
  }

  @Override
  public void setupRealm(ClassRealm realm, ClassRealmRequest request) {
    if(supportsBuildApi(request.getConstituents())) {
      ClassRealm coreRealm = plexus.getContainerRealm();

      realm.importFrom(coreRealm, "org.codehaus.plexus.util.DirectoryScanner"); //$NON-NLS-1$
      realm.importFrom(coreRealm, "org.codehaus.plexus.util.AbstractScanner"); //$NON-NLS-1$
      realm.importFrom(coreRealm, "org.codehaus.plexus.util.Scanner"); //$NON-NLS-1$

      realm.importFrom(coreRealm, "org.sonatype.plexus.build.incremental"); //$NON-NLS-1$
      realm.importFrom(coreRealm, "org.codehaus.plexus.build"); //$NON-NLS-1$
    }
  }

  private boolean supportsBuildApi(List<ClassRealmConstituent> constituents) {
    boolean hasBuildApi = false;
    for(Iterator<ClassRealmConstituent> it = constituents.iterator(); it.hasNext();) {
      ClassRealmConstituent constituent = it.next();
      if("org.sonatype.plexus".equals(constituent.getGroupId()) //$NON-NLS-1$
          && "plexus-build-api".equals(constituent.getArtifactId())) { //$NON-NLS-1$
        ArtifactVersion version = new DefaultArtifactVersion(constituent.getVersion());
        if(legacytBuildApiVersion.compareTo(version) >= 0) {
          // removing the JAR from the plugin realm to prevent discovery of the DefaultBuildContext
          it.remove();
          hasBuildApi = true;
        }
      }
      if("org.codehaus.plexus.build".equals(constituent.getGroupId()) //$NON-NLS-1$
          && "plexus-build-api".equals(constituent.getArtifactId())) { //$NON-NLS-1$
        ArtifactVersion version = new DefaultArtifactVersion(constituent.getVersion());
        // currently we support only version 1, we want to design the API that it is always backward compatible for the client
        // so even if we implements Version X and the client Version < X it can be used!
        // for incompatible changes we need to use a different package then
        if(version.getMajorVersion() >= 1 && version.getMajorVersion() <= 2) {
          // removing the JAR from the plugin realm to prevent discovery of the default components
          it.remove();
          hasBuildApi = true;
        }
      }
    }
    return hasBuildApi;
  }

}
