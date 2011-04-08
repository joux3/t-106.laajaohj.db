package minidb.storage
import minidb.sqlexpr.DBValue
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

class PrimitiveHashIndex(override val indexName: String,
                         columnNums: Seq[Int]) extends Index(indexName,
                                                             columnNums) {
  val theData: HashMap[DBKey, ArrayBuffer[DBRow]] = HashMap()

  def searchExact(key: DBKey): Seq[DBRow] = {
    val result = theData.getOrElse(key, ArrayBuffer())
    if (result.isEmpty) List()
    else result
  }
  def clear() {
    theData.clear()
  }
  override def delete(key: DBKey) = {
    theData.remove(key)
  }
  def insert(key: DBKey, data: DBRow) {
    theData.get(key) match {
      case Some(v) => v += data
      case None => theData += (key -> ArrayBuffer(data))
    }
  }
  override def supportsDelete: Boolean = true
}

