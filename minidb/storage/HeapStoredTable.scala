package minidb.storage
import minidb.sqlexpr._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

/** A table that stores its data in an unordered array
 * (or "a heap of rows")
 */
class HeapStoredTable(columns: Seq[(String, DBType)],
                      constraints: Seq[TableConstraint]) extends Table(columns, constraints) {
  // Stores the rows unordered in this ArrayBuffer
  protected val contents = ArrayBuffer[DBRow]()

  def doInsert(row: DBRow) {
    contents += row
  }

  def allRows = contents
}

