package minidb.storage
import minidb.sqlexpr.DBValue
import minidb.queryproc.QueryProcException

class HashIndex(override val indexName: String, columnNums: Seq[Int]) extends Index(indexName, columnNums) {
	private var currentTableSize = 32
	private var table = new Array[Seq[Tuple2[DBKey, DBRow]]](currentTableSize)
  private var loadFactor = 0
  private val MAX_LOAD_FACTOR = 3

	def searchExact(key: DBKey): Seq[DBRow] = {
    val data = table(key.hashCode % currentTableSize)
    
    if (data == null) List()
    else {
      var rows:List[DBRow] = Nil

      for (t <- data) rows +: t._2

      rows
    }
	}

	def clear() {
    table = new Array[Seq[Tuple2[DBKey, DBRow]]](currentTableSize)
	}

	def insert(key: DBKey, data: DBRow) {
    val hash = key.hashCode % currentTableSize

    if (table(hash) == null) {
      table(hash) = List((key, data))
    } else {
      (key,data) +: table(hash)
      val l = table(hash).length
      if (l > loadFactor) {
        loadFactor = l
        if (loadFactor > MAX_LOAD_FACTOR)
          resize
      }
    }
	}

  private def resize {
    loadFactor = 0
    val oldTable = table
    currentTableSize *= 2
    table = new Array[Seq[Tuple2[DBKey, DBRow]]](currentTableSize)
    
    oldTable.foreach { s => s.foreach { r => 
        insert(r._1, r._2)
      }
    } 
  }
}
