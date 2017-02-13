/*
 *  [2012] - [2017] Codenvy, S.A.
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
package com.codenvy.api.workspace.server.filters;

import com.codenvy.api.permission.server.PermissionsManager;
import com.codenvy.api.permission.server.SystemDomain;
import com.codenvy.api.permission.server.model.impl.AbstractPermissions;
import com.google.inject.Inject;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.stack.StackService;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.ws.rs.Path;

import static com.codenvy.api.workspace.server.stack.StackDomain.DELETE;
import static com.codenvy.api.workspace.server.stack.StackDomain.DOMAIN_ID;
import static com.codenvy.api.workspace.server.stack.StackDomain.READ;
import static com.codenvy.api.workspace.server.stack.StackDomain.SEARCH;
import static com.codenvy.api.workspace.server.stack.StackDomain.UPDATE;

/**
 * Restricts access to methods of {@link StackService} by users' permissions
 *
 * <p>Filter should contain rules for protecting of all methods of {@link StackService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/stack{path:(/.*)?}")
public class StackPermissionsFilter extends CheMethodInvokerFilter {

    private final PermissionsManager permissionsManager;

    @Inject
    public StackPermissionsFilter(PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
    }

    @Override
    public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments) throws ForbiddenException, ServerException {
        final String methodName = genericResourceMethod.getMethod().getName();

        final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
        String action;
        String stackId;

        switch (methodName) {
            case "getStack":
            case "getIcon":
                stackId = ((String)arguments[0]);
                action = READ;

                if (currentSubject.hasPermission(DOMAIN_ID, stackId, SEARCH)) {
                    //allow to read stack if user has 'search' permission
                    return;
                }
                break;

            case "updateStack":
            case "uploadIcon":
                stackId = ((String)arguments[1]);
                action = UPDATE;
                break;

            case "removeIcon":
                stackId = ((String)arguments[0]);
                action = UPDATE;
                break;

            case "removeStack":
                stackId = ((String)arguments[0]);
                action = DELETE;
                break;

            case "createStack":
            case "searchStacks":
                //available for all
                return;
            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        if (currentSubject.hasPermission(SystemDomain.DOMAIN_ID, stackId, SystemDomain.MANAGE_SYSTEM_ACTION)
            && isStackPredefined(stackId)) {
            // allow any operation with predefined stack if user has 'manageSystem' permission
            return;
        }

        if (!currentSubject.hasPermission(DOMAIN_ID, stackId, action)) {
            throw new ForbiddenException("The user does not have permission to " + action + " stack with id '" + stackId + "'");
        }
    }

    /**
     * Determines whether stack is predefined.
     * Note, that 'predefined' means public for all users (not necessary provided with system from the box).
     *
     * @param stackId
     *         id of stack to test
     * @return true if stack is predefined, false otherwise
     * @throws ServerException
     *         when any error occurs during permissions fetching
     */
    private boolean isStackPredefined(String stackId) throws ServerException {
        try {
            Page<AbstractPermissions> permissionsPage = null;
            do {
                permissionsPage = getNextPermissionsPage(stackId, permissionsPage);
                for (AbstractPermissions stackPermission : permissionsPage.getItems()) {
                    if ("*".equals(stackPermission.getUserId())) {
                        return true;
                    }
                }
            } while (permissionsPage.hasNextPage());
        } catch (ConflictException | NotFoundException e) { /* consider that stack is not predefined */ }
        return false;
    }

    /**
     * Retrieves next permission page for given stack.
     * If previous page provided as null then first page will be retrieved.
     *
     * @param stackId
     *         id of stack to which permissions will be obtained
     * @param permissionsPage
     *         previous permission page
     * @return next permissions page for given stack
     */
    private Page<AbstractPermissions> getNextPermissionsPage(String stackId,
                                                             @Nullable Page<AbstractPermissions> permissionsPage) throws NotFoundException,
                                                                                                                         ConflictException,
                                                                                                                         ServerException {
        if (permissionsPage == null) {
            // get first page
            return permissionsManager.getByInstance(DOMAIN_ID, stackId, 25, 0);
        }
        if (!permissionsPage.hasNextPage()) {
            return null;
        }

        final Page.PageRef nextPageRef = permissionsPage.getNextPageRef();
        return permissionsManager.getByInstance(DOMAIN_ID, stackId, nextPageRef.getPageSize(), (int) nextPageRef.getItemsBefore());
    }

}
