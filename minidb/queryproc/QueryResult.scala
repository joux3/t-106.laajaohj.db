package minidb.queryproc
import minidb.sqlexpr.DBValue
import minidb.storage.DBRow
import minidb.storage.Table

/** Stores the result of a SELECT query */
class QueryResult(val columns: Seq[String], val values: Seq[DBRow]) {
  /** The table where all values are currently stored
   * This is used for deletion only.
   */
  var table: Option[Table] = None

  /** Makes a QueryResult from some rows of an existing table */
  def this(table: Table, values: Seq[DBRow]) {
    this(table.columnNames, values)
    this.table = Some(table)
  }

  /** Deletes all rows that are stored in this QueryResult
   * from their tables and indexes. Only works if table is set.
   */
  def deleteAllRows() {
    if (table.isEmpty)
      throw new QueryError("QueryResult.deleteAllRows failed because table is unset")
    val thetable = table.get
    values foreach { v => thetable.delete(v) }
  }

  def csvPrintResult {
    val header = new StringBuilder
    columns.addString(header, ";")
    println(header.toString)
    values foreach { v =>
      val row = new StringBuilder
      v map {_.valueString} addString(row, ";")
      println(row.toString)
    }
  }
  def printResult = csvPrintResult
}
