/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
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
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.frontend.phases.CompilationPhaseTracer.CompilationPhase.AST_REWRITE
import org.opencypher.v9_0.rewriting.Deprecations
import org.opencypher.v9_0.rewriting.RewritingStepSequencer
import org.opencypher.v9_0.rewriting.rewriters.LiteralsAreAvailable
import org.opencypher.v9_0.rewriting.rewriters.PatternExpressionsHaveSemanticInfo
import org.opencypher.v9_0.rewriting.rewriters.ProjectionClausesHaveSemanticInfo
import org.opencypher.v9_0.rewriting.rewriters.expandCallWhere
import org.opencypher.v9_0.rewriting.rewriters.expandShowWhere
import org.opencypher.v9_0.rewriting.rewriters.insertWithBetweenOptionalMatchAndMatch
import org.opencypher.v9_0.rewriting.rewriters.mergeInPredicates
import org.opencypher.v9_0.rewriting.rewriters.normalizeWithAndReturnClauses
import org.opencypher.v9_0.rewriting.rewriters.replaceDeprecatedCypherSyntax
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.StepSequencer.AccumulatedSteps
import org.opencypher.v9_0.util.inSequence

case class PreparatoryRewriting(deprecations: Deprecations) extends Phase[BaseContext, BaseState, BaseState] {

  override def process(from: BaseState, context: BaseContext): BaseState = {

    val AccumulatedSteps(orderedSteps, _) = RewritingStepSequencer.orderSteps(Set(
      normalizeWithAndReturnClauses(context.cypherExceptionFactory, context.notificationLogger),
      insertWithBetweenOptionalMatchAndMatch,
      expandCallWhere,
      expandShowWhere,
      replaceDeprecatedCypherSyntax(deprecations),
      mergeInPredicates), initialConditions = Set(LiteralsAreAvailable))

    val rewrittenStatement = from.statement().endoRewrite(inSequence(orderedSteps: _*))

    from.withStatement(rewrittenStatement)
  }

  override val phase = AST_REWRITE

  override val description = "rewrite the AST into a shape that semantic analysis can be performed on"

  override def postConditions: Set[StepSequencer.Condition] = Set.empty
}
