package com.jezza.github;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "reactor")
public class ReactorLifecycle extends AbstractMavenLifecycleParticipant {
	private static final String CHILDREN_PROP = "reactor.children";
	private static final String PLUGIN_COORDINATES = "com.github.jezza:simplified-reactor-plugin";

	@Requirement
	Logger log;

	@Requirement
	ProjectBuilder builder;

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		log.info("Looking up children... ");

		MavenProject project = session.getCurrentProject();

		String[] children = determineChildren(project);
		if (children == null || children.length == 0) {
			log.info("Unable to determine children or none were declared.");
			return;
		}

		List<MavenProject> projects = session.getProjects();

		Path base = project.getFile().getParentFile().toPath();
		List<MavenProject> newProjects = new ArrayList<>(projects);

		for (String child : children) {
			child = child.trim();
			if (child.isEmpty()) {
				continue;
			}

			Path target = findTarget(base, child);
			if (target == null) {
				continue;
			}

			ProjectBuildingRequest pbr = session.getProjectBuildingRequest();

			ProjectBuildingResult result;
			try {
				result = builder.build(target.toFile(), pbr);
				log.info(target + " contains a valid Maven Project.");
			} catch (ProjectBuildingException e) {
				throw new MavenExecutionException("Unable to read file: " + target, e);
			}

			MavenProject built = result.getProject();
			if (!newProjects.contains(built)) {
				log.info("Added " + target + " to build.");
				newProjects.add(built);
			} else {
				log.info(target + " was already added in build.");
			}
		}

		session.setProjects(newProjects);
	}

	private String[] determineChildren(MavenProject project) {
		Build build = project.getBuild();
		Plugin plugin = build.getPluginsAsMap().get(PLUGIN_COORDINATES);

		Object configuration = plugin.getConfiguration();
		if (configuration instanceof Xpp3Dom) {
			Xpp3Dom config = (Xpp3Dom) configuration;

			String[] children = extract(config);
			if (children != null) {
				return children;
			}
		}

		String property = project.getProperties().getProperty(CHILDREN_PROP);
		if (property == null || property.isEmpty()) {
			log.info("No \"" + CHILDREN_PROP + "\" property set; Doing nothing...");
			return null;
		}

		return property.split("\n");
	}

	private static final String[] EMPTY = new String[0];

	private String[] extract(Xpp3Dom config) {
		Xpp3Dom[] children = config.getChildren("child");
		if (children == null || children.length == 0) {
			return null;
		}

		List<String> paths = new ArrayList<>(children.length);
		for (Xpp3Dom child : children) {
			paths.add(child.getValue().trim());
		}
		return paths.toArray(EMPTY);
	}

	private Path findTarget(Path base, String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		Path target = base.resolve(path)
				.normalize();

		if (Files.isDirectory(target)) {
			target = target.resolve("pom.xml");
		}

		if (Files.exists(target) && Files.isRegularFile(target)) {
			return target;
		}

		log.warn("Unable to locate pom.xml with " + path);
		return null;
	}
}
