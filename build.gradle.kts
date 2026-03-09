import java.time.Duration

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.openapi.generator)
	alias(libs.plugins.docker.compose)
	id("jacoco")
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

val itest by sourceSets.creating {
	kotlin.srcDirs("src/itest/kotlin")
	compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
	runtimeClasspath += output + compileClasspath
}

configurations {
	getByName(itest.implementationConfigurationName).extendsFrom(testImplementation.get())
	getByName(itest.runtimeOnlyConfigurationName).extendsFrom(testRuntimeOnly.get())
	create("pitest")
}

dependencies {
	implementation(libs.spring.boot.web)
	implementation(libs.spring.boot.data.jpa)
	implementation(libs.spring.boot.validation)
	implementation(libs.kotlin.reflect)
	implementation(libs.jackson.kotlin)
	implementation(libs.temporal.sdk)
	implementation(libs.springdoc)
	implementation(libs.jakarta.annotation)
	runtimeOnly(libs.postgresql)

	testImplementation(libs.spring.boot.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.mockito.kotlin)
	testImplementation(libs.archunit)
	testImplementation(libs.jqwik)
	testImplementation(libs.temporal.testing)
	testRuntimeOnly(libs.junit.platform.launcher)

	add(itest.implementationConfigurationName, libs.rest.assured)
	add(itest.implementationConfigurationName, libs.awaitility.kotlin)

	"pitest"(libs.pitest.core)
	"pitest"(libs.pitest.junit5)
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
	apiPackage.set("com.bidding.auction.generated.api")
	modelPackage.set("com.bidding.auction.generated.api.model")
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

sourceSets.main.get().kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin").get().asFile)

tasks.compileKotlin { dependsOn(tasks.openApiGenerate) }

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Test>("itest") {
	description = "Runs integration tests"
	group = "verification"
	testClassesDirs = itest.output.classesDirs
	classpath = itest.runtimeClasspath
	useJUnitPlatform()
	shouldRunAfter(tasks.named("test"))
}

dockerCompose {
	dockerExecutable.set("/usr/local/bin/docker")
	buildBeforeUp.set(false)
	useComposeFiles.set(listOf("docker-compose.yml"))
	waitForHealthyStateTimeout.set(Duration.ofMinutes(2))
	isRequiredBy(tasks.named("itest"))
}


tasks.named("check") {
	dependsOn(tasks.named("itest"))
	dependsOn(tasks.named("pitest"))
}

tasks.register<JavaExec>("pitest") {
	description = "Runs PITest mutation testing on BidDomainService"
	group = "verification"
	dependsOn(tasks.named("testClasses"))

	mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
	classpath = configurations["pitest"]

	doFirst {
		val reportDir = layout.buildDirectory.dir("reports/pitest").get().asFile
		reportDir.mkdirs()

		val classPathStr = project.files(
			sourceSets.main.get().output.classesDirs,
			sourceSets.test.get().output.classesDirs,
			sourceSets.test.get().output.resourcesDir,
			configurations.testRuntimeClasspath
		).files.joinToString(",") { it.absolutePath }

		setArgs(listOf(
			"--reportDir",          reportDir.absolutePath,
			"--targetClasses",      "com.bidding.auction.domain.service.BidDomainService",
			"--targetTests",        "com.bidding.auction.domain.service.BidDomainServiceMutationTest",
			"--sourceDirs",         file("src/main/kotlin").absolutePath,
			"--classPath",          classPathStr,
			"--outputFormats",      "HTML",
			"--timestampedReports", "false",
			"--mutators",           "STRONGER",
			"--threads",            "4"
		))
	}
}

jacoco {
	toolVersion = libs.versions.jacoco.get()
}

tasks.named<JacocoReport>("jacocoTestReport") {
	dependsOn(tasks.named("test"), tasks.named("itest"))
	executionData(
		layout.buildDirectory.file("jacoco/test.exec"),
		layout.buildDirectory.file("jacoco/itest.exec")
	)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
	classDirectories.setFrom(
		sourceSets.main.get().output.classesDirs.map { dir ->
			fileTree(dir) {
				exclude("**/AuctionApplication*", "com/bidding/auction/api/generated/**")
			}
		}
	)
}
