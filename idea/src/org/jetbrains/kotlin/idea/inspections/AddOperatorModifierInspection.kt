/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.withExpectedActuals
import org.jetbrains.kotlin.idea.util.nameIdentifierTextRangeInThis
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.util.OperatorChecks
import org.jetbrains.kotlin.util.OperatorNameConventions

class AddOperatorModifierInspection : AbstractApplicabilityBasedInspection<KtNamedFunction>(KtNamedFunction::class.java) {
    override fun inspectionHighlightRangeInElement(element: KtNamedFunction) = element.nameIdentifierTextRangeInThis()

    override fun inspectionText(element: KtNamedFunction) = "Function should have 'operator' modifier"

    override val defaultFixText = "Add 'operator' modifier"

    override fun isApplicable(element: KtNamedFunction): Boolean {
        if (element.nameIdentifier == null || element.hasModifier(KtTokens.OPERATOR_KEYWORD)) return false
        if (element.nameAsName !in OperatorNameConventions.ALL_OPERATION_NAMES) return false
        val functionDescriptor = element.resolveToDescriptorIfAny() ?: return false
        return !functionDescriptor.isOperator && OperatorChecks.check(functionDescriptor).isSuccess
    }

    override fun applyTo(element: KtNamedFunction, project: Project, editor: Editor?) {
        for (declaration in element.withExpectedActuals()) {
            declaration.addModifier(KtTokens.OPERATOR_KEYWORD)
        }
    }
}
