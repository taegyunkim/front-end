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
package org.opencypher.v9_0.rewriting

import org.opencypher.v9_0.ast.Statement
import org.opencypher.v9_0.expressions.Parameter
import org.opencypher.v9_0.parser.ParserFixture.parser
import org.opencypher.v9_0.rewriting.rewriters.parameterValueTypeReplacement
import org.opencypher.v9_0.util.OpenCypherExceptionFactory
import org.opencypher.v9_0.util.symbols.CTBoolean
import org.opencypher.v9_0.util.symbols.CTInteger
import org.opencypher.v9_0.util.symbols.CTString
import org.opencypher.v9_0.util.symbols.CypherType
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class ParameterTypeValueReplacementTest extends CypherFunSuite {
  // This class does not work with the old parameter syntax

  test("single integer parameter should be rewritten") {
    val params = Map("param" -> CTInteger)
    assertRewrite("MATCH (n) WHERE n.foo > $param RETURN n", params)
  }

  test("multiple integer parameters should be rewritten") {
    val params = Map("param1" -> CTInteger, "param2" -> CTInteger, "param3" -> CTInteger)
    assertRewrite("MATCH (n) WHERE n.foo > $param1 AND n.bar < $param2 AND n.baz = $param3 RETURN n", params)
  }

  test("single string parameter should be rewritten") {
    val params = Map("param" -> CTString)
    assertRewrite("MATCH (n) WHERE n.foo > $param RETURN n", params)
  }

  test("multiple string parameters should be rewritten") {
    val params = Map("param1" -> CTString, "param2" -> CTString, "param3" -> CTString)
    assertRewrite("MATCH (n) WHERE n.foo STARTS WITH $param1 AND n.bar ENDS WITH $param2 AND n.baz = $param3 RETURN n", params)
  }

  test("mixed parameters should be rewritten") {
    val params = Map("param1" -> CTString, "param2" -> CTBoolean, "param3" -> CTInteger)
    assertRewrite("MATCH (n) WHERE n.foo STARTS WITH $param1 AND n.bar = $param2 AND n.baz = $param3 RETURN n", params)
  }

  private def assertRewrite(originalQuery: String, parameterTypes: Map[String, CypherType]) {
    val exceptionFactory = OpenCypherExceptionFactory(None)
    val original: Statement = parser.parse(originalQuery, exceptionFactory) // Do not use the old parameter syntax here

    original.findByAllClass[Parameter].size should equal(parameterTypes.size) // make sure we use all given parameters in the query

    val rewriter = parameterValueTypeReplacement(parameterTypes)
    val result = original.rewrite(rewriter).asInstanceOf[Statement]

    val rewrittenParameters: Seq[Parameter] = result.findByAllClass[Parameter]
    val rewrittenParameterTypes = rewrittenParameters.map(p => p.name -> parameterTypes.getOrElse(p.name, fail("something went wrong"))).toMap
    rewrittenParameterTypes should equal(parameterTypes)
  }
}
