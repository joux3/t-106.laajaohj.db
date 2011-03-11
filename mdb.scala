// THIS FILE IS NOT MEANT TO BE COMPILED!
// Instead, you can load it into the interpreter after compiling the rest:
//   make
//   scala
//   :load mdb.scala
// and execute your queries like the examples below.

import minidb.sqlexpr._
import minidb.queryproc._

// CREATE TABLE foo (a TEXT, b INT, c DOUBLE);
QueryProc.processQuery(
  CreateTable("foo",
              List(("a", DBTypeText),
                   ("b", DBTypeInt)
                   ("c", DBTypeDouble)),
              List()))

// INSERT INTO foo VALUES (("qwer", 3, 1.5), ("tyui", 5, 4.3),
//                         ("abc", 9, 9.5), ("def", 7, 8.3));
QueryProc.processQuery(
  InsertValues("foo",
               List(List(DBString("qwer"), DBInt(3), DBDouble(1.5)),
                    List(DBString("tyui"), DBInt(5), DBDouble(4.3)),
                    List(DBString("abc"), DBInt(9), DBDouble(9.5)),
                    List(DBString("def"), DBInt(7), DBDouble(8.3)))))

// CREATE INDEX ON foo (b)
QueryProc.processQuery(CreateIndex("", "", "foo", List("b")))

// SELECT * FROM foo
val result1 = QueryProc.processQuery(SimpleSelect(List("foo"), CTrue))
result1.get.printResult

// SELECT * FROM foo WHERE b > 6
val result2 = QueryProc.processQuery(
  SimpleSelect(List("foo"),
               CCompare(VField("", "b"),
                        VConstant(DBInt(6)),
                        CGreater)))
result2.get.printResult

// SELECT * FROM foo WHERE b < c
val result3 = QueryProc.processQuery(
  SimpleSelect(List("foo"),
               CCompare(VField("", "b"),
                        VField("", "c"),
                        CLess)))
result3.get.printResult

