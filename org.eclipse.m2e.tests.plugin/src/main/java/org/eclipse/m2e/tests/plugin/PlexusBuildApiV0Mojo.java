/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.tests.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * This mojo test that we can use the V1 API
 */
@Mojo(name = "m2e-test-buildapi-v0")
public class PlexusBuildApiV0Mojo extends AbstractMojo {

	@Component
	BuildContext buildContext;

	@Parameter(property = "project", readonly = true)
	MavenProject mavenProject;

	public void execute() throws MojoExecutionException, MojoFailureException {
		String msg = "The buildContext is: " + buildContext.getClass().getName();
		getLog().info(msg);
		buildContext.addMessage(mavenProject.getBasedir(), 1, 0, msg, BuildContext.SEVERITY_WARNING, null);
		// TODO we should have mojo parameter to do some usefull things for test...
	}

}
