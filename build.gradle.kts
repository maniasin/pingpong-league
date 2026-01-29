plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.maniasin"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// Redis 캐싱 및 Rate Limiting 지원
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	implementation("com.bucket4j:bucket4j-core:8.10.1")
	implementation("com.bucket4j:bucket4j-redis:8.10.1")
	implementation("org.jsoup:jsoup:1.17.2")
	// 2. Selenium의 버전을 자동으로 관리해주는 BOM(Bill of Materials)을 추가합니다.
	implementation(platform("org.seleniumhq.selenium:selenium-bom:4.22.0"))
	// 3. 이제 버전 없이 selenium-java만 추가합니다.
	implementation("org.seleniumhq.selenium:selenium-java")
	implementation("io.github.bonigarcia:webdrivermanager:5.8.0")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
