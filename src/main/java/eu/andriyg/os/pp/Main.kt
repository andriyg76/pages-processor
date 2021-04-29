package eu.andriyg.os.pp

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

object Main {
    private val options: Options by lazy {
        // create Options object
        val options = Options()

        options.addOption(null, "output-path", true, "directory where pages will be written, by default [pages]")
        options.addOption("h", "help", false, "Display help")
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun printUsage(): Nothing {
        HelpFormatter().printHelp("", options)
        exitProcess(0)
    }

    @JvmStatic
    fun main(vararg args: String) {
        logger.debug("Hello from $this")
        val params = parseArgs(args)
        logger.debug("Parsed arguments: {}", params)
        Processor(outputPath = params.outputPath).process()
    }

    private fun parseArgs(args: Array<out String>): Params {
        logger.debug("passed args: {}", args)
        val params = DefaultParser().parse(options, args)

        if (params.hasOption("h") || params.hasOption("help")) {
            printUsage()
        }
        var outputPath = params.getOptionValue("output-path") ?: null
        if (outputPath.isNullOrBlank()) {
            outputPath = "pages"
        }

        val v = Params(
            outputPath = outputPath!!,
        )
        return v
    }
}