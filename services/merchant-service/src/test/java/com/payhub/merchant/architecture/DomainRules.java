package com.payhub.merchant.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class DomainRules {

    @ArchTest
    public static final ArchRule domain_should_be_framework_free = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .allowEmptyShould(true)
            .as("Domain should not depend on any Spring component");

    @ArchTest
    public static final ArchRule domain_should_not_depend_on_persistence = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.persistence..")
            .orShould().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..")
            .orShould().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data..")
            .orShould().dependOnClassesThat().resideInAnyPackage(
                    "org.hibernate..")
            .allowEmptyShould(true)
            .as("Domain should not depend on any persistence annotations or classes (JPA/Hibernate)");

    @ArchTest
    public static final ArchRule domain_should_not_depend_on_messaging_or_workflow = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.apache.kafka..",
                    "org.springframework.kafka..",
                    "io.temporal..",
                    "org.springframework.data.redis..",
                    "redis.clients..",
                    "io.micrometer..",
                    "io.opentelemetry..")
            .allowEmptyShould(true)
            .as("Domain should not depend on Kafka, Temporal, Redis, or Micrometer/OpenTelemetry");

    @ArchTest
    public static final ArchRule domain_should_not_depend_on_other_layers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..")
            .orShould().dependOnClassesThat().resideInAnyPackage("..application..")
            .orShould().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .allowEmptyShould(true)
            .as("Domain should not depend on any other layer");
}
