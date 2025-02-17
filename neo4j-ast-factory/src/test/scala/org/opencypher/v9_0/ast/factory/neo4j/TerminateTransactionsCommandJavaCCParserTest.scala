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
import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.util.symbols.CTAny
import org.opencypher.v9_0.util.test_helpers.TestName
import org.scalatest.FunSuiteLike

/* Tests for terminating transactions */
class TerminateTransactionsCommandJavaCCParserTest extends ParserComparisonTestBase with FunSuiteLike with TestName with AstConstructionTestSupport {

  Seq("TRANSACTION", "TRANSACTIONS").foreach { transactionKeyword =>

    test(s"TERMINATE $transactionKeyword") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List.empty))(defaultPos)))
    }

    test(s"TERMINATE $transactionKeyword 'db1-transaction-123'") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("db1-transaction-123")))(defaultPos)))
    }

    test(s"""TERMINATE $transactionKeyword "db1-transaction-123"""") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("db1-transaction-123")))(defaultPos)))
    }

    test(s"TERMINATE $transactionKeyword 'my.db-transaction-123'") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("my.db-transaction-123")))(pos)), comparePosition = false)
    }

    test(s"TERMINATE $transactionKeyword $$param") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Right(parameter("param", CTAny)))(pos)), comparePosition = false)
    }

    test(s"""TERMINATE $transactionKeyword "db1 - transaction - 123", 'db2-transaction-45a6'""") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("db1 - transaction - 123", "db2-transaction-45a6")))(defaultPos)))
    }

    test(s"TERMINATE $transactionKeyword 'yield-transaction-123'") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("yield-transaction-123")))(defaultPos)))
    }

    test(s"TERMINATE $transactionKeyword 'where-transaction-123'") {
      assertJavaCCAST(testName, query(ast.TerminateTransactionsClause(Left(List("where-transaction-123")))(pos)), comparePosition = false)
    }

    test(s"USE db TERMINATE $transactionKeyword 'db1-transaction-123'") {
      assertJavaCCAST(testName, query(use(varFor("db")), ast.TerminateTransactionsClause(Left(List("db1-transaction-123")))(pos)),
        comparePosition = false)
    }

  }

  // Filtering is not allowed

  test("TERMINATE TRANSACTIONS YIELD") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTION WHERE transactionId = 'db1-transaction-123'") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS YIELD database") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS 'db1-transaction-123', 'db2-transaction-456' YIELD *") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS YIELD * ORDER BY transactionId SKIP 2 LIMIT 5") {
    assertSameAST(testName)
  }

  test("USE db TERMINATE TRANSACTIONS YIELD transactionId, activeLockCount AS pp ORDER BY pp SKIP 2 LIMIT 5 WHERE pp < 50 RETURN transactionId") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS 'db1-transaction-123' YIELD transactionId AS TRANSACTION, database AS OUTPUT") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS YIELD * YIELD *") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS WHERE transactionId = 'db1-transaction-123' YIELD *") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS WHERE transactionId = 'db1-transaction-123' RETURN *") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS YIELD a b RETURN *") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS RETURN *") {
    assertSameAST(testName)
  }

  // Negative tests

  test("TERMINATE TRANSACTIONS db-transaction-123") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS 'db-transaction-123', $param") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS $param, 'db-transaction-123'") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS $param, $param2") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS ['db1-transaction-123', 'db2-transaction-456']") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTION foo") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTION x+2") {
    assertSameAST(testName)
  }

  test("TERMINATE CURRENT USER TRANSACTION") {
    assertSameAST(testName)
  }

  test("TERMINATE USER user TRANSACTION") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTION EXECUTED BY USER user") {
    assertSameAST(testName)
  }

  test("TERMINATE ALL TRANSACTIONS") {
    assertSameAST(testName)
  }

  test("TERMINATE TRANSACTIONS ALL") {
    assertSameAST(testName)
  }

  // Invalid clause order

  for (prefix <- Seq("USE neo4j", "")) {
    test(s"$prefix TERMINATE TRANSACTIONS WITH * MATCH (n) RETURN n") {
      // Can't parse WITH after TERMINATE
      assertJavaCCExceptionStart(testName, "Invalid input 'WITH': expected")
    }

    test(s"$prefix UNWIND range(1,10) as b TERMINATE TRANSACTIONS YIELD * RETURN *") {
      // Can't parse TERMINATE  after UNWIND
      assertJavaCCExceptionStart(testName, "Invalid input 'TERMINATE': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS WITH name, type RETURN *") {
      // Can't parse WITH after TERMINATE
      assertJavaCCExceptionStart(testName, "Invalid input 'WITH': expected")
    }

    test(s"$prefix WITH 'n' as n TERMINATE TRANSACTIONS YIELD name RETURN name as numIndexes") {
      assertJavaCCExceptionStart(testName, "Invalid input 'TERMINATE': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS RETURN name as numIndexes") {
      assertJavaCCExceptionStart(testName, "Invalid input 'RETURN': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS WITH 1 as c RETURN name as numIndexes") {
      assertJavaCCExceptionStart(testName, "Invalid input 'WITH': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS WITH 1 as c") {
      assertJavaCCExceptionStart(testName, "Invalid input 'WITH': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS UNWIND as as a RETURN a") {
      assertJavaCCExceptionStart(testName, "Invalid input 'UNWIND': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS TERMINATE TRANSACTIONS YIELD id2 RETURN id2") {
      assertJavaCCExceptionStart(testName, "Invalid input 'TERMINATE': expected")
    }

    test(s"$prefix TERMINATE TRANSACTIONS RETURN id2 YIELD id2") {
      assertJavaCCExceptionStart(testName, "Invalid input 'RETURN': expected")
    }
  }

}
