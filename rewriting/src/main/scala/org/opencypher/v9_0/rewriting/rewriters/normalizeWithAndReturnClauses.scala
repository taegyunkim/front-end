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
package org.opencypher.v9_0.rewriting.rewriters

import org.opencypher.v9_0.ast.AliasedReturnItem
import org.opencypher.v9_0.ast.AscSortItem
import org.opencypher.v9_0.ast.DescSortItem
import org.opencypher.v9_0.ast.OrderBy
import org.opencypher.v9_0.ast.ProjectionClause
import org.opencypher.v9_0.ast.Query
import org.opencypher.v9_0.ast.QueryPart
import org.opencypher.v9_0.ast.Return
import org.opencypher.v9_0.ast.ReturnItems
import org.opencypher.v9_0.ast.ShowCurrentUser
import org.opencypher.v9_0.ast.ShowDatabase
import org.opencypher.v9_0.ast.ShowPrivilegeCommands
import org.opencypher.v9_0.ast.ShowPrivileges
import org.opencypher.v9_0.ast.ShowRoles
import org.opencypher.v9_0.ast.ShowUsers
import org.opencypher.v9_0.ast.SingleQuery
import org.opencypher.v9_0.ast.SortItem
import org.opencypher.v9_0.ast.UnaliasedReturnItem
import org.opencypher.v9_0.ast.UnionAll
import org.opencypher.v9_0.ast.UnionDistinct
import org.opencypher.v9_0.ast.Where
import org.opencypher.v9_0.ast.Yield
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.expressions.LogicalVariable
import org.opencypher.v9_0.expressions.Variable
import org.opencypher.v9_0.rewriting.rewriters.factories.PreparatoryRewritingRewriterFactory
import org.opencypher.v9_0.util.CypherExceptionFactory
import org.opencypher.v9_0.util.InternalNotificationLogger
import org.opencypher.v9_0.util.MissingAliasNotification
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.StepSequencer.Condition
import org.opencypher.v9_0.util.StepSequencer.Step
import org.opencypher.v9_0.util.topDown

case object ReturnItemsAreAliased extends Condition
case object ExpressionsInOrderByAndWhereUseAliases extends Condition

/**
 * This rewriter normalizes the scoping structure of a query, ensuring it is able to
 * be correctly processed for semantic checking. It makes sure that all return items
 * in WITH clauses are aliased.
 *
 * It also replaces expressions and subexpressions in ORDER BY and WHERE
 * to use aliases introduced by the WITH, where possible.
 *
 * Example:
 *
 * MATCH n
 * WITH n.prop AS prop ORDER BY n.prop DESC
 * RETURN prop
 *
 * This rewrite will change the query to:
 *
 * MATCH n
 * WITH n.prop AS prop ORDER BY prop DESC
 * RETURN prop AS prop
 */
case class normalizeWithAndReturnClauses(cypherExceptionFactory: CypherExceptionFactory, notificationLogger: InternalNotificationLogger) extends Rewriter {

  def apply(that: AnyRef): AnyRef = that match {
    case q@Query(_, queryPart) => q.copy(part = rewriteTopLevelQueryPart(queryPart))(q.position)

    case s@ShowPrivileges(_, Some(Left((yields, returns))),_) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case s@ShowPrivilegeCommands(_, _, Some(Left((yields, returns))),_) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case s@ShowDatabase(_, Some(Left((yields, returns))), _) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case s@ShowCurrentUser(Some(Left((yields, returns))),_) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case s@ShowUsers(Some(Left((yields, returns))),_) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case s@ShowRoles(_, _, Some(Left((yields, returns))),_) =>
      s.copy(yieldOrWhere = Some(Left((addAliasesToYield(yields),returns.map(addAliasesToReturn)))))(s.position)
        .withGraph(s.useGraph)

    case x => x
  }

  /**
   * Rewrites all single queries in the top level query (which can be a single or a union query of single queries).
   * It does not rewrite query parts in subqueries.
   */
  private def rewriteTopLevelQueryPart(queryPart: QueryPart): QueryPart = queryPart match {
    case sq:SingleQuery => rewriteTopLevelSingleQuery(sq)
    case union@UnionAll(part, query) => union.copy(part = rewriteTopLevelQueryPart(part), query = rewriteTopLevelSingleQuery(query))(union.position)
    case union@UnionDistinct(part, query) => union.copy(part = rewriteTopLevelQueryPart(part), query = rewriteTopLevelSingleQuery(query))(union.position)
  }

  /**
   * Adds aliases to all return items in Return clauses in the top level query.
   * Rewrites all projection clauses (also in subqueries) using [[rewriteProjectionsRecursively]].
   */
  private def rewriteTopLevelSingleQuery(singleQuery: SingleQuery): SingleQuery = {
    val newClauses = singleQuery.clauses.map {
      case r: Return => addAliasesToReturn(r)
      case x => x
    }
    singleQuery.copy(clauses = newClauses)(singleQuery.position).endoRewrite(rewriteProjectionsRecursively)
  }

  private def addAliasesToReturn(r: Return): Return = r.copy(returnItems = aliasUnaliasedReturnItems(r.returnItems, warnForMissingAliases = false))(r.position)
  private def addAliasesToYield(y: Yield): Yield = y.copy(returnItems = aliasUnaliasedReturnItems(y.returnItems, warnForMissingAliases = false))(y.position)

  /**
   * Convert all UnaliasedReturnItems to AliasedReturnItems.
   *
   * @param warnForMissingAliases if `true`, generate warnings if an expression other than a variable or map projection
   *                              gets automatically aliased.
   */
  private def aliasUnaliasedReturnItems(ri: ReturnItems, warnForMissingAliases: Boolean): ReturnItems = {
    val aliasedReturnItems =
      ri.items.map {
        case i: UnaliasedReturnItem =>
          if (warnForMissingAliases && i.alias.isEmpty) {
            notificationLogger.log(MissingAliasNotification(i.position))
          }
          val alias = i.alias match {
            case Some(value) => value
            case None =>
              Variable(i.name)(i.expression.position)
          }

          AliasedReturnItem(i.expression, alias)(i.position, isAutoAliased = true)
        case x => x
      }
    ri.copy(items = aliasedReturnItems)(ri.position)
  }

  /**
   * Convert those UnaliasedReturnItems to AliasedReturnItems which refer to a variable or a map projection.
   * Those can be deemed as implicitly aliased.
   */
  private def aliasImplicitlyAliasedReturnItems(ri: ReturnItems): ReturnItems = {
    val newItems =
      ri.items.map {
        case i: UnaliasedReturnItem if i.alias.isDefined =>
          AliasedReturnItem(i.expression, i.alias.get)(i.position, isAutoAliased = true)
        case x => x
      }
    ri.copy(items = newItems)(ri.position)
  }

  private val rewriteProjectionsRecursively: Rewriter = topDown(Rewriter.lift {
    // Only alias return items
    case clause@ProjectionClause(_, ri: ReturnItems, None, _, _, None) =>
      val replacer: ReturnItems => ReturnItems = if (clause.isReturn) aliasUnaliasedReturnItems(_, warnForMissingAliases = true) else aliasImplicitlyAliasedReturnItems
      clause.copyProjection(returnItems = replacer(ri))

    // Alias return items and rewrite ORDER BY and WHERE
    case clause@ProjectionClause(_, ri: ReturnItems, orderBy, _, _, where) =>
      clause.verifyOrderByAggregationUse((s, i) => throw cypherExceptionFactory.syntaxException(s, i))

      val existingAliases = ri.items.collect {
        case AliasedReturnItem(expression, variable) => expression -> variable
      }.toMap

      val updatedOrderBy = orderBy.map(aliasOrderBy(existingAliases, _))
      val updatedWhere = where.map(aliasWhere(existingAliases, _))

      val replacer: ReturnItems => ReturnItems = if (clause.isReturn) aliasUnaliasedReturnItems(_, warnForMissingAliases = true) else aliasImplicitlyAliasedReturnItems
      clause.copyProjection(returnItems = replacer(ri), orderBy = updatedOrderBy, where = updatedWhere)
  })

  /**
   * Given a list of existing aliases, this rewrites an OrderBy to use these where possible.
   */
  private def aliasOrderBy(existingAliases: Map[Expression, LogicalVariable], originalOrderBy: OrderBy): OrderBy = {
    val updatedSortItems = originalOrderBy.sortItems.map { aliasSortItem(existingAliases, _)}
    OrderBy(updatedSortItems)(originalOrderBy.position)
  }

  /**
   * Given a list of existing aliases, this rewrites a SortItem to use these where possible.
   */
  private def aliasSortItem(existingAliases: Map[Expression, LogicalVariable], sortItem: SortItem): SortItem = {
    sortItem match {
      case AscSortItem(expression) => AscSortItem(aliasExpression(existingAliases, expression))(sortItem.position, sortItem.originalExpression)
      case DescSortItem(expression) => DescSortItem(aliasExpression(existingAliases, expression))(sortItem.position, sortItem.originalExpression)
    }
  }

  /**
   * Given a list of existing aliases, this rewrites a where to use these where possible.
   */
  private def aliasWhere(existingAliases: Map[Expression, LogicalVariable], originalWhere: Where): Where = {
    Where(aliasExpression(existingAliases, originalWhere.expression))(originalWhere.position)
  }

  /**
   * Given a list of existing aliases, this rewrites expressions to use these where possible.
   */
  private def aliasExpression(existingAliases: Map[Expression, LogicalVariable], expression: Expression): Expression = {
    existingAliases.get(expression) match {
      case Some(alias) if !existingAliases.valuesIterator.contains(expression) =>
        alias.copyId
      case _ =>
        val newExpression = expression.endoRewrite(topDown(Rewriter.lift {
          case subExpression: Expression =>
            existingAliases.get(subExpression) match {
              case Some(subAlias) if !existingAliases.valuesIterator.contains(subExpression) => subAlias.copyId
              case _ => subExpression
            }
        }))
        newExpression
    }
  }
}

object normalizeWithAndReturnClauses extends Step with PreparatoryRewritingRewriterFactory {
  override def getRewriter(cypherExceptionFactory: CypherExceptionFactory, notificationLogger: InternalNotificationLogger): Rewriter = {
    normalizeWithAndReturnClauses(cypherExceptionFactory, notificationLogger)
  }

  override def preConditions: Set[StepSequencer.Condition] = Set.empty

  override def postConditions: Set[StepSequencer.Condition] = Set(ReturnItemsAreAliased, ExpressionsInOrderByAndWhereUseAliases)

  override def invalidatedConditions: Set[StepSequencer.Condition] = Set.empty
}