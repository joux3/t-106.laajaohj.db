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

  def runTests() {
    evalCondition1()
    evalCondition2()
  }
}
