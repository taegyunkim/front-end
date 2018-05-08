/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openCypher.v9_0.rewriting

import org.openCypher.v9_0.ast.Statement
import org.openCypher.v9_0.ast.semantics.SemanticChecker
import org.openCypher.v9_0.util.Rewriter
import org.openCypher.v9_0.util.test_helpers.CypherFunSuite
import org.openCypher.v9_0.parser.ParserFixture.parser

trait RewriteTest {
  self: CypherFunSuite =>

  def rewriterUnderTest: Rewriter

  protected def assertRewrite(originalQuery: String, expectedQuery: String) {
    val original = parseForRewriting(originalQuery)
    val expected = parseForRewriting(expectedQuery)
    SemanticChecker.check(original)
    val result = rewrite(original)
    assert(result === expected, "\n" + originalQuery)
  }

  protected def parseForRewriting(queryText: String): Statement = parser.parse(queryText.replace("\r\n", "\n"))

  protected def rewrite(original: Statement): AnyRef =
    original.rewrite(rewriterUnderTest)

  protected def endoRewrite(original: Statement): Statement =
    original.endoRewrite(rewriterUnderTest)

  protected def assertIsNotRewritten(query: String) {
    val original = parser.parse(query)
    val result = original.rewrite(rewriterUnderTest)
    assert(result === original, "\n" + query)
  }
}
