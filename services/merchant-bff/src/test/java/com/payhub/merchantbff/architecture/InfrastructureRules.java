package com.payhub.merchantbff.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class InfrastructureRules {

    @ArchTest
    public static final ArchRule domain_must_never_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .allowEmptyShould(true)
            .as("Domain should never depend on the Infrastructure layer");

    @ArchTest
    public static final ArchRule infrastructure_should_not_depend_on_adapter = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..")
            .allowEmptyShould(true)
            .as("Infrastructure should not depend on the Adapter layer");
}
