package app.contacts;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import app.contacts.domain.AuditEntry;
import app.contacts.repository.AuditEntryRepository;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Architecture constraints a tool can check (architecture.md AR-03, AR-04, AR-06, AR-09) and the
 * append-only Audit entries (SR-04, specification §6.2).
 */
@AnalyzeClasses(packages = "app.contacts", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  /** Repositories of data that is not owned by one Group (ADR-02). */
  private static final Set<String> NOT_GROUP_OWNED =
      Set.of("UserRepository", "GroupRepository", "MembershipRepository");

  @ArchTest
  static final ArchRule ar03Layers =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .withOptionalLayers(true)
          .layer("Web")
          .definedBy("app.contacts.web..")
          .layer("Auth")
          .definedBy("app.contacts.auth..")
          .layer("Service")
          .definedBy("app.contacts.service..")
          .layer("Repository")
          .definedBy("app.contacts.repository..")
          .layer("Domain")
          .definedBy("app.contacts.domain..")
          .whereLayer("Web")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Service")
          .mayOnlyBeAccessedByLayers("Web", "Auth")
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Service")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Service", "Repository");

  @ArchTest
  static final ArchRule ar04GroupOwnedRepositoriesHaveNoGenericFinders =
      classes()
          .that()
          .resideInAPackage("app.contacts.repository..")
          .and()
          .areInterfaces()
          .and(notExempt())
          .should()
          .notBeAssignableTo(CrudRepository.class)
          .allowEmptyShould(true)
          .because("generic finders read Group data without a Group id (AR-04)");

  @ArchTest
  static final ArchRule ar04GroupOwnedRepositoryMethodsTakeGroupId =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("app.contacts.repository..")
          .and()
          .areDeclaredInClassesThat(notExempt())
          .should(takeGroupId())
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule ar06OnlyAuthUsesFirebase =
      noClasses()
          .that()
          .resideOutsideOfPackage("app.contacts.auth..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.google.firebase..");

  @ArchTest
  static final ArchRule ar09OnlyServiceBuildsWorkbooks =
      noClasses()
          .that()
          .resideOutsideOfPackage("app.contacts.service..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.apache.poi..");

  /** SR-04: Audit entries are only added; the repository declares no method of its own. */
  @ArchTest
  static final ArchRule sr04AuditEntryRepositoryOnlyAdds =
      noMethods()
          .that()
          .areDeclaredIn(AuditEntryRepository.class)
          .should()
          .bePublic()
          .allowEmptyShould(true)
          .because("Audit entries have no update or delete path (SR-04)");

  /** SR-04: no query changes or deletes Audit entries; only the Group erasure cascades (D-19). */
  @ArchTest
  static final ArchRule sr04NoQueryChangesAuditEntries =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("app.contacts.repository..")
          .and()
          .areAnnotatedWith(Query.class)
          .should(notChangeAuditEntries())
          .allowEmptyShould(true);

  /** SR-04: an Audit entry cannot be changed once created. */
  @ArchTest
  static final ArchRule sr04AuditEntryHasNoSetters =
      noMethods()
          .that()
          .areDeclaredIn(AuditEntry.class)
          .should()
          .haveNameStartingWith("set")
          .because("Audit entries are never changed (SR-04)");

  private static com.tngtech.archunit.base.DescribedPredicate<
          com.tngtech.archunit.core.domain.JavaClass>
      notExempt() {
    return com.tngtech.archunit.base.DescribedPredicate.describe(
        "not exempt from AR-04", c -> !NOT_GROUP_OWNED.contains(c.getSimpleName()));
  }

  private static ArchCondition<JavaMethod> notChangeAuditEntries() {
    return new ArchCondition<>("not update or delete Audit entries (SR-04)") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        String query = method.getAnnotationOfType(Query.class).value().toLowerCase(Locale.ROOT);
        boolean changes = query.startsWith("update") || query.startsWith("delete");
        boolean audit = query.contains("auditentry") || query.contains("audit_entry");
        events.add(
            new SimpleConditionEvent(
                method, !(changes && audit), method.getFullName() + " changes Audit entries"));
      }
    };
  }

  private static ArchCondition<JavaMethod> takeGroupId() {
    return new ArchCondition<>("take a UUID parameter named groupId (AR-04)") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        Parameter[] parameters = method.reflect().getParameters();
        boolean ok =
            Arrays.stream(parameters)
                .anyMatch(p -> p.getType().equals(UUID.class) && p.getName().equals("groupId"));
        events.add(new SimpleConditionEvent(method, ok, method.getFullName() + " lacks groupId"));
      }
    };
  }
}
