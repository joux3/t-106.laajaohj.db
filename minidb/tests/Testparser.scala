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
    Test.assertEquals("insert one row",
      Parser.parse("INSERT INTO foo VALUES (1, 2.2, \"c\", True);"),
      InsertValues("foo", List(List(DBInt(1), DBDouble(2.2), DBString("c"), DBBoolean(true)))))

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
    /*
    Test.assertEquals("one comparison in where",
      Parser.parse("SELECT * FROM foo WHERE b > 6;"),
      SimpleSelect(List("foo"), 
                   CCompare(VField("", "b"),
                            VConstant(DBInt(6)),
                            CGreater)))
    */
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

  def runTests() {
    createTable()
    insert()
    simpleSelect()
    createIndex()
  }
}
