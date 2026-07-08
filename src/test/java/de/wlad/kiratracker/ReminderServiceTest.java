package de.wlad.kiratracker;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private final WalkService walkService = mock(WalkService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final PauseRepository pauseRepository = mock(PauseRepository.class);
    private final WeatherService weatherService = mock(WeatherService.class);

    private ReminderService service() {
        return new ReminderService(walkService, notificationService, pauseRepository, weatherService);
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

    @Test
    void heatWarning_sendsMessageWithWindows_whenRedAndNoPause() {
        pause(null);
        WeatherWindowDto morning = new WeatherWindowDto("07:00", "10:00");
        WeatherWindowDto evening = new WeatherWindowDto("20:00", "22:00");
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 3, morning, evening));

        service().checkHeatWarning();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).sendCustomNotification(captor.capture());
        assertThat(captor.getValue()).contains("07:00–10:00").contains("20:00–22:00");
    }

    @Test
    void heatWarning_sendsShortRoundsMessage_whenNoSafeWindow() {
        pause(null);
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 3, null, null));

        service().checkHeatWarning();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).sendCustomNotification(captor.capture());
        assertThat(captor.getValue()).contains("kurze, schattige Runden");
    }

    @Test
    void heatWarning_silent_whenBelowRed() {
        pause(null);
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 2, null, null));

        service().checkHeatWarning();

        verifyNoInteractions(notificationService);
    }

    @Test
    void heatWarning_silent_whenPaused() {
        pause(0);

        service().checkHeatWarning();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(weatherService);
    }
}
