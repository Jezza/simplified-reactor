package com.jezza.github;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "reactor")
public class ReactorLifecycle extends AbstractMavenLifecycleParticipant {
	private static final String CHILDREN_PROP = "reactor.children";

	@Requirement
	Logger log;

	@Requirement
	ProjectBuilder builder;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		log.info("Looking up children...");

		ProjectBuildingRequest pbr = session.getProjectBuildingRequest();

		List<MavenProject> projects = session.getProjects();
		if (projects.size() != 1) {
			log.warn("Don't know how to handle multiple (" + projects.size() + ") projects...");
			return;
		}
		MavenProject project = session.getCurrentProject();

		File base = project.getFile().getParentFile();

		String property = project.getProperties().getProperty(CHILDREN_PROP);
		if (property == null || property.isEmpty()) {
			log.info("No \"" + CHILDREN_PROP + "\" property set; Doing nothing...");
			return;
		}

		String[] files = property.split("\n");
		if (files.length == 0) {
			return;
		}

		for (String file : files) {
			file = file.trim();
			if (file.isEmpty()) {
				continue;
			}

			File target = findTarget(base, file);
			if (target == null) {
				continue;
			}

			ProjectBuildingResult result;
			try {
				result = builder.build(target, pbr);
			} catch (ProjectBuildingException e) {
				throw new MavenExecutionException("Unable to read file: " + target, e);
			}

			projects.add(result.getProject());
		}
	}

	private File findTarget(File base, String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		Path resolved = base.toPath()
				.resolve(path)
				.normalize();

		if (Files.isDirectory(resolved)) {
			resolved = resolved.resolve("pom.xml");
		}
		if (!Files.exists(resolved)) {
			log.warn("File/Directory does not exist: " + resolved);
			return null;
		}
		return resolved.toFile();
	}
}
