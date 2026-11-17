package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.config.ApplicationConfig;
import app.contacts.repository.GroupRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

class RemovalJobTest {

  private static final Instant NOW = Instant.parse("2026-04-04T11:00:00Z");

  private final GroupRepository groups = mock(GroupRepository.class);
  private final PermanentDeleteService permanentDelete = mock(PermanentDeleteService.class);
  private final RemovalJob job =
      new RemovalJob(groups, permanentDelete, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void br08RemovesInEveryGroupTheContactsWhoseRecycleBinPeriodHasPassed() {
    UUID konferenca = UUID.randomUUID();
    UUID drugo = UUID.randomUUID();
    when(groups.findAllIds()).thenReturn(List.of(konferenca, drugo));

    job.removeDueContacts();

    Instant thirtyDaysAgo = NOW.minus(Duration.ofDays(30));
    verify(permanentDelete).permanentlyDeleteDue(konferenca, thirtyDaysAgo);
    verify(permanentDelete).permanentlyDeleteDue(drugo, thirtyDaysAgo);
  }

  @Test
  void br08RemovesTheGroupsWhoseRecycleBinPeriodHasPassed() {
    job.removeDueGroups();

    verify(groups).deleteDue(NOW.minus(Duration.ofDays(30)));
  }

  @Test
  void br08GroupsAreRemovedAtLeastOncePerHour() throws NoSuchMethodException {
    Scheduled schedule =
        RemovalJob.class.getMethod("removeDueGroups").getAnnotation(Scheduled.class);

    assertThat(Duration.parse(schedule.fixedDelayString()))
        .isLessThanOrEqualTo(Duration.ofHours(1));
  }

  @Test
  void r01205RunsAtLeastOncePerHour() throws NoSuchMethodException {
    Scheduled schedule =
        RemovalJob.class.getMethod("removeDueContacts").getAnnotation(Scheduled.class);

    assertThat(Duration.parse(schedule.fixedDelayString()))
        .isLessThanOrEqualTo(Duration.ofHours(1));
    assertThat(ApplicationConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
  }
}
