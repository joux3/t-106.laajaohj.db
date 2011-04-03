package minidb.tests
import minidb.queryproc._
import minidb.sqlexpr._

object Testsqlexpr extends RunnableTest {
  def values() {
    Test.startTestSet("DBValue")

    Test.assertEquals("DBBoolean value true", DBBoolean(true).v, true)
    Test.assertEquals("DBBoolean value false", DBBoolean(false).v, false)
    Test.assertEquals("DBInt value", DBInt(123).v, 123)
    Test.assertEquals("DBDouble value", DBDouble(123.0).v, 123.0)
    Test.assertEquals("DBString value", DBString("abc").v, "abc")

    Test.assertEquals("DBInt valueString", DBInt(123).valueString, "123")
    Test.assertEquals("DBString valueString",
                      DBString("abc").valueString, "abc")
    
    Test.finishTestSet()
  }

  def types() {
    Test.startTestSet("DBType")

    Test.assertTrue("DBTypeBoolean accepts DBBoolean",
                    DBTypeBoolean valueOk DBBoolean(false))
    Test.assertTrue("DBTypeBoolean does not accept DBInt",
                    !(DBTypeBoolean valueOk DBInt(1)))

    Test.assertTrue("DBTypeText accepts DBString",
                    DBTypeText valueOk DBString("abc"))

    Test.finishTestSet()
  }

  def comparisons() {
    Test.startTestSet("DBValue comparisons")

    Test.assertTrue("DBBoolean true == true",
                    DBBoolean(true) == DBBoolean(true))
    Test.assertTrue("DBInt 1 == 1", DBInt(1) == DBInt(1))
    Test.assertTrue("DBString ab == ab", DBString("ab") == DBString("ab"))

    Test.assertTrue("DBInt 1 < 2", DBInt(1) < DBInt(2))
    Test.assertTrue("DBInt 1 <= 2", DBInt(1) <= DBInt(2))
    Test.assertTrue("DBInt 2 <= 2", DBInt(2) <= DBInt(2))
    Test.assertTrue("DBInt 1 > 2 false", !(DBInt(1) > DBInt(2)))

    Test.assertTrue("DBDouble 1 < 2", DBDouble(1.0) < DBDouble(2.0))

    Test.assertTrue("DBDouble 1  < DBInt 2", DBDouble(1.0) < DBInt(2))
    Test.assertTrue("DBInt 1 < DBDouble 2", DBInt(1) < DBDouble(2.0))

    Test.assertTrue("DBString aa < ab", DBString("aa") < DBString("ab"))
    Test.assertTrue("DBString ba > aa", DBString("ba") > DBString("aa"))

    Test.finishTestSet()
  }

  def conditions() {
    Test.startTestSet("ConditionExpr")

    Test.assertTrue("1 == 1 DBInt", CEquals.compareValues(DBInt(1), DBInt(1)))
    Test.assertTrue("1 < 2 DBInt", CLess.compareValues(DBInt(1), DBInt(2)))
    Test.assertTrue("2.0 <= 2.0 DBDouble",
                    CLessEq.compareValues(DBDouble(2.0), DBDouble(2.0)))

    Test.finishTestSet()
  }

  def fieldConstantEquals() {
    Test.startTestSet("getFieldConstantEquals")
    // nothing should match
    var where: ConditionExpr = CCompare(VField("a", "b"), VField("a", "c"), CEquals)
    val res = EvalCondition.getFieldConstantEquals(where)
    // work around possible scala bug:
    // (ArrayBuffer(), true) != (Seq(), true), but
    // (ArrayBuffer(5), true) == (Seq(5), true)
    Test.assertEquals("simple compare", res._1, Seq())
    Test.assertEquals("simple compare2", res._2, true)
    // ((a.b = 5) && (a.c = 8))
    where = CAnd(CCompare(VField("a", "b"), VConstant(DBInt(5)), CEquals), CCompare(VField("a", "c"), VConstant(DBInt(8)), CEquals))
    Test.assertEquals("simple AND case", EvalCondition.getFieldConstantEquals(where), 
                    (Seq( (VField("a", "b"), DBInt(5)), (VField("a", "c"), DBInt(8)) ), false))
    // (compare || compare) && (... = ...)
    where = CAnd(COr( CCompare(VField("a", "b"), VConstant(DBInt(5)), CLess), CCompare(VField("a", "c"), VConstant(DBDouble(5)), CEquals)), CCompare(VField("a", "d"), VConstant(DBInt(7)), CEquals))
    Test.assertEquals("complex case", EvalCondition.getFieldConstantEquals(where), 
                    (Seq( (VField("a", "d"), DBInt(7)) ), true))
    Test.finishTestSet()
  }

  def runTests() {
    values()
    types()
    comparisons()
    conditions()
    fieldConstantEquals()
  }
}
