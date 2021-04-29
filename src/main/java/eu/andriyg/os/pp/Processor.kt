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
        val dataDir = Paths.get(".").resolve("data").toFile()
        logger.info("Navigating data directory: $dataDir")

        convertPath(dataDir)
    }

    private fun convertPath(path: File) {
        logger.info("Working with {} directory", path)
        if (!path.exists() || !path.isDirectory) {
            logger.error("Path {} is not exist or it is not directory", path)
            return
        }
        path.listFiles { it -> it.isFile }?.forEach { renderFile(file = it) }
        path.listFiles { it -> it.isDirectory }?.forEach { convertPath(it) }
    }

    private val objectMapper by lazy {
        ObjectMapper().enable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
    }

    private val pagesDir by lazy {
        Paths.get(outputPath)
    }

    private fun renderFile(file: File) {
        logger.info("Rendering file {}", file)
        val map = objectMapper.readValue(file, MutableMap::class.java)
        if (!map.containsKey("_page") || map["_page"] !is Map<*, *>) {
            logger.warn("File {} does not contain _page directory", file)
            return
        }

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
