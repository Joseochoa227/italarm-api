package co.italarm.api.arquitectura;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/** Reglas de la sección 10 verificadas en cada compilación. */
@AnalyzeClasses(packages = "co.italarm.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

  private static final String RAIZ = "co.italarm.api.";
  private static final Set<String> CAPAS_INTERNAS = Set.of("dominio", "infraestructura");

  /** BP-01: controladores delgados, sin acceso directo a repositorios ni infraestructura. */
  @ArchTest
  static final ArchRule CONTROLADORES_SIN_INFRAESTRUCTURA =
      noClasses()
          .that()
          .resideInAPackage("co.italarm.api.*.api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("co.italarm.api.*.infraestructura..");

  /** BP-01: los controladores viven en la capa api. */
  @ArchTest
  static final ArchRule CONTROLADORES_EN_API =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .resideInAPackage("co.italarm.api.*.api..");

  /**
   * BP-05: el dominio no depende de Spring. Solo se admiten las anotaciones de auditoría de Spring
   * Data que usan las entidades (BP-12).
   */
  @ArchTest
  static final ArchRule DOMINIO_SIN_SPRING =
      noClasses()
          .that()
          .resideInAPackage("..dominio..")
          .should()
          .dependOnClassesThat(
              resideInAPackage("org.springframework..")
                  .and(
                      not(
                          resideInAnyPackage(
                              "org.springframework.data.annotation..",
                              "org.springframework.data.jpa.domain.support.."))));

  /** BP-08: las transacciones solo se abren en la capa de aplicación. */
  @ArchTest
  static final ArchRule CLASES_TRANSACCIONALES_EN_APLICACION =
      noClasses()
          .that()
          .resideOutsideOfPackage("..aplicacion..")
          .should()
          .beAnnotatedWith(Transactional.class);

  @ArchTest
  static final ArchRule METODOS_TRANSACCIONALES_EN_APLICACION =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .resideOutsideOfPackage("..aplicacion..")
          .should()
          .beAnnotatedWith(Transactional.class);

  @ArchTest
  static final ArchRule TRANSACCIONES_PUBLICAS =
      methods().that().areAnnotatedWith(Transactional.class).should().bePublic();

  /** BP-03: inyección por constructor. */
  @ArchTest
  static final ArchRule SIN_INYECCION_EN_CAMPOS =
      GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

  @ArchTest
  static final ArchRule SIN_SALIDA_ESTANDAR =
      GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

  @ArchTest
  static final ArchRule SIN_LOGGING_DE_JAVA =
      GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

  /**
   * Sección 10.1: cada módulo solo expone sus servicios (aplicacion) y DTO (api) a los demás. El
   * módulo shared es común a todos.
   */
  @ArchTest
  static final ArchRule MODULOS_SOLO_POR_SU_FACHADA =
      classes().that().resideInAPackage("co.italarm.api..").should(noUsarInternosDeOtroModulo());

  private static ArchCondition<JavaClass> noUsarInternosDeOtroModulo() {
    return new ArchCondition<>("no usar dominio ni infraestructura de otro módulo") {
      @Override
      public void check(JavaClass origen, ConditionEvents eventos) {
        String moduloOrigen = modulo(origen);
        for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
          JavaClass destino = dependencia.getTargetClass();
          String moduloDestino = modulo(destino);
          if (moduloDestino == null
              || moduloDestino.equals("shared")
              || moduloDestino.equals(moduloOrigen)) {
            continue;
          }
          if (CAPAS_INTERNAS.contains(capa(destino))) {
            eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
          }
        }
      }
    };
  }

  private static String modulo(JavaClass clase) {
    String paquete = clase.getPackageName();
    if (!paquete.startsWith(RAIZ)) {
      return null;
    }
    String resto = paquete.substring(RAIZ.length());
    int punto = resto.indexOf('.');
    return punto < 0 ? resto : resto.substring(0, punto);
  }

  private static String capa(JavaClass clase) {
    String[] partes = clase.getPackageName().split("\\.");
    return partes.length > 4 ? partes[4] : "";
  }
}
