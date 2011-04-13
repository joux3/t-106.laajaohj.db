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
  def insert(key: DBKey, data: DBRow) {
    theData.get(key) match {
      case Some(v) => v += data
      case None => theData += (key -> ArrayBuffer(data))
    }
  }
  override def supportsDelete = true
  override def delete(key: DBKey, data: DBRow) {
    theData.get(key) match {
      case Some(v) => {
        v -= data
        if (v.isEmpty) // this was the last item with this key
          theData -= key
      }
      case None =>
        throw new IndexDeleteFailed("tried to delete a nonexistent row!")
    }
  }
}

