package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import java.io.Serializable

/**
 * This object performs symbol resolution within a single AST.
 * It is not to be used to perform cross-ASTs symbol resolution.
 */
abstract class LocalSymbolResolver : Serializable {
    /**
     * This will resolve symbols on the given AST. It will set the links in the ReferenceByName found.
     * It will return a list of issues encountered during symbol resolution.
     */
    abstract fun resolveSymbols(root: Node): List<Issue>
}
