package de.wlad.kiratracker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final NotificationService notificationService;
    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");

    public FoodService(FoodRepository foodRepository, NotificationService notificationService) {
        this.foodRepository = foodRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FoodEntryDto addFood(String person, String food) {
        if (person == null || person.trim().isEmpty()) {
            throw new IllegalArgumentException("Person darf nicht leer sein.");
        }
        FoodEntry entry = new FoodEntry(
                person.trim(),
                food != null ? food.trim() : "",
                ZonedDateTime.now(BERLIN_ZONE)
        );
        FoodEntry saved = foodRepository.save(entry);
        notificationService.sendFoodNotification(person.trim());
        return toDto(saved);
    }

    public List<FoodEntryDto> getTodayFood() {
        ZonedDateTime startOfDay = ZonedDateTime.now(BERLIN_ZONE)
                .toLocalDate().atStartOfDay(BERLIN_ZONE);
        ZonedDateTime endOfDay = startOfDay.plusDays(1);
        return foodRepository
                .findByTimestampBetweenOrderByTimestampDesc(startOfDay, endOfDay)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFood(Long id) {
        if (!foodRepository.existsById(id)) {
            throw new IllegalArgumentException("Futterung nicht gefunden: " + id);
        }
        foodRepository.deleteById(id);
    }

    @Transactional
    public FoodEntryDto updateFood(Long id, String person, String food) {
        FoodEntry entry = foodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Futterung nicht gefunden: " + id));
        if (person != null && !person.trim().isEmpty()) {
            entry.setPerson(person.trim());
        }
        if (food != null) {
            entry.setFood(food.trim());
        }
        return toDto(foodRepository.save(entry));
    }

    private FoodEntryDto toDto(FoodEntry entry) {
        return new FoodEntryDto(
                entry.getId(),
                entry.getPerson(),
                entry.getFood(),
                entry.getTimestamp().withZoneSameInstant(BERLIN_ZONE).format(FORMATTER)
        );
    }
}
