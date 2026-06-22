package de.wlad.kiratracker;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private final WalkService walkService = mock(WalkService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final PauseRepository pauseRepository = mock(PauseRepository.class);

    private ReminderService service() {
        return new ReminderService(walkService, notificationService, pauseRepository);
    }

    private void pause(Integer index) {
        PauseState state = new PauseState();
        state.setPauseIndex(index);
        when(pauseRepository.findById(1L)).thenReturn(Optional.of(state));
    }

    @Test
    void morningReminder_sendsOneMessage_whenNotWalkedAndNoPause() {
        when(walkService.wasMorning()).thenReturn(false);
        pause(null);

        service().checkMorningReminder();

        verify(notificationService, times(1)).sendCustomNotification(anyString());
    }

    @Test
    void morningReminder_silent_whenAlreadyWalked() {
        when(walkService.wasMorning()).thenReturn(true);

        service().checkMorningReminder();

        verifyNoInteractions(notificationService);
    }

    @Test
    void morningReminder_silent_whenPaused() {
        when(walkService.wasMorning()).thenReturn(false);
        pause(0);

        service().checkMorningReminder();

        verifyNoInteractions(notificationService);
    }

    @Test
    void eveningReminder_sendsOneMessage_whenNotWalkedAndNoPause() {
        when(walkService.wasEvening()).thenReturn(false);
        pause(null);

        service().checkEveningReminder();

        verify(notificationService, times(1)).sendCustomNotification(anyString());
    }

    @Test
    void eveningReminder_silent_whenPaused() {
        when(walkService.wasEvening()).thenReturn(false);
        pause(0);

        service().checkEveningReminder();

        verifyNoInteractions(notificationService);
    }
}
