package com.bidding.auction.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture

private const val HANDWRITTEN_API_PACKAGE = "com.bidding.auction.api"
private const val GENERATED_API_PACKAGE = "com.bidding.auction.generated.api"

@AnalyzeClasses(packages = ["com.bidding.auction"])
class ArchitectureRulesTest {

    companion object {

        // The OpenAPI generator annotates generated *Api interfaces with @RestController,
        // so we scope this rule to the handwritten com.bidding.auction.api package only.
        @JvmField
        @ArchTest
        val controllers_implement_generated_api: ArchRule =
            classes()
                .that().resideInAPackage("$HANDWRITTEN_API_PACKAGE..")
                .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController::class.java)
                .should(implementAtLeastOneInterfaceFromPackage(GENERATED_API_PACKAGE))

        @JvmField
        @ArchTest
        val domain_services_have_no_spring_stereotypes: ArchRule =
            noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.stereotype..")

        @JvmField
        @ArchTest
        val domain_services_have_no_spring_transactions: ArchRule =
            noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework.transaction..")

        // Generated code lives in com.bidding.auction.generated.* and is excluded from
        // layer checks — it acts as shared DTOs accessible from any layer.
        @JvmField
        @ArchTest
        val layer_dependencies_are_respected: ArchRule =
            layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("com.bidding.auction..")
                .ignoreDependency(
                    resideInAPackage("com.bidding.auction.."),
                    resideInAPackage("$GENERATED_API_PACKAGE..")
                )
                .layer("api").definedBy("$HANDWRITTEN_API_PACKAGE..")
                .layer("application").definedBy("..application..")
                .layer("domain").definedBy("..domain..")
                .layer("config").definedBy("..config..")
                .whereLayer("api").mayNotBeAccessedByAnyLayer()
                .whereLayer("application").mayOnlyBeAccessedByLayers("api", "config")
                .whereLayer("domain").mayOnlyBeAccessedByLayers("api", "application", "config")

        private fun implementAtLeastOneInterfaceFromPackage(pkg: String): ArchCondition<JavaClass> =
            object : ArchCondition<JavaClass>("implement at least one interface from package $pkg") {
                override fun check(item: JavaClass, events: ConditionEvents) {
                    val implementsOne = item.interfaces.any { it.toErasure().packageName.startsWith(pkg) }
                    if (!implementsOne) {
                        events.add(
                            SimpleConditionEvent.violated(
                                item,
                                "${item.simpleName} does not implement any API interface from $pkg"
                            )
                        )
                    }
                }
            }
    }
}
