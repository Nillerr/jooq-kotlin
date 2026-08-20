package io.github.nillerr.jooq

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class RowGeneratorProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RowGeneratorProcessor(environment)
    }
}

class RowGeneratorProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getDeclarationsFromPackage("org.jooq.generated.tables.records")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { it.containingFile?.let { file -> file to it } }
            .groupBy({ (file, _) -> file }, { (_, type) -> type })
            .forEach { (srcFile, srcTypes) ->
                val fileBuilder = FileSpec.builder(srcFile.packageName.asString(), "${srcFile.fileName}Rows")

                srcTypes.forEach { srcType ->
                    val recordType = srcType.toClassName()

                    val properties = srcType.getDeclaredProperties().toList()

                    val typeBuilder = TypeSpec.classBuilder("${srcType.simpleName}Row")
                    typeBuilder.primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                ParameterSpec.builder("record", recordType, KModifier.PRIVATE)
                                    .build()
                            )
                            .build()
                    )
                    typeBuilder.addProperty(
                        PropertySpec.builder("_record", recordType, KModifier.PRIVATE)
                            .build()
                    )
                    typeBuilder.addInitializerBlock(
                        CodeBlock.of("this._record = record")
                    )
                    // Add Properties
                    properties.forEach { property ->
                        val propertyType = property.type.toTypeName()
                        typeBuilder.addProperty(
                            PropertySpec.builder(property.simpleName.asString(), propertyType)
                                .getter(
                                    FunSpec.getterBuilder()
                                        .addCode(CodeBlock.of("return this._record.${property.simpleName.asString()}"))
                                        .build()
                                )
                                .setter(
                                    FunSpec.setterBuilder()
                                        .addParameter("value", propertyType)
                                        .addCode(CodeBlock.of("this._record.${property.simpleName.asString()} = value"))
                                        .build()
                                )
                                .build()
                        )
                    }
                    typeBuilder.addFunction(
                        FunSpec.constructorBuilder()
                            .fold(properties) { ctorSpec, property ->
                                val propertyType = property.type.toTypeName()
                                ctorSpec.addParameter(
                                    ParameterSpec.builder(property.simpleName.asString(), propertyType)
                                        .also { paramSpec ->
                                            if (propertyType.isNullable) {
                                                paramSpec.defaultValue("null")
                                            }
                                        }
                                        .build()
                                )
                            }
                            .addCode(CodeBlock.of("this(%T(%L))", srcType, properties.joinToString { it.simpleName.asString() }))
                            .build()
                    )
                    typeBuilder.addFunction(
                        FunSpec.builder("asRecord")
                            .returns(recordType)
                            .addCode(CodeBlock.of("return _record"))
                            .build()
                    )
                }
            }
    }
}

fun <T, R> R.fold(elements: Iterable<T>, operation: (R, T) -> R): R {
    return elements.fold(this, operation)
}
