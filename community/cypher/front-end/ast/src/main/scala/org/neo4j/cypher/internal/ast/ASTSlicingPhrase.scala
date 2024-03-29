/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.ast

import org.neo4j.cypher.internal.ast.semantics.SemanticAnalysisTooling
import org.neo4j.cypher.internal.ast.semantics.SemanticCheck
import org.neo4j.cypher.internal.ast.semantics.SemanticCheckResult
import org.neo4j.cypher.internal.ast.semantics.SemanticCheckable
import org.neo4j.cypher.internal.ast.semantics.SemanticError
import org.neo4j.cypher.internal.ast.semantics.SemanticExpressionCheck
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.Literal
import org.neo4j.cypher.internal.expressions.LogicalVariable
import org.neo4j.cypher.internal.expressions.PathExpression
import org.neo4j.cypher.internal.expressions.PatternComprehension
import org.neo4j.cypher.internal.expressions.PatternExpression
import org.neo4j.cypher.internal.expressions.SignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.UnsignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.util.ASTNode
import org.neo4j.cypher.internal.util.symbols.CTInteger

// Skip/Limit
trait ASTSlicingPhrase extends SemanticCheckable with SemanticAnalysisTooling {
  self: ASTNode =>
  def name: String
  def dependencies: Set[LogicalVariable] = expression.dependencies
  def expression: Expression

  def semanticCheck: SemanticCheck =
    containsNoVariables chain
      doesNotTouchTheGraph chain
      literalShouldBeUnsignedInteger chain
      SemanticExpressionCheck.simple(expression) chain
      expectType(CTInteger.covariant, expression)

  private def containsNoVariables: SemanticCheck = {
    val deps = dependencies
    if (deps.nonEmpty) {
      val id = deps.toSeq.minBy(_.position)
      error(s"It is not allowed to refer to variables in $name", id.position)
    }
    else SemanticCheckResult.success
  }

  private def doesNotTouchTheGraph: SemanticCheck = {
    val badExpressionFound = expression.folder.treeExists {
      case _: PatternComprehension |
           _: PatternExpression |
           _: PathExpression =>
        true
    }
    when(badExpressionFound) {
      error(s"It is not allowed to refer to variables in $name", expression.position)
    }
  }

  private def literalShouldBeUnsignedInteger: SemanticCheck = {
    try {
      expression match {
        case _: UnsignedDecimalIntegerLiteral => SemanticCheckResult.success
        case i: SignedDecimalIntegerLiteral if i.value >= 0 => SemanticCheckResult.success
        case lit: Literal => error(s"Invalid input. '${lit.asCanonicalStringVal}' is not a valid value. Must be a non-negative integer.", lit.position)
        case _ => SemanticCheckResult.success
      }
    } catch {
      case nfe: NumberFormatException => SemanticError("Invalid input for " + name +
        ". Either the string does not have the appropriate format or the provided number is bigger then 2^63-1", expression.position)
    }
  }
}
