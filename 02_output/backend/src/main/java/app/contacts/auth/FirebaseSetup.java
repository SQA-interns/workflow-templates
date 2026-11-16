package app.contacts.auth;

import app.contacts.config.AppProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.internal.FirebaseProcessEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The Firebase Admin SDK for the configured project (ADR-03, ADR-05, AR-06). */
@Configuration
public class FirebaseSetup {

  private static final String EMULATOR_VARIABLE = "FIREBASE_AUTH_EMULATOR_HOST";

  /**
   * A Firebase app of its own per application context, so that several contexts can run in one JVM
   * (tests); deleted with the context.
   *
   * @param properties the settings
   * @return the Firebase app
   * @throws IOException when the service account file cannot be read
   */
  @Bean(destroyMethod = "delete")
  public FirebaseApp firebaseApp(AppProperties properties) throws IOException {
    AppProperties.Firebase firebase = properties.firebase();
    if (hasText(firebase.authEmulatorHost())) {
      FirebaseProcessEnvironment.setenv(EMULATOR_VARIABLE, firebase.authEmulatorHost());
    }
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setProjectId(firebase.projectId())
            .setCredentials(credentials(firebase.serviceAccountFile()))
            .build();
    return FirebaseApp.initializeApp(options, "contacts-" + UUID.randomUUID());
  }

  /**
   * The ID token verifier.
   *
   * @param app the Firebase app
   * @return the verifier
   */
  @Bean
  public TokenVerifier tokenVerifier(FirebaseApp app) {
    return new FirebaseTokenVerifier(FirebaseAuth.getInstance(app)::verifyIdToken);
  }

  /**
   * The service account if configured (needed only to delete accounts, US-021); token verification
   * needs none.
   */
  private static GoogleCredentials credentials(String serviceAccountFile) throws IOException {
    if (!hasText(serviceAccountFile)) {
      return GoogleCredentials.create(null);
    }
    try (InputStream in = Files.newInputStream(Path.of(serviceAccountFile))) {
      return GoogleCredentials.fromStream(in);
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
