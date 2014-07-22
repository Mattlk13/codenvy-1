/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.cli.command.builtin;

import com.codenvy.cli.command.builtin.model.DefaultUserRunnerStatus;
import com.codenvy.cli.command.builtin.model.UserProject;
import com.codenvy.cli.command.builtin.model.UserRunnerStatus;
import com.codenvy.client.Codenvy;
import com.codenvy.client.model.Project;
import com.codenvy.client.model.RunnerStatus;
import com.codenvy.client.model.Workspace;
import com.codenvy.client.model.WorkspaceReference;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.fusesource.jansi.Ansi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static com.codenvy.client.model.RunnerState.STOPPED;
import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;

/**
 * Allows to run a given project
 * @author Florent Benoit
 */
@Command(scope = "codenvy", name = "run", description = "Run a project in Codenvy System")
public class RunnerCommand extends ScopedIDCommand {


    /**
     * Execute the command
     */
    @Override
    protected Object doExecute() throws Exception {
        final Codenvy current = checkLoggedIn();

        // not logged in
        if (current == null) {
            return null;
        }

        // do we have the projectID ?
        String projectShortId = getScopedProjectId();
        if (projectShortId == null) {
            Ansi buffer = Ansi.ansi();
            buffer.fg(Ansi.Color.RED);
            buffer.a("No projectID has been set");
            buffer.fg(Ansi.Color.DEFAULT);
            session.getConsole().println(buffer.toString());
            return null;
        }

        // get project for the given shortID
        UserProject project = getProject(current, projectShortId);

        if (project == null) {
            Ansi buffer = Ansi.ansi();
            buffer.fg(Ansi.Color.RED);
            buffer.a("No matching project for identifier '").a(projectShortId).a("'.");
            buffer.fg(Ansi.Color.DEFAULT);
            session.getConsole().println(buffer.toString());
            return null;
        }


        final Project projectToRun = project.getInnerProject();

        // Ok now we have the project
        final RunnerStatus runnerStatus = current.runner().run(projectToRun).execute();

        UserRunnerStatus userRunnerStatus = new DefaultUserRunnerStatus(runnerStatus, project);

        Ansi buffer = Ansi.ansi();
        buffer.a("Run task for project ").a(INTENSITY_BOLD).a(project.name()).a(INTENSITY_BOLD_OFF).a("' has been submitted with process ID ").a(INTENSITY_BOLD).a(userRunnerStatus.shortId()).a(INTENSITY_BOLD_OFF);
        session.getConsole().println(buffer.toString());

        return null;
    }
}

