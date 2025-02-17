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
package org.opencypher.v9_0.ast.factory.neo4j

import org.opencypher.v9_0.ast

class ProjectionClauseJavaccParserTest extends JavaccParserAstTestBase[ast.Clause] {

  implicit val parser: JavaccRule[ast.Clause] = JavaccRule.Clause

  test("WITH *") {
    yields(ast.With(ast.ReturnItems(includeExisting = true, Seq.empty)(pos)))
  }

  test("WITH 1 AS a") {
    yields(ast.With(ast.ReturnItems(includeExisting = false, Seq(ast.AliasedReturnItem(literalInt(1), varFor("a"))(pos, isAutoAliased = false)))(pos)))
  }

  test("WITH *, 1 AS a") {
    yields(ast.With(ast.ReturnItems(includeExisting = true, Seq(ast.AliasedReturnItem(literalInt(1), varFor("a"))(pos, isAutoAliased = false)))(pos)))
  }

  test("WITH ") {
    failsToParse
  }

  test("RETURN *") {
    yields(ast.Return(ast.ReturnItems(includeExisting = true, Seq.empty)(pos)))
  }

  test("RETURN 1 AS a") {
    yields(ast.Return(ast.ReturnItems(includeExisting = false, Seq(ast.AliasedReturnItem(literalInt(1), varFor("a"))(pos, isAutoAliased = false)))(pos)))
  }

  test("RETURN *, 1 AS a") {
    yields(ast.Return(ast.ReturnItems(includeExisting = true, Seq(ast.AliasedReturnItem(literalInt(1), varFor("a"))(pos, isAutoAliased = false)))(pos)))
  }

  test("RETURN ") {
    failsToParse
  }

  test("RETURN GRAPH *") {
    failsToParse
  }
}
