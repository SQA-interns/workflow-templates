package app.contacts;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

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
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

/** Architecture constraints a tool can check (architecture.md AR-03, AR-04, AR-06, AR-09). */
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

  private static com.tngtech.archunit.base.DescribedPredicate<
          com.tngtech.archunit.core.domain.JavaClass>
      notExempt() {
    return com.tngtech.archunit.base.DescribedPredicate.describe(
        "not exempt from AR-04", c -> !NOT_GROUP_OWNED.contains(c.getSimpleName()));
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
