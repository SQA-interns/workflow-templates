package app.contacts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** AR-07: the schema changes only through Flyway migrations, with every timestamp in UTC. */
class MigrationConventionsTest {

  private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
  private static final Pattern TIMESTAMP_WITHOUT_TZ =
      Pattern.compile(
          "\\btimestamp\\b(?!tz)(?!\\s+with\\s+time\\s+zone)", Pattern.CASE_INSENSITIVE);

  @Test
  void ar07MigrationsAreVersionedFlywayFiles() throws IOException {
    try (Stream<Path> files = Files.list(MIGRATIONS)) {
      assertThat(files.map(p -> p.getFileName().toString()))
          .isNotEmpty()
          .allMatch(name -> name.matches("V\\d+__[a-z0-9_]+\\.sql"));
    }
  }

  @Test
  void ar07EveryTimestampIsTimestamptz() throws IOException {
    try (Stream<Path> files = Files.list(MIGRATIONS)) {
      List<String> offending =
          files
              .flatMap(MigrationConventionsTest::lines)
              .filter(line -> !line.trim().startsWith("--"))
              .filter(line -> TIMESTAMP_WITHOUT_TZ.matcher(line).find())
              .toList();
      assertThat(offending).isEmpty();
    }
  }

  private static Stream<String> lines(Path file) {
    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8).stream();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
