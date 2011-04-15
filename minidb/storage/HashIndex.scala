package minidb.storage
import minidb.sqlexpr.DBValue
import minidb.queryproc.QueryProcException

class HashIndex(override val indexName: String, columnNums: Seq[Int]) extends Index(indexName, columnNums) {
  class HashTableEntry(val key: DBKey, var row: DBRow) {
    override def toString = {
      key.toString + " -> " + row.toString
    }
  }

  private var rowCount = 0
  private var currentTableSize = 32
  private var table = new Array[List[HashTableEntry]](currentTableSize)
  private var loadFactor = 0
  private val MAX_LOAD_FACTOR = 3


  def searchExact(key: DBKey): Seq[DBRow] = {
    val data = table(key.hashCode % currentTableSize)

    if (data == null) List()
    else {
      var rows:List[DBRow] = Nil

      for (t <- data) {
        if (t.key == key) {
          rows = t.row :: rows
        }
      }

      rows
    }
  }

  def clear() {
    table = new Array[List[HashTableEntry]](currentTableSize)
  }

  def insert(key: DBKey, data: DBRow) {
    val hash = key.hashCode % currentTableSize
    val entry = new HashTableEntry(key, data)
    rowCount += 1
    if (table(hash) == null) {
      table(hash) = List(entry)
    } else {
      table(hash) = entry :: table(hash) 
    }

    // XXX find new max load factor as the definition got changed
    if (rowCount / currentTableSize > MAX_LOAD_FACTOR) {
      resize
    }
  }

  override def supportsDelete: Boolean = true

  override def delete(key: DBKey, data: DBRow) {
    val hash = key.hashCode % currentTableSize
    val rows = table(hash)

    if (rows == null) throw new IndexDeleteFailed("tried to delete a nonexistent row!")
    else {
      val length = rows.length

      val index = rows.findIndexOf( r => r.key == key && r.row == data )

      if (index > -1) {
        if (index == 0) table(hash) = rows tail
        else if (index == rows.length - 1) table(hash) = rows dropRight 1
        else table(hash) = rows.drop(index) ::: rows.dropRight(length - index)
      } else {
        throw new IndexDeleteFailed("tried to delete a nonexistent row!")
      }
    }
  }

  override def rebuild(rows: Seq[DBRow]) {
    currentTableSize = scala.math.max(rows.size * 2, currentTableSize)
    clear()
    rows foreach { insert(_) }
  }

  private def resize {
    loadFactor = 0
    val oldTable = table
    currentTableSize *= 2
    table = new Array[List[HashTableEntry]](currentTableSize)

    oldTable.foreach { s => 
      if (s != null) {
        s.foreach { r => 
          insert(r.key, r.row)
        }
      }
    } 
  }
}
