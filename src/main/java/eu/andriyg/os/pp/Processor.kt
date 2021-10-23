package eu.andriyg.os.pp

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists

class Processor(var outputPath: String) {

    private val handlebars: Handlebars by lazy {
        Handlebars(
            FileTemplateLoader(
                Paths.get(".").resolve(".processor/templates").toFile()
            )
        )
    }

    private val templates = LinkedHashMap<String, Template>()
    private fun getTemplate(template: String): Template {
        return templates.computeIfAbsent(template) { handlebars.compile(it) }
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun process() {
        val sharedPath = Paths.get(".").resolve("shared")
        val shared: Map<String, Any>
        if (!sharedPath.exists()) {
            logger.info("Shared path is not found, ignoring loading")
            shared = emptyMap()
        } else {
            shared = loadShared(sharedPath.toFile())
        }
        val dataDir = Paths.get(".").resolve("data").toFile()
        logger.info("Navigating data directory: $dataDir")

        convertPath(dataDir, shared)
    }

    private fun loadShared(path: File): Map<String, Any> {
        logger.info("Loading shared from {}", path)

        var shared = emptyMap<String, Any>()
        path.listFiles { it -> it.isFile }?.forEach { shared += prefix(it.name) to loadJson(it) }
        path.listFiles{ it -> it.isDirectory }?.forEach { shared += it.name to loadShared(it) }
        return shared
    }

    private fun prefix(name: String): String {
        return name.split('.')[0]
    }

    private fun loadJson(file: File): Any {
        return objectMapper.readValue(file, MutableMap::class.java)
    }

    private fun convertPath(path: File, shared: Map<String, Any>) {
        logger.info("Working with {} directory", path)
        if (!path.exists() || !path.isDirectory) {
            logger.error("Path {} is not exist or it is not directory", path)
            return
        }
        path.listFiles { it -> it.isFile }?.forEach { renderFile(file = it, shared = shared) }
        path.listFiles { it -> it.isDirectory }?.forEach { convertPath(it, shared) }
    }

    private val objectMapper by lazy {
        ObjectMapper().enable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
    }

    private val pagesDir by lazy {
        Paths.get(outputPath)
    }

    private fun renderFile(file: File, shared: Map<String, Any>) {
        logger.info("Rendering file {}", file)
        val map = objectMapper.readValue(file, MutableMap::class.java) as MutableMap<String, Any>
        if (!map.containsKey("_page") || map["_page"] !is Map<*, *>) {
            logger.warn("File {} does not contain _page directory", file)
            return
        }

        map["_shared"] = shared
        val _page = map["_page"] as Map<*, *>
        val template = _page["template"].toString()
        var output: String = _page["output"]?.toString() ?: run {
            file.name
            logger.warn("File {} output is not defined")
            return
        }
        if (output.startsWith("/")) {
            output = output.substring(1)
        }

        var path = pagesDir.resolve(output)
        Files.createDirectories(path.parent)
        FileWriter(path.toFile()).use {
            logger.info("Rendering {} with template {} to {}", file, template, path)
            getTemplate(template).apply(Context.newContext(map), it)
        }
    }
}
