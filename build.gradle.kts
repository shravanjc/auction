plugins {
	kotlin("jvm") version "2.3.10"
	kotlin("plugin.spring") version "2.3.10"
	kotlin("plugin.jpa") version "2.3.10"
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.10.0"
}

group = "com.bidding"
version = "0.0.1-SNAPSHOT"
description = "Demo project for using Temporal for closing and extending auctions in a distributed, fault tolerant, retry-able manner"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.temporal:temporal-sdk:1.27.0")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
	implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

openApiGenerate {
	generatorName.set("kotlin-spring")
	inputSpec.set("$rootDir/src/main/resources/openapi.yaml")
	outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.path)
	apiPackage.set("com.bidding.auction.api.generated")
	modelPackage.set("com.bidding.auction.api.generated.model")
	generateApiTests.set(false)
	generateModelTests.set(false)
	configOptions.set(mapOf(
		"interfaceOnly" to "true",
		"useSpringBoot3" to "true",
		"useTags" to "true",
		"dateLibrary" to "java8",
		"useBeanValidation" to "false",
		"swaggerAnnotations" to "false"
	))
}

sourceSets.main { kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin")) }
tasks.compileKotlin { dependsOn(tasks.openApiGenerate) }

tasks.withType<Test> {
	useJUnitPlatform()
}
