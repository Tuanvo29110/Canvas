plugins {
    `java-library`
    idea
}

dependencies {
    implementation(project(":canvas-server"))
    implementation(project(":canvas-api"))
}

tasks.register<Copy>("buildAndCopyPlugin") {
    dependsOn(tasks.named("build"))

    from(layout.buildDirectory.dir("libs"))
    include("canvas-test-plugin-*.jar")
    into(file("../local/plugins/"))
}
