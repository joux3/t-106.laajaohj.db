package minidb.tests
import minidb.sqlparse._
import minidb.sqlexpr._

object Testparser extends RunnableTest {
  def createTable() {
    Test.startTestSet("parsing create table")
    Test.assertEquals("simple table",
      Parser.parse("CREATE TABLE foo (a TEXT, b INT, c DOUBLE);"),
      CreateTable("foo",
            List(("a", DBTypeText),
                 ("b", DBTypeInt),
                 ("c", DBTypeDouble)),
            List()))
    Test.finishTestSet()
  }

  def insert() {
    Test.startTestSet("parsing insert into")
    Test.assertEquals("insert one row #1",
      Parser.parse("INSERT INTO foo VALUES (1, 2.2, \"c\", True);"),
      InsertValues("foo", List(List(DBInt(1), DBDouble(2.2), DBString("c"), DBBoolean(true)))))
      
    Test.assertEquals("insert one row #2",
      Parser.parse("INSERT INTO test VALUES (1448, 50, \"Holy See (Vatican City State)\", \"Blair Moon\");"),
      InsertValues("test", List(List(DBInt(1448), DBInt(50), DBString("Holy See (Vatican City State)"), DBString("Blair Moon")))))
      
    Test.assertEquals("insert one row #3",
      Parser.parse("INSERT INTO test VALUES (2321, 97, \"Korea, Republic of\", \"Madonna Hanson\");"),
      InsertValues("test", List(List(DBInt(2321), DBInt(97), DBString("Korea, Republic of"), DBString("Madonna Hanson")))))

    /*Test.assertEquals("insert four rows",
      Parser.parse("INSERT INTO foo VALUES ((\"qwer\", 3, 1.5), "+
                   "(\"tyui\", 5, 4.3), (\"abc\", 9, 9.5), "+
                   "(\"def\", 7, 8.3));"),
      InsertValues("foo",
                   List(List(DBString("qwer"), DBInt(3), DBDouble(1.5)),
                        List(DBString("tyui"), DBInt(5), DBDouble(4.3)),
                        List(DBString("abc"), DBInt(9), DBDouble(9.5)),
                        List(DBString("def"), DBInt(7), DBDouble(8.3)))))
    */
    Test.finishTestSet()
  }

  def simpleSelect() {
    Test.startTestSet("simple select queries")
    
    Test.assertEquals("select without where",
      Parser.parse("SELECT * FROM foo;"),
      SimpleSelect(List("foo"), CTrue))
    Test.assertEquals("nested comparisons in where",
      Parser.parse("SELECT * FROM foo WHERE b > 6 AND (c < 5 OR c > 8);"),
      SimpleSelect(List("foo"), 
                   CAnd(CCompare(VField("", "b"), VConstant(DBInt(6)), CGreater),
                        COr(CCompare(VField("", "c"), VConstant(DBInt(5)), CLess),
                            CCompare(VField("", "c"), VConstant(DBInt(8)), CGreater)))))
    Test.finishTestSet()
  }
  
  def createIndex() {
    Test.startTestSet("parsing create index statements")
    
    Test.assertEquals("without indexname and indextype", 
      Parser.parse("CREATE INDEX ON tablename (column1, column2, column3)"),
      CreateIndex("", "", "tablename", List("column1", "column2", "column3")))
    
    Test.assertEquals("with indexname but without indextype",
      Parser.parse("CREATE INDEX indexname ON tablename (column1, column2, column3)"),
      CreateIndex("indexname", "", "tablename", List("column1", "column2", "column3")))
    
    Test.assertEquals("without indexname but with indextype",
      Parser.parse("CREATE INDEX USING indextype ON tablename (column1, column2, column3)"),
      CreateIndex("", "indextype", "tablename", List("column1", "column2", "column3")))
    
    Test.assertEquals("with indexname and indextype",
      Parser.parse("CREATE INDEX indexname USING indextype ON tablename (column1, column2, column3)"),
      CreateIndex("indexname", "indextype", "tablename", List("column1", "column2", "column3")))
    
    Test.finishTestSet()
  }
  
  def parseConditions() {
    Test.startTestSet("parsing conditions")
    
    Test.assertEquals("ordinary values", 
      Parser.conditionsToList("\"Testi\" = abc AND (12.456 <= cde) AND True"), 
      List(VConstant(DBString("Testi")), CEquals, VField("", "abc"), CAnd, "(", VConstant(DBDouble(12.456)), CLessEq, VField("", "cde"), ")", CAnd, CTrue))
    
    Test.assertEquals("parsing comparisons",
      Parser.parseComparisons(List(VConstant(DBString("Testi")), CEquals, VField("", "abc"), CAnd, "(", VConstant(DBDouble(12.456)), CLessEq, VField("", "cde"), ")", CAnd, CTrue)),
      List(CCompare(VConstant(DBString("Testi")), VField("", "abc"), CEquals), CAnd, "(", CCompare(VConstant(DBDouble(12.456)), VField("", "cde"), CLessEq), ")", CAnd, CTrue))
    
    Test.assertEquals("parsing condition string to ConditionExpr",
      Parser.parseConditions("\"Testi\" = abc AND (12.456 <= cde) AND True"),
      CAnd(CAnd(CCompare(VConstant(DBString("Testi")), VField("", "abc"), CEquals), CCompare(VConstant(DBDouble(12.456)), VField("", "cde"), CLessEq)), CTrue))
    Test.finishTestSet()
  }

  def dropIndex() {
    Test.startTestSet("dropping index")

    Test.assertEquals("parsing drop index",
      Parser.parse("DROP INDEX indexname ON tablename;"),
      DropIndex("indexname", "tablename"))

    Test.finishTestSet()
  }

  def runTests() {
    createTable()
    insert()
    simpleSelect()
    createIndex()
    parseConditions()
    dropIndex()
  }
}
