/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;


/**
 * EclipseProjectBuilder
 *
 * @author christoph
 */
public class EclipseProjectBuilder extends DefaultProjectBuilder {

  @Override
  public ProjectBuildingResult build(Artifact artifact, boolean allowStubModel, ProjectBuildingRequest request)
      throws ProjectBuildingException {
    return super.build(artifact, allowStubModel, request);
  }

  @Override
  public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
      throws ProjectBuildingException {
    return super.build(pomFiles, recursive, request);
  }

  @Override
  public ProjectBuildingResult build(Artifact artifact, ProjectBuildingRequest request)
      throws ProjectBuildingException {
    //just a delegate...
    return super.build(artifact, request);
  }

  @Override
  public ProjectBuildingResult build(File pomFile, ProjectBuildingRequest request) throws ProjectBuildingException {
    //just a delegate
    return super.build(pomFile, request);
  }

  @Override
  public ProjectBuildingResult build(ModelSource modelSource, ProjectBuildingRequest request)
      throws ProjectBuildingException {
    //just a delegate
    return super.build(modelSource, request);
  }

}
