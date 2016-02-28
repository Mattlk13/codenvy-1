/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.workspace.listener;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;

import com.codenvy.service.http.WorkspaceInfoCache;
import com.codenvy.workspace.event.DeleteWorkspaceEvent;

import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Listeners(MockitoTestNGListener.class)
public class WsCacheCleanupSubscriberTest {
    private static final String ID = "id";

    private static final String NAME = "name";

    private EventService eventService;

    @Mock
    private WorkspaceInfoCache.WorkspaceCacheLoader cacheLoader;

    private WorkspaceInfoCache workspaceInfoCache;

    private WsCacheCleanupSubscriber wsCacheCleanupSubscriber;

    @BeforeMethod
    public void setUp() throws Exception {
        eventService = new EventService();
        workspaceInfoCache = spy(new WorkspaceInfoCache(cacheLoader));
        wsCacheCleanupSubscriber = new WsCacheCleanupSubscriber(eventService, workspaceInfoCache);
    }

    @Test
    public void shouldRemoveItemsOnEvent() throws NotFoundException, ServerException {
        // given
        wsCacheCleanupSubscriber.subscribe();

        // when
        eventService.publish(new DeleteWorkspaceEvent(DtoFactory.newDto(UsersWorkspaceDto.class)
                                                                .withId(ID)
                                                                .withConfig(newDto(WorkspaceConfigDto.class).withName(NAME))
                                                                .withOwner("owner")));

        //then
        verify(workspaceInfoCache, timeout(500)).removeByName(NAME, "owner");
        verify(workspaceInfoCache).removeById(ID);
    }
}