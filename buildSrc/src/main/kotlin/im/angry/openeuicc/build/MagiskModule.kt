package im.angry.openeuicc.build

import com.android.build.api.dsl.ApplicationDefaultConfig
import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.URL
import java.util.SortedMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class MagiskModule : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("assembleMagiskModule") {
            group = "magisk"
            description = "Assembles the Magisk Module"

            val config = target.defaultConfig
            val appFile = target.layout.buildDirectory
                .file("outputs/apk/debug/app-debug.apk")
                .get().asFile

            val options = MagiskModuleOptions(
                appId = config.applicationId!!,
                appFile = appFile,
                permissionFile = target.rootProject.file("privapp_whitelist_${config.applicationId}.xml"),
                versionName = config.versionName!!,
                versionCode = config.versionCode!!,
                manifestUri = ""
            )

            val manifest = MagiskModuleManifest(
                versionName = options.versionName,
                versionCode = options.versionCode,
                releaseUri = "",
                changelogUri = ""
            )

            project.layout.buildDirectory
                .file("outputs/magisk-module.zip")
                .get().asFile
                .writeBytes(buildMagiskModule(options))

            project.layout.buildDirectory
                .file("outputs/magisk-module-manifest.json")
                .get().asFile
                .writeText(buildMagiskModuleManifestFile(manifest))
        }
    }
}

data class MagiskModuleOptions(
    val appId: String,
    val appName: String = "OpenEUICC",
    val appFile: File,
    val permissionFile: File,
    val versionName: String,
    val versionCode: Int,
    val author: String = "Peter Cai",
    val description: String = "OpenEUICC provides system-level eUICC integration",
    val manifestUri: String? = null,
)

data class MagiskModuleManifest(
    val versionName: String,
    val versionCode: Int,
    val releaseUri: String,
    val changelogUri: String,
)

private fun buildMagiskModule(options: MagiskModuleOptions) = buildZipFile {
    // https://topjohnwu.github.io/Magisk/guides.html
    val systemExt = "system/system_ext"
    val metaInfo = "META-INF/com/google/android"
    val apkPath = "$systemExt/priv-app/${options.appName}/${options.appName}.apk"
    put("module.prop") {
        val module = buildList {
            add("id" to options.appId)
            add("name" to options.appName)
            add("version" to options.versionName)
            add("versionCode" to options.versionCode)
            add("author" to options.author)
            add("description" to options.description)
            if (options.manifestUri != null) add("updateJson" to options.manifestUri)
        }
        module
            .joinToString("\n") { (key, value) -> "${key}=${value}" }
            .encodeToByteArray()
    }
    put("customize.sh") {
        val copiedApkPath = "\$TMPDIR/${options.appName}.apk"
        val script = buildString {
            appendLine("chmod u+x \"\$MODPATH/uninstall.sh\"")
            appendLine()
            appendLine("cp \"\$MODPATH/$apkPath\" \"$copiedApkPath\"")
            appendLine("pm install -r \"$copiedApkPath\"")
            appendLine("rm -f \"$copiedApkPath\"")
            appendLine()
            appendLine("pm grant \"${options.appId}\" android.permission.READ_PHONE_STATE")
        }
        script.encodeToByteArray()
    }
    put("uninstall.sh") {
        "pm uninstall ${options.appId}\n".encodeToByteArray()
    }
    put("$metaInfo/update-binary") {
        val installerUri =
            URI("https://github.com/topjohnwu/Magisk/raw/bf4ed29/scripts/module_installer.sh")
        val connection = URL.of(installerUri, null).openConnection()
        connection.inputStream.readBytes()
    }
    put("$metaInfo/updater-script") {
        "#MAGISK\n".encodeToByteArray()
    }
    put(apkPath) {
        options.appFile.readBytes()
    }
    put("$systemExt/etc/permissions/privapp_whitelist_${options.appId}.xml") {
        options.permissionFile.readBytes()
    }
}

private fun buildMagiskModuleManifestFile(manifest: MagiskModuleManifest): String {
    val entries = buildMap {
        put("version", manifest.versionName)
        put("versionCode", manifest.versionCode)
        put("zipUrl", manifest.releaseUri)
        put("changelog", manifest.changelogUri)
    }
    val jsonPayload = JsonOutput.toJson(entries)
    return JsonOutput.prettyPrint(jsonPayload)
}

private fun buildZipFile(builderAction: SortedMap<String, () -> ByteArray>.() -> Unit): ByteArray {
    val out = ByteArrayOutputStream()
    val zip = ZipOutputStream(out)
    val entries = buildMap { builderAction(this.toSortedMap()) }
    for ((name, generate) in entries) {
        val entry = ZipEntry(name)
        entry.time = 0 // reproducible builds
        zip.putNextEntry(entry)
        zip.write(generate())
        zip.closeEntry()
    }
    zip.close()
    out.close()
    return out.toByteArray()
}

private val Project.defaultConfig: ApplicationDefaultConfig
    get() = when (val android = extensions.findByName("android")) {
        is com.android.build.gradle.AppExtension -> android.defaultConfig
        is com.android.build.api.dsl.ApplicationExtension -> android.defaultConfig
        else -> throw Exception("Unavailable Android configuration")
    }
