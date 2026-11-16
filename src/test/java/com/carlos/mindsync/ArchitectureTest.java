package com.carlos.mindsync;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Guards the hexagonal "golden rule": dependencies point inwards.
 * The rules are allow-lists, so any new framework import in the core fails the build.
 */
@AnalyzeClasses(packages = "com.carlos.mindsync", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.carlos.mindsync.domain..";
    private static final String APPLICATION = "com.carlos.mindsync.application..";

    @ArchTest
    static final ArchRule domain_depends_only_on_itself_and_the_jdk =
            classes().that().resideInAPackage(DOMAIN)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..");

    @ArchTest
    static final ArchRule application_depends_only_on_domain_itself_and_the_jdk =
            classes().that().resideInAPackage(APPLICATION)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..");
}
