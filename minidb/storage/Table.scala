package minidb.storage
import minidb.sqlexpr._
import minidb.queryproc.EvalCondition
import minidb.queryproc.QueryProcException
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class TableNotFound(msg: String) extends QueryProcException(msg)
class CreateTableFailed(msg: String) extends QueryProcException(msg)
class InsertFailed(msg: String) extends QueryProcException(msg)

/** Abstract class for sequences of DBValues */
abstract class DBSeq(v: Seq[DBValue]) extends Seq[DBValue] with Ordered[DBSeq] {
  def length = v.length
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
  /** Hash key by hashing together the hashes of the individual DBValues
   * using the Shift-Add-Xor hashing algorithm
   */
  override def hashCode: Int = {
    var h = 0

    v.foreach(i =>  h ^= (h << 5) + (h >> 2) + i.hashCode)

    // We need an unsigned int, and since Java and therefore Scala don't offer
    // that built in, we need to mask off the sign bit to get around the issue
    (h & 0x7fffffff)
  }
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

  /** Removes the index with the given name 
    * returns the deleted index */
  def dropIndex(indexName: String) = {
    val i = indexes.find {x => x.indexName == indexName }
    i match {
      case Some(index) => {
        indexes -= index
        Index.allIndexes -= index
      }
      case None => {
        throw new IndexNotFound("No index named \""+indexName+"\"!")
      }
    }
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
  def checkConstraints(row: DBRow): DBRow = {
    var checkedRow = row
    constraints foreach { c =>
      c match {
        case TCPrimaryKey(pkcols) => {
          // checks that the PRIMARY KEY columns are
          // unique and throws new InsertFailed if not
          val i = indexes.find { x => x.indexName.startsWith("_primarykey_") }
          val key = new DBKey(i.get.columnNums map { row(_) })
          if (!i.get.searchExact(key).isEmpty) 
            throw new InsertFailed("Inserted rows PRIMARY KEY is not unique.")              
        } 
	case TCNotNull(nncols) => { 
	  // checks that the values in NOT NULL columns are not NULL
	  // if are throws new InsertFailed
	  nncols foreach { col =>
            val colnum = columnNames.indexOf(col)
            if (row(colnum) == null)
              throw new InsertFailed("NOT NULL column is NULL.")
          }
	}

	case TCUnique(unicols) => { 
          val i = indexes.find { x => x.indexName.startsWith("_unique_" + unicols.toString) }
          val key = new DBKey(i.get.columnNums map { row(_) })
	  if (!i.get.searchExact(key).isEmpty) 	  
	    throw new InsertFailed("Inserted rows UNIQUE key is not unique.")                         
        }
  
        // sets DEFAULT keys to default value, if they are null
        case TCDefault(defcols, defvalues) => { 
          defcols foreach { col =>
            val colnum = columnNames.indexOf(col)
            if (row(colnum) == null)  
	      checkedRow = new DBRow(checkedRow.updated(colnum, defvalues(defcols.indexOf(columnNames(colnum)))))           
          }
        }
        case TCCheck(conditions) => {
          if (EvalCondition.eval(conditions, columnNames.map{("", _)}, row) != DBBoolean(true))
            throw new InsertFailed("Inserted row doesn't fulfill CHECK constraint.")
        }
        case TCForeignKey(columns, reftablename, refcolumns) => { //XXX needs tests
          val refTable = Table.find(reftablename)
          val i = refTable.indexes.find { x => x.indexName.startsWith("_primarykey_") }
          val refColumns = columns map { name => // columnnumbers for the FOREIGN KEY columns
            columnNames findIndexOf { _ == name } }
          val key = new DBKey(refColumns map { row(_) })
          if (i.get.searchExact(key).isEmpty) 	  
	    throw new InsertFailed("Inserted rows FOREIGN KEY not found in REFERENCED tables PRIMARY KEY.")                         
        }
      }  
    }
    checkedRow
  }
  
    
  

  /** Inserts a new row into this table.
   * Checks the types and constraints of the given values.
   * Calls doInsert for the actual insertion, and then
   * inserts the row to all indexes.
   */
  def insert(row: DBRow) {
    checkTypes(row)
    val checkedRow = checkConstraints(row)
    doInsert(checkedRow)
    indexes foreach { _.insert(checkedRow) }
  }

  /** Deletes a single row from this table.
   * The given row object must be currently stored in the table.
   * Also deletes the row from all indexes.
   */
  def delete(row: DBRow) {
    doDelete(row)
    indexes foreach { i =>
      if (i.supportsDelete)
        i.delete(row) 
      else // fall back to rebuilding the whole index
        i.rebuild(allRows)
    }
  }

  /** Returns indexes that have atleast one of the given columns
   *  as its key
   * 
   *  Returns the indexes that can be used, sorted by the number of 
   *  common columns in index & fields
   */
  def getUsableIndexes(fields: Seq[VField]): Seq[Index] = {
    // XXX doesn't check if VField.table matches the current one!
    val columnNums = new ArrayBuffer[Int]
    fields.foreach {field => 
      // check that this table has this field
      if (columnNames.contains(field.fieldname)) {
        columnNums += columnNames.indexOf(field.fieldname)
      }
    }

    if (columnNums.isEmpty) {
      // no way to give out an index to use
      Seq()
    } else {       
      // pick those indexes which have all of their columns in
      // the local columnNums
      val usableIndexes = indexes.filter {index =>
        index.columnNums.forall {num => columnNums.contains(num)}
      }
      // sort by the number of common columns
      usableIndexes.sortWith {(a, b) =>
        a.columnNums.size > b.columnNums.size
      }
      usableIndexes
    }
  }

  // returns number of column that matches the given field
  // XXX doesn't check if VField even matches this table
  def getColumnNum(field: VField): Int = {
    columnNames.indexOf(field.fieldname)
  }

  /** Actually inserts a new row into this table.
   * The row has already been checked to be correct.
   */
  protected def doInsert(row: DBRow)

  /** Actually deletes a row from this table. */
  protected def doDelete(row: DBRow)

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
	
  def saveToFile(){
    //ToDo: Table saveToFile not implemented
  }
  def loadFromFile(){
    //ToDo: Table loadFromFile not implemented
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
    constraints foreach { c => 
      c match { 
          case TCForeignKey(columns, reftablename, refcolumns) => {
            if (columns.size != refcolumns.size)
              throw new CreateTableFailed("Number of FOREIGN KEY columns is not equal to REFERENCED columns.")

            val refTable = Table.find(reftablename)
            val i = refTable.indexes.find { x => x.indexName.startsWith("_primarykey_") }
            i match {
              case Some(index) => { 
                val refColumnNums = refcolumns map { name => 
                  refTable.columnNames findIndexOf { _ == name } }
                if (!refColumnNums.sameElements(index.columnNums))
                  throw new CreateTableFailed("REFERENCED columns are not REFERENCED tables PRIMARY KEY.")
              }
              case None => { 
                throw new CreateTableFailed("REFERENCED table has no PRIMARY KEY.")
              }
            }
          }
          case _ =>
        }
      }
    val table = new HeapStoredTable(columns, constraints)
    allTables += (name -> table)
    table
  }
}
