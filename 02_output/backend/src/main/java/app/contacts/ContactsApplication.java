package app.contacts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Contacts REST API (architecture.md, component backend). */
@SpringBootApplication
public class ContactsApplication {

  /**
   * Starts the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(ContactsApplication.class, args);
  }
}
