/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.commons.env;

import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.Workspace;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Initialize  EnvironmentContext variables; */
@Singleton
public class EnvironmentContextInitializationFilter implements Filter {
    public static final Pattern TENANT_URL_PATTERN = Pattern.compile("^(/ide/)(?!_sso)(rest/|websocket/)?(.+?)(/.*)?$");
    private final File           vfsRootDir;
    private final File           tempVfsRootDir;
    private final File           vfsIndexDir;
    private       WorkspaceCache workspaceCache;

    @Inject
    private WorkspaceDao workspaceDao;

    public EnvironmentContextInitializationFilter() {
        this.vfsRootDir = new File(System.getProperty("tenant.data.dir"));
        this.vfsIndexDir = new File(System.getProperty("java.io.tmpdir"));
        this.tempVfsRootDir = new File(System.getProperty("java.io.tmpdir") + "/tempWorkspacesFS");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        /*this.workspaceCache = (WorkspaceCache)getContainer().getComponentInstanceOfType(WorkspaceCache.class);
        if (workspaceCache == null) {
            throw new IllegalStateException("Workspace id cache is null");
        }*/
        this.workspaceCache = new WorkspaceCache(TimeUnit.MINUTES.toMillis(1), 100);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String requestUrl = httpRequest.getRequestURL().toString();
        String requestUri = httpRequest.getRequestURI();
        Matcher matcher = TENANT_URL_PATTERN.matcher(requestUri);
        try {
            if (matcher.matches()) {
                String tenant = matcher.group(3);

                Workspace workspace = workspaceCache.get(tenant);

                if (workspace == null) {
                    try {
                        workspace = workspaceDao.getByName(tenant);
                    } catch (WorkspaceException e) {
                        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Workspace " + tenant + "is not found");
                        return;
                    }
                    workspaceCache.put(workspace.getName(), workspace);
                }
                EnvironmentContext context = EnvironmentContext.getCurrent();
                if (workspace.isTemporary()) {
                    context.setVariable(EnvironmentContext.VFS_ROOT_DIR, tempVfsRootDir);
                } else {
                    context.setVariable(EnvironmentContext.VFS_ROOT_DIR, vfsRootDir);
                }
                UriBuilder ub = UriBuilder.fromUri(requestUrl).replacePath(null).replaceQuery(null);

                context.setVariable(EnvironmentContext.VFS_INDEX_DIR, vfsIndexDir);
                context.setVariable(EnvironmentContext.MASTERHOST_NAME, request.getServerName());
                context.setVariable(EnvironmentContext.MASTERHOST_PORT, request.getServerPort());
                context.setVariable(EnvironmentContext.MASTERHOST_URL, ub.build().toString());
                context.setWorkspaceName(tenant);
                context.setAccountId(workspace.getOrganizationId());
                context.setVariable(EnvironmentContext.WORKSPACE_URL, ub.replacePath("/ide/").path(tenant).build().toString());
                context.setVariable(EnvironmentContext.GIT_SERVER, "git");
                context.setWorkspaceId(workspace.getId());
                context.setVariable("WORKSPACE_IS_TEMPORARY", workspace.isTemporary());
                if (workspace.isTemporary()) {
//                    if (workspace.getAttribute("is_private") != null) {
//                        context.setVariable("WORKSPACE_IS_PRIVATE", Boolean.parseBoolean(workspace.getAttribute("is_private")));
//                    } else {
//                        workspace = workspaceManager.getWorkspaceById(workspace.getId());
//                        workspaceCache.put(tenant, workspace);
//                        context.setVariable("WORKSPACE_IS_PRIVATE", Boolean.parseBoolean(workspace.getAttribute("is_private", "true")));
//                    }
                    Iterator<Attribute> it = workspace.getAttributes().iterator();
                    while (it.hasNext()) {
                        Attribute attribute = it.next();
                        if (attribute.getName().equals("is_private"))
                            context.setVariable("WORKSPACE_IS_PRIVATE", Boolean.parseBoolean(attribute.getValue()));
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            EnvironmentContext.reset();
        }
    }

    @Override
    public void destroy() {
    }
}
