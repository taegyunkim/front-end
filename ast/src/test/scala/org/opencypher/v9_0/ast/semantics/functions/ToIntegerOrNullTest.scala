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
package org.opencypher.v9_0.ast.semantics.functions

import org.opencypher.v9_0.util.symbols.CTAny
import org.opencypher.v9_0.util.symbols.CTBoolean
import org.opencypher.v9_0.util.symbols.CTDate
import org.opencypher.v9_0.util.symbols.CTFloat
import org.opencypher.v9_0.util.symbols.CTInteger
import org.opencypher.v9_0.util.symbols.CTMap
import org.opencypher.v9_0.util.symbols.CTNumber
import org.opencypher.v9_0.util.symbols.CTPoint
import org.opencypher.v9_0.util.symbols.CTString

class ToIntegerOrNullTest extends FunctionTestBase("toIntegerOrNull")  {

  test("shouldAcceptCorrectTypes") {
    testValidTypes(CTString)(CTInteger)
    testValidTypes(CTFloat)(CTInteger)
    testValidTypes(CTInteger)(CTInteger)
    testValidTypes(CTNumber.covariant)(CTInteger)
    testValidTypes(CTAny.covariant)(CTInteger)
    testValidTypes(CTBoolean)(CTInteger)
    testValidTypes(CTMap)(CTInteger)
    testValidTypes(CTDate)(CTInteger)
    testValidTypes(CTPoint)(CTInteger)
  }

  test("shouldFailIfWrongNumberOfArguments") {
    testInvalidApplication()(
      "Insufficient parameters for function 'toIntegerOrNull'"
    )
    testInvalidApplication(CTString, CTMap)(
      "Too many parameters for function 'toIntegerOrNull'"
    )
  }
}
