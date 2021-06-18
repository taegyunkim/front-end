/*
 * Copyright (c) Neo4j Sweden AB (http://neo4j.com)
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

import org.opencypher.v9_0.ast.Statement
import org.opencypher.v9_0.frontend.PlannerName
import org.opencypher.v9_0.frontend.helpers.TestContext
import org.opencypher.v9_0.parser.ParserFixture.parser
import org.opencypher.v9_0.rewriting.Deprecations.semanticallyDeprecatedFeaturesIn4_X
import org.opencypher.v9_0.rewriting.Deprecations.syntacticallyDeprecatedFeaturesIn4_X
import org.opencypher.v9_0.util.AnonymousVariableNameGenerator
import org.opencypher.v9_0.util.DeprecatedCoercionOfListToBoolean
import org.opencypher.v9_0.util.DeprecatedHexLiteralSyntax
import org.opencypher.v9_0.util.DeprecatedOctalLiteralSyntax
import org.opencypher.v9_0.util.DeprecatedPatternExpressionOutsideExistsSyntax
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.OpenCypherExceptionFactory
import org.opencypher.v9_0.util.RecordingNotificationLogger
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class SyntaxDeprecationWarningsAndReplacementsTest extends CypherFunSuite {

  test("should warn about deprecated octal syntax") {
    check("RETURN 01277") should equal(Set(
      DeprecatedOctalLiteralSyntax(InputPosition(7, 1, 8))
    ))
  }

  test("should warn about deprecated octal syntax (negative literal)") {
    check("RETURN -01277") should equal(Set(
      DeprecatedOctalLiteralSyntax(InputPosition(7, 1, 8))
    ))
  }

  test("should not warn about correct octal syntax") {
    check("RETURN 0o1277") should equal(Set.empty)
  }

  test("should not warn about correct octal syntax  (negative literal)") {
    check("RETURN -0o1277") should equal(Set.empty)
  }

  test("should warn about deprecated hexadecimal syntax") {
    check("RETURN 0X1277") should equal(Set(
      DeprecatedHexLiteralSyntax(InputPosition(7, 1, 8))
    ))
  }

  test("should warn about deprecated hexadecimal syntax (negative literal)") {
    check("RETURN -0X1277") should equal(Set(
      DeprecatedHexLiteralSyntax(InputPosition(7, 1, 8))
    ))
  }

  test("should not warn about correct hexadecimal syntax") {
    check("RETURN 0x1277") should equal(Set.empty)
  }

  test("should not warn about correct hexadecimal syntax  (negative literal)") {
    check("RETURN -0x1277") should equal(Set.empty)
  }

  test("should warn about pattern expression in RETURN clause") {
    check("RETURN ()--()") should equal(Set(
      DeprecatedPatternExpressionOutsideExistsSyntax(InputPosition(7, 1, 8))
    ))
  }

  test("should not warn about pattern expression in exists function") {
    check("WITH 1 AS foo WHERE exists(()--()) RETURN *") should equal(Set.empty)
  }

  test("should only warn about coercion with a pattern expression in WHERE clause") {
    check("WITH 1 AS foo WHERE ()--() RETURN *") should equal(Set(
      DeprecatedCoercionOfListToBoolean(InputPosition(20, 1, 21))
    ))
  }

  test("should only warn about coercion with a pattern expression in boolean expression") {
    check("RETURN NOT ()--()") should equal(Set(
      DeprecatedCoercionOfListToBoolean(InputPosition(11, 1, 12))
    ))
    check("RETURN ()--() AND ()--()--()") should equal(Set(
      DeprecatedCoercionOfListToBoolean(InputPosition(7, 1, 8)),
      DeprecatedCoercionOfListToBoolean(InputPosition(18, 1, 19)),
    ))
    check("RETURN ()--() OR ()--()--()") should equal(Set(
      DeprecatedCoercionOfListToBoolean(InputPosition(7, 1, 8)),
      DeprecatedCoercionOfListToBoolean(InputPosition(17, 1, 18)),
    ))
  }

  private val plannerName = new PlannerName {
    override def name: String = "fake"
    override def toTextOutput: String = "fake"
    override def version: String = "fake"
  }

  private def check(query: String) = {
    val logger = new RecordingNotificationLogger()
    val statement = parse(query)
    val initialState = InitialState(query, None, plannerName, new AnonymousVariableNameGenerator, maybeStatement = Some(statement))

    val pipeline =
      SyntaxDeprecationWarningsAndReplacements(syntacticallyDeprecatedFeaturesIn4_X) andThen
        PreparatoryRewriting andThen
        SemanticAnalysis(warn = true) andThen
        SyntaxDeprecationWarningsAndReplacements(semanticallyDeprecatedFeaturesIn4_X)

    pipeline.transform(initialState, TestContext(logger))
    logger.notifications
  }

  private def parse(queryText: String): Statement = parser.parse(queryText.replace("\r\n", "\n"), OpenCypherExceptionFactory(None))

}
