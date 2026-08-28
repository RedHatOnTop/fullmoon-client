plugins {
    id("fabric-loom") version "1.17.17"
    `java-library`
}

fun prop(name: String) = project.property(name) as String

val minecraftVersion = prop("minecraft_version")
val loaderVersion = prop("loader_version")
val fabricVersion = prop("fabric_version")
val modVersion = prop("mod_version")
val mavenGroup = prop("maven_group")
val archivesBaseName = prop("archives_base_name")

base { archivesName = archivesBaseName }
version = modVersion
group = mavenGroup

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

// i3 reaches the GUI submission point through an access widener rather than a mixin.
// i2 died on `@Inject … 'render' in Gui`; that method does not exist on 26.1.x, and a
// widened field cannot silently miss its target the way a descriptor-matched hook can.
loom {
    accessWidenerPath = file("src/main/resources/fullmoon.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-Werror"))
}

tasks.test { useJUnitPlatform() }

tasks.processResources {
    val props = mapOf(
        "version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion,
        "fabric_version" to fabricVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

tasks.jar {
    from("LICENSE") { rename { "${it}_$archivesBaseName" } }
}
