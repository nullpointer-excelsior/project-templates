package tasks;

import java.io.File;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

abstract class CreateSpringApp : DefaultTask() {

    private var appNameOption: String? = null

    @Option(option = "appName", description = "Configures the app name")
    fun setAppNameOption(appNameOption: String) {
        this.appNameOption = appNameOption
    }

    @Input
    fun getAppNameOption(): String? {
        return this.appNameOption
    }

    @Internal
    fun getSanitizedClassName(): String {
        val input = this.appNameOption.toString()
        val sb = StringBuilder()
        var capitalizeNext = false

        for (char in input) {
            if (char == '-') {
                capitalizeNext = true
            } else {
                sb.append(if (capitalizeNext) char.uppercaseChar() else char)
                capitalizeNext = false
            }
        }

        val sanitizedClassName = sb.toString()

        return sanitizedClassName.substring(0, 1).uppercase() +
                sanitizedClassName.substring(1) + "Application"
    }

    @Internal
    fun getSanitizedPackagePath(): String {
        val appPackage = this.appNameOption.toString().replace("-", "_")
        val groupPackage = project.group.toString().replace(".", "/")
        return "$groupPackage/$appPackage"
    }

    @TaskAction
    fun create() {
        println("Creating app: $appNameOption with group-id:${project.group}")

        val mainClassTemplate = this.readFile("buildSrc/src/main/resources/main.java.template")
        val buildGradleTemplate = this.readFile("buildSrc/src/main/resources/build.gradle.kts.template")
        val applicationYamlTemplate = this.readFile("buildSrc/src/main/resources/application.yaml.template")

        val packageName = project.group.toString()
        val packagePath = this.getSanitizedPackagePath()
        val className = this.getSanitizedClassName()
        val appPackage = appNameOption.toString().replace("-", "_")

        val mainClassContent = mainClassTemplate
            .replace("#{APP_NAME}", appPackage)
            .replace("#{PACKAGE_NAME}", packageName)
            .replace("#{CLASS_NAME}", className)

        val buildGradleContent = buildGradleTemplate
            .replace("#{PACKAGE_NAME}", packageName)

        val applicationYamlContent = applicationYamlTemplate
            .replace("#{APP_NAME}", appPackage)

        this.exec("mkdir -p apps/$appNameOption/src/main/java/$packagePath")
        this.exec("mkdir -p apps/$appNameOption/src/main/resources")
        this.exec("mkdir -p apps/$appNameOption/src/test/java/$packagePath")

        this.writeFile("apps/$appNameOption/src/main/java/$packagePath/${className}.java", mainClassContent)
        this.writeFile("apps/$appNameOption/build.gradle.kts", buildGradleContent)
        this.writeFile("apps/$appNameOption/src/main/resources/application.yaml", applicationYamlContent)
    }

    private fun exec(command: String) {
        Runtime.getRuntime().exec(command)
    }

    private fun readFile(fileName: String): String {
        return File(fileName).readText(Charsets.UTF_8)
    }

    private fun writeFile(fileName: String, content: String) {
        File(fileName).writeText(content, Charsets.UTF_8)
    }

}