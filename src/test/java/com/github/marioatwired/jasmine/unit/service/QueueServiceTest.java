/*
 * Copyright 2026 Arif Banai (arif-banai)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import com.github.marioatwired.jasmine.service.QueueService;
import com.github.marioatwired.jasmine.testutil.service.ServiceTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.marioatwired.jasmine.testutil.TestConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueServiceTest
{
    private ServiceTestFixture fixture;
    private QueueService queueService;
    private OutputAdapter output;

    @BeforeEach
    void setUp()
    {
        fixture = ServiceTestFixture.create();
        queueService = new QueueService(fixture.getBot());
        output = mock(OutputAdapter.class);
    }

    @Test
    void moveTrackMovesForDj()
    {
        fixture.withDJPermission().withQueueSize(5);
        QueuedTrack track = fixture.getQueue().get(1);
        when(fixture.getQueue().moveItem(1, 4)).thenReturn(track);

        queueService.moveTrack(fixture.getGuild(), fixture.getMember(), 2, 5, output);

        verify(fixture.getQueue()).moveItem(1, 4);
        verify(output).replySuccess(contains("Moved"));
    }

    @Test
    void moveTrackRejectsSamePosition()
    {
        fixture.withDJPermission().withQueueSize(5);

        queueService.moveTrack(fixture.getGuild(), fixture.getMember(), 2, 2, output);

        verify(fixture.getQueue(), never()).moveItem(1, 1);
        verify(output).replyError("Can't move a track to the same position.");
    }

    @Test
    void removeTrackRemovesRequestersOwnTrack()
    {
        fixture.withQueueSize(3);
        QueuedTrack track = fixture.getQueue().get(1);
        when(track.getIdentifier()).thenReturn(USER_ID);

        queueService.removeTrack(fixture.getGuild(), fixture.getMember(), 2, output);

        verify(fixture.getQueue()).remove(1);
        verify(output).replySuccess(contains("Removed"));
    }

    @Test
    void removeAllTracksByUserReturnsRemovedCount()
    {
        fixture.withQueueSize(3);
        when(fixture.getQueue().removeAll(USER_ID)).thenReturn(3);

        assertEquals(3, queueService.removeAllTracksByUser(fixture.getGuild(), USER_ID));
        verify(fixture.getQueue()).removeAll(USER_ID);
    }

    @Test
    void playNextAtFirstPositionOnlyWarns()
    {
        queueService.playNext(fixture.getGuild(), fixture.getMember(), 1, output);

        verify(output).replyWarning("This track is already next!");
        verify(fixture.getQueue(), never()).moveItem(0, 0);
    }

    @Test
    void moveTrackPositionReturnsNullForInvalidPosition()
    {
        fixture.withQueueSize(2);

        assertNull(queueService.moveTrackPosition(fixture.getGuild(), 1, 5));
    }
}
