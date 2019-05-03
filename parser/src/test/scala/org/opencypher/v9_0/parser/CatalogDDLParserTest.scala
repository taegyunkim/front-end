/*
 * Copyright © 2002-2019 Neo4j Sweden AB (http://neo4j.com)
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
package org.opencypher.v9_0.parser

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.expressions.{Parameter => Param}
import org.opencypher.v9_0.util.symbols._
import org.parboiled.scala.Rule1

class CatalogDDLParserTest
  extends ParserAstTest[ast.Statement] with Statement with AstConstructionTestSupport {


  implicit val parser: Rule1[ast.Statement] = Statement

  private val singleQuery = ast.SingleQuery(Seq(ast.ConstructGraph()(pos)))(pos)
  private val returnGraph: ast.ReturnGraph = ast.ReturnGraph(None)(pos)
  private val returnQuery = ast.SingleQuery(Seq(returnGraph))(pos)

  test("SHOW USERS") {
    yields(ast.ShowUsers())
  }

  test("CATALOG CREATE USER foo SET PASSWORD 'password'") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CREATE uSER foo SET PASSWORD $password") {
    yields(ast.CreateUser("foo", None, Some(Param("password", CTAny)(_)), requirePasswordChange = true, suspended = false))
  }

  test("CREATE USER \"foo\" SET PASSwORD 'password'") {
    yields(ast.CreateUser("\"foo\"", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CREATE USER !#\"~ SeT PASSWORD 'password'") {
    yields(ast.CreateUser("!#\"~", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CREaTE USER foo SET PASSWORD 'password' CHANGE REQUIRED") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CATALOG CREATE USER foo SET PASSWORD $password CHANGE REQUIRED") {
    yields(ast.CreateUser("foo", None, Some(Param("password", CTAny)(_)), requirePasswordChange = true, suspended = false))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE required") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CREATE USER foo SET PASSWORD 'password' CHAngE NOT REQUIRED") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = false, suspended = false))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = false, suspended = false))
  }

  test("CREATE USER foo SET PASSWORD $password SET  PASSWORD CHANGE NOT REQUIRED") {
    yields(ast.CreateUser("foo", None, Some(Param("password", CTAny)(_)), requirePasswordChange = false, suspended = false))
  }

  test("CATALOG CREATE USER foo SET PASSWORD 'password' SET STATUS SUSPENDed") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = true, suspended = true))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET STATUS ACtiVE") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = true, suspended = false))
  }

  test("CREATE USER foo SET PASSWORD 'password' SET PASSWORD CHANGE NOT REQUIRED SET   STATuS SUSPENDED") {
    yields(ast.CreateUser("foo", Some("password"), None, requirePasswordChange = false, suspended = true))
  }

  test("CREATE USER foo SET PASSWORD $password CHANGE REQUIRED SET STATUS SUSPENDED") {
    yields(ast.CreateUser("foo", None, Some(Param("password", CTAny)(_)), requirePasswordChange = true, suspended = true))
  }

  test("CREATE USER foo") {
    failsToParse
  }

  test("CATALOG CREATE USER fo,o SET PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER f:oo SET PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER foo PASSWORD 'password'") {
    failsToParse
  }

  test("CREATE USER foo SET PASSWORD 'password' WITH STAUS ACTIVE") {
    failsToParse
  }

  test("DROP USER foo") {
    yields(ast.DropUser("foo"))
  }

  // TODO ALTER USER tests

  test("SHOW ROLES") {
    yields(ast.ShowRoles(withUsers = false, showAll = true))
  }

  test("SHOW ALL ROLES") {
    yields(ast.ShowRoles(withUsers = false, showAll = true))
  }

  test("CATALOG SHOW POPULATED ROLES") {
    yields(ast.ShowRoles(withUsers = false, showAll = false))
  }

  test("SHOW ROLES WITH USERS") {
    yields(ast.ShowRoles(withUsers = true, showAll = true))
  }

  test("CATALOG SHOW ALL ROLES WITH USERS") {
    yields(ast.ShowRoles(withUsers = true, showAll = true))
  }

  test("SHOW POPULATED ROLES WITH USERS") {
    yields(ast.ShowRoles(withUsers = true, showAll = false))
  }

  test("CREATE ROLE foo") {
    yields(ast.CreateRole("foo", None))
  }

  test("CATALOG CREATE ROLE \"foo\"") {
    yields(ast.CreateRole("foo", None))
  }

  test("CREATE ROLE f%o") {
    failsToParse
  }

  test("CREATE ROLE foo AS COPY OF bar") {
    yields(ast.CreateRole("foo", Some("bar")))
  }

  test("CREATE ROLE foo AS COPY OF") {
    failsToParse
  }

  test("DROP ROLE foo") {
    yields(ast.DropRole("foo"))
  }

  test("SHOW DATABASE foo.bar") {
    yields(ast.ShowDatabase("foo.bar"))
  }

  test("SHOW DATABASES") {
    yields(ast.ShowDatabases())
  }

  test("CREATE DATABASE foo.bar") {
    yields(ast.CreateDatabase("foo.bar"))
  }

  test("CREATE DATABASE \"foo.bar\"") {
    yields(ast.CreateDatabase("foo.bar"))
  }

  test("CATALOG CREATE DATABASE foo.bar") {
    yields(ast.CreateDatabase("foo.bar"))
  }

  test("CATALOG CREATE DATABASE foo_bar42") {
    yields(ast.CreateDatabase("foo_bar42"))
  }

  test("CATALOG CREATE DATABASE _foo_bar42") {
    failsToParse
  }

  test("CATALOG CREATE DATABASE 42foo_bar") {
    failsToParse
  }

  test("CATALOG DROP DATABASE foo.bar") {
    yields(ast.DropDatabase("foo.bar"))
  }

  test("CATALOG CREATE GRAPH foo.bar { RETURN GRAPH }") {
    val query = ast.SingleQuery(Seq(returnGraph))(pos)
    val graphName = ast.CatalogName("foo", List("bar"))

    yields(ast.CreateGraph(graphName, query))
  }

  test("CATALOG CREATE GRAPH foo.bar { FROM GRAPH foo RETURN GRAPH UNION ALL FROM GRAPH bar RETURN GRAPH }") {
    val useGraph1 = ast.GraphLookup(ast.CatalogName("foo"))(pos)
    val useGraph2 = ast.GraphLookup(ast.CatalogName("bar"))(pos)
    val lhs = ast.SingleQuery(Seq(useGraph1, returnGraph))(pos)
    val rhs = ast.SingleQuery(Seq(useGraph2, returnGraph))(pos)
    val union = ast.UnionAll(lhs, rhs)(pos)
    val graphName = ast.CatalogName("foo", List("bar"))

    yields(ast.CreateGraph(graphName, union))
  }

  test("CATALOG CREATE GRAPH foo.bar { FROM GRAPH foo RETURN GRAPH UNION FROM GRAPH bar RETURN GRAPH }") {
    val useGraph1 = ast.GraphLookup(ast.CatalogName("foo"))(pos)
    val useGraph2 = ast.GraphLookup(ast.CatalogName("bar"))(pos)
    val lhs = ast.SingleQuery(Seq(useGraph1, returnGraph))(pos)
    val rhs = ast.SingleQuery(Seq(useGraph2, returnGraph))(pos)
    val union = ast.UnionDistinct(lhs, rhs)(pos)
    val graphName = ast.CatalogName("foo", List("bar"))

    yields(ast.CreateGraph(graphName, union))
  }

  test("CATALOG CREATE GRAPH foo.bar { CONSTRUCT }") {
    val graphName = ast.CatalogName("foo", List("bar"))

    yields(ast.CreateGraph(graphName, singleQuery))
  }

  // missing graph name
  test("CATALOG CREATE GRAPH { RETURN GRAPH }") {
    failsToParse
  }

  test("CATALOG CREATE GRAPH `foo.bar.baz.baz` { CONSTRUCT }"){
    yields(ast.CreateGraph(
      new ast.CatalogName(List("foo.bar.baz.baz")),
      singleQuery
    ))
  }

  test("CATALOG CREATE GRAPH `foo.bar`.baz { CONSTRUCT }"){
    yields(ast.CreateGraph(
      new ast.CatalogName(List("foo.bar", "baz")),
      singleQuery
    ))
  }

  test("CATALOG CREATE GRAPH foo.`bar.baz` { CONSTRUCT }"){
    yields(ast.CreateGraph(
      new ast.CatalogName(List("foo", "bar.baz")),
      singleQuery
    ))
  }

  test("CATALOG CREATE GRAPH `foo.bar`.`baz.baz` { CONSTRUCT }"){
    yields(ast.CreateGraph(
      new ast.CatalogName(List("foo.bar", "baz.baz")),
      singleQuery
    ))
  }

  // missing graph name
  test("CATALOG DROP GRAPH union") {
    val graphName = ast.CatalogName("union")

    yields(ast.DropGraph(graphName))
  }

  // missing graph name; doesn't fail because it's a valid query if GRAPH is a variable
  ignore("CATALOG DROP GRAPH") {
    failsToParse
  }

  test("CATALOG CREATE VIEW viewName { RETURN GRAPH }") {
    val graphName = ast.CatalogName("viewName")

    yields(ast.CreateView(graphName, Seq.empty, returnQuery, "RETURN GRAPH"))
  }

  test("CATALOG CREATE QUERY viewName { RETURN GRAPH }") {
    val graphName = ast.CatalogName("viewName")

    yields(ast.CreateView(graphName, Seq.empty, returnQuery, "RETURN GRAPH"))
  }

  test("CATALOG CREATE VIEW foo.bar($graph1, $graph2) { FROM $graph1 RETURN GRAPH }") {
    val query = ast.SingleQuery(Seq(ast.GraphByParameter(parameter("graph1", CTAny))(pos),  returnGraph))(pos)
    val graphName = ast.CatalogName("foo", List("bar"))
    val params = Seq(parameter("graph1", CTAny), parameter("graph2", CTAny))

    yields(ast.CreateView(graphName, params, query, "FROM $graph1 RETURN GRAPH"))
  }

  test("CATALOG CREATE VIEW foo.bar() { RETURN GRAPH }") {
    val query = ast.SingleQuery(Seq(returnGraph))(pos)
    val graphName = ast.CatalogName("foo", List("bar"))

    yields(ast.CreateView(graphName, Seq.empty, query, "RETURN GRAPH"))
  }

  test("CATALOG DROP VIEW viewName") {
    val graphName = ast.CatalogName("viewName")

    yields(ast.DropView(graphName))
  }

  test("CATALOG DROP QUERY viewName") {
    val graphName = ast.CatalogName("viewName")

    yields(ast.DropView(graphName))
  }
}
