package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import org.eclipse.emf.ecore.EClass
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ModelTest {

    @Test
    fun generateSimpleModel() {
        val cu = CompilationUnit(listOf(
                VarDeclaration(Visibility.PUBLIC, "a", StringLiteral("foo")),
                VarDeclaration(Visibility.PRIVATE, "b", StringLiteral("bar"))
        ))
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm")
        metamodelBuilder.addClass(CompilationUnit::class)
        var ePackage = metamodelBuilder.generate()

        val eo = cu.toEObject(ePackage)
        eo.saveXMI(File("simplemodel.xmi"))
        eo.saveAsJson(File("simplem.json"))
    }
}