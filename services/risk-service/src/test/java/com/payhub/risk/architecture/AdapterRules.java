package com.payhub.risk.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class AdapterRules {

    @ArchTest
    public static final ArchRule adapter_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .allowEmptyShould(true)
            .as("Adapter should not depend on the Infrastructure layer");

    @ArchTest
    public static final ArchRule adapter_should_not_depend_on_domain = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..")
            .allowEmptyShould(true)
            .as("Adapter should not depend on the Domain layer");
}
