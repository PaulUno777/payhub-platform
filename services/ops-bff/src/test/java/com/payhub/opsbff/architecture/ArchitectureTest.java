package com.payhub.opsbff.architecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import org.junit.jupiter.api.Tag;

@Tag("architecture")
@AnalyzeClasses(
        packages = "com.payhub.opsbff",
        importOptions = DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchTests domainRules = ArchTests.in(DomainRules.class);

    @ArchTest
    static final ArchTests applicationRules = ArchTests.in(ApplicationRules.class);

    @ArchTest
    static final ArchTests adapterRules = ArchTests.in(AdapterRules.class);

    @ArchTest
    static final ArchTests infrastructureRules = ArchTests.in(InfrastructureRules.class);

    @ArchTest
    static final ArchTests layeredRules = ArchTests.in(LayeredArchitectureRules.class);
}
