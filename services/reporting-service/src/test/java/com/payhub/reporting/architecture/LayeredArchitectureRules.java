package com.payhub.reporting.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class LayeredArchitectureRules {

    @ArchTest
    public static final ArchRule clean_architecture_layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("com.payhub.reporting..")
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapter").definedBy("..adapter..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Adapter").mayOnlyAccessLayers("Application")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application");
}
