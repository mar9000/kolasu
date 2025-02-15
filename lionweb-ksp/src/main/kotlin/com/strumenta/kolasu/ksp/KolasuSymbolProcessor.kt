package com.strumenta.kolasu.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.strumenta.kolasu.model.Node
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.IllegalStateException

class KolasuSymbolProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    var generated : Boolean = false

    class LanguageDef(val packageName: String) {
        val classes = mutableListOf<KSClassDeclaration>()

        fun dependencies() : Dependencies {
            val usedFiles = this.classes.mapNotNull { it.containingFile }.toSet().toTypedArray()
            val dependencies = Dependencies(true, *usedFiles)
            return dependencies
        }

        fun write(os: OutputStream) {
            val buf = PrintWriter(os)
            buf.println("""
                    package $packageName
                    
                    import com.strumenta.kolasu.lionweb.LionWebLanguageGeneratorCommand
                    import com.strumenta.kolasu.lionweb.KolasuLanguage
                    import com.strumenta.kolasu.lionweb.LionWebLanguageExporter
                    import io.lionweb.lioncore.java.language.Language

                    val kLanguage = KolasuLanguage("$packageName").apply { 
                        ${classes.joinToString("\n        ") { "addClass(${it.simpleName.asString()}::class)" }}
                    }
                    
                    val lwLanguage : Language by lazy {
                        val importer = LionWebLanguageExporter()
                        importer.export(kLanguage)
                    }

                    fun main(args: Array<String>) {
                        LionWebLanguageGeneratorCommand(lwLanguage).main(args)
                    }                    
            """.trimIndent())
            buf.flush()
        }
    }
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val kspFile = File(environment.options["file"] as String)
        if (!kspFile.exists()) {
            throw IllegalStateException("The KSP configuration file at $kspFile does not exist")
        }
        val exportPackagesStr = kspFile.readText().trim().split("=")[1]


        if (exportPackagesStr.isNullOrBlank()) {
            // No packages to export, we are done here
            return emptyList()
        }
        val exportPackages = exportPackagesStr.split(",").map { it.trim() }

        val languagesByPackage = mutableMapOf<String, LanguageDef>()
        resolver.getAllFiles().filter { it.packageName.asString() in exportPackages }.forEach { ksFile ->
            val packageName = ksFile.packageName.asString()
            ksFile.declarations.forEach { ksDeclaration ->
                if (ksDeclaration is KSClassDeclaration) {
                    val isNodeDecl = ksDeclaration.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == Node::class.qualifiedName}
                    if (isNodeDecl) {
                        languagesByPackage.computeIfAbsent(packageName) {
                            LanguageDef(packageName)
                        }.classes.add(ksDeclaration)
                    }
                }
            }
        }

        if (!generated) {
            languagesByPackage.values.forEach { languageDef ->
                languageDef.write(environment.codeGenerator.createNewFile(languageDef.dependencies(),
                    languageDef.packageName, "Language", "kt"))
            }

            generated = true
        }
        return emptyList()
    }
}