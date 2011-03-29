package minidb.storage
import minidb.sqlexpr.DBValue
import minidb.queryproc.QueryProcException

class HashIndex(override val indexName: String, columnNums: Seq[Int]) extends Index(indexName, columnNums) {
  class HashTableEntry(val key: DBKey, var row: DBRow) {
    override def toString = {
      key.toString + " -> " + row.toString
    }
  }

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
          rows = rows ::: List(t.row)
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

    if (table(hash) == null) {
      table(hash) = List(entry)
    } else {
      table(hash) = table(hash) ::: List(entry)
      val l = table(hash).length
      if (l > loadFactor) {
        loadFactor = l
        if (loadFactor > MAX_LOAD_FACTOR)
          resize
      }
    }
  }

  def rebuild(rows: Seq[DBRow]) {
    currentTableSize = rows.size * 2
    clear()
    rows foreach { insert(_) }
  }

  private def resize {
    loadFactor = 0
    val oldTable = table
    currentTableSize *= 2
    table = new Array[List[HashTableEntry]](currentTableSize)
    
    oldTable.foreach { s => s.foreach { r => 
        insert(r.key, r.row)
      }
    } 
  }
}
