package minidb.storage
import minidb.sqlexpr._
import minidb.queryproc.QueryProcException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class TableNotFound(msg: String) extends QueryProcException(msg)
class CreateTableFailed(msg: String) extends QueryProcException(msg)
class InsertFailed(msg: String) extends QueryProcException(msg)

/** Abstract class for sequences of DBValues */
abstract class DBSeq(v: Seq[DBValue]) extends Seq[DBValue] with Ordered[DBSeq] {
  def length = v.length
  //def elements = v.elements
  def apply(i: Int) = v.apply(i)
  def iterator = v.iterator

  /* compares by comparing each individual parts of sequence */
  def compare(that: DBSeq) =
    this.zip(that).foldLeft(0) {
      (memo, c) => if (memo != 0) memo else c._1.compare(c._2)
    }
}

/** Class that holds DBValues of a row */
class DBRow(v: Seq[DBValue]) extends DBSeq(v) {
}

/** Class that holds DBValues of a key */
class DBKey(v: Seq[DBValue]) extends DBSeq(v) {
}

/** Abstract superclass for tables */
abstract class Table(columns: Seq[(String, DBType)],
                     constraints: Seq[TableConstraint]) {
  /** The names of the columns of this table */
  val columnNames = columns map {_._1}
  /** The types of the columns of this table */
  val columnTypes = columns map {_._2}
  /** A sequence of indexes created for this table */
  val indexes = new ArrayBuffer[Index]()

  /** Creates a new index for this table */
  def createIndex(indexName: String,
                  indexType: String,
                  indexColumns: Seq[String]) {
    val columnNums = indexColumns map { name =>
      columnNames findIndexOf { _ == name } }
    if (columnNums contains -1)
      throw new IndexCreateFailed("Unknown column names for index "+
                                  indexName+": "+indexColumns)
    val index = Index.create(indexName, indexType, columnNums)
    index.rebuild(allRows)
    indexes += index
  }

  /** Checks that row has values whose types match the columns of this table */
  def checkTypes(row: DBRow) {
    if (row.size != columnTypes.size)
      throw new InsertFailed("Invalid number of values in row: " + row.size)
    (columnTypes zip row) foreach { case (t, v) =>
      if (!t.valueOk(v))
        throw new InsertFailed("Type error: value "+v+
                               " should be of type "+t)
    }
  }

  /** Checks that row satisfies any constraints of this table */
  def checkConstraints(row: DBRow) {
    constraints foreach { c =>
      c match {
        case TCPrimaryKey(pkcols) => {
          // XXX should check that the columns in the primary key are
          // unique and throw new InsertFailed if not
          ()
        }
      }
    }
  }

  /** Inserts a new row into this table.
   * Checks the types and constraints of the given values.
   * Calls doInsert for the actual insertion, and then
   * inserts the row to all indexes.
   */
  def insert(row: DBRow) {
    checkTypes(row)
    checkConstraints(row)
    doInsert(row)
    indexes foreach { _.insert(row) }
  }

  /** Actually inserts a new row into this table.
   * The row has already been checked to be correct.
   */
  protected def doInsert(row: DBRow)

  /** Returns a sequence of all the rows in this table (in any order) */
  def allRows: Seq[DBRow]
}

/** Stores the list of created tables.
 * Used to find and create tables.
 */
object Table {
  val allTables: HashMap[String, Table] = HashMap()

  /** Finds a table given its name */
  def find(name: String): Table =
    allTables.get(name) match {
      case Some(t) => t
      case None => throw new TableNotFound(name)
    }

  /** Creates a new table.
   * @param name name of table
   * @param ttype type of storage for the table (currently always "heap")
   * @param columns names and types of the columns in the table
   * @param constraints a list of constraints for the table
   */
  def create(name: String,
             ttype: String,
             columns: Seq[(String, DBType)],
             constraints: Seq[TableConstraint]): Table = {
    if (ttype != "heap")
      throw new CreateTableFailed("only table type \"heap\" is supported")
    val table = new HeapStoredTable(columns, constraints)
    allTables += (name -> table)
    table
  }
}
