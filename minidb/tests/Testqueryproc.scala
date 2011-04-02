package minidb.tests
import minidb.queryproc._
import minidb.sqlexpr._

object Testqueryproc extends RunnableTest {
  def evalCondition1() {
    Test.startTestSet("EvalCondition empty env")

    val emptyEnv = new Env(List(), List())

    Test.assertEquals("CTrue", EvalCondition.eval(CTrue, emptyEnv),
                      DBBoolean(true))
    Test.assertEquals("CFalse", EvalCondition.eval(CFalse, emptyEnv),
                      DBBoolean(false))

    Test.assertEquals(
      "(true && false) && true",
      EvalCondition.eval(CAnd(CAnd(CTrue, CFalse), CTrue),
                         emptyEnv),
      DBBoolean(false))

    Test.assertEquals("1 < 2",
                      EvalCondition.eval(CCompare(VConstant(DBInt(1)),
                                                  VConstant(DBInt(2)),
                                                  CLess),
                                         emptyEnv),
                      DBBoolean(true))

    Test.finishTestSet()
  }

  def evalCondition2() {
    Test.startTestSet("EvalCondition with env")

    val simpleEnv = new Env(List(("a","b"), ("a", "c"), ("d", "b")),
                            List(DBInt(3), DBInt(4), DBInt(5)))

    Test.assertEquals("a.b == 3",
                      EvalCondition.eval(CCompare(VField("a", "b"),
                                                  VConstant(DBInt(3)),
                                                  CEquals),
                                         simpleEnv),
                      DBBoolean(true))

    Test.assertEquals("c == 4",
                      EvalCondition.eval(CCompare(VField("", "c"),
                                                  VConstant(DBInt(4)),
                                                  CEquals),
                                         simpleEnv),
                      DBBoolean(true))

    Test.assertEquals("d.b > 3",
                      EvalCondition.eval(CCompare(VField("d", "b"),
                                                  VConstant(DBInt(3)),
                                                  CGreater),
                                         simpleEnv),
                      DBBoolean(true))

    Test.assertAnyException(
      "c.c not found",
      EvalCondition.eval(CCompare(VField("c","c"),
                                  VConstant(DBInt(4)),
                                  CEquals),
                         simpleEnv))

    Test.finishTestSet()
  }

  def selectTest() {
    Test.startTestSet("Table queries")
    // CREATE TABLE foo (0 INT, 1 TEXT);
    QueryProc.processQuery(CreateTable("foo", Seq(("0", DBTypeInt), ("1", DBTypeText)), Seq()))
    for (i <- 0 to 100) {
      // INSERT INTO foo VALUES (i, i.toString)
      QueryProc.processQuery(InsertValues("foo", Seq(Seq(DBInt(i), DBString(i.toString)))))
    }
    // test SELECT without index
    val select = SimpleSelect(Seq("foo"), CCompare(VField("", "0"), VConstant(DBInt(87)), CEquals))
    val r1 = QueryProc.processQuery(select)
    Test.assertTrue("select without index", QueryProc.filteredRowCount == 101)
    QueryProc.processQuery(CreateIndex("index", "", "foo", Seq("0")))
    val r2 = QueryProc.processQuery(select)
    Test.assertTrue("select with index", QueryProc.filteredRowCount == 1)
    Test.assertEquals("select results", r1.get.values, r2.get.values)
    QueryProc.processQuery(DropIndex("index", "foo"))
    val r3 = QueryProc.processQuery(select)
    Test.assertTrue("select with removed index", QueryProc.filteredRowCount == 101)
    Test.assertEquals("select results", r1.get.values, r2.get.values)

    Test.finishTestSet()
  }

  def runTests() {
    evalCondition1()
    evalCondition2()
    selectTest()
  }
}
