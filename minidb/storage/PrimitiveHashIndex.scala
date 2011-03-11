package minidb.storage
import minidb.sqlexpr.DBValue
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

class PrimitiveHashIndex(override val indexName: String,
                         columnNums: Seq[Int]) extends Index(indexName,
                                                             columnNums) {
  val theData: HashMap[Seq[DBValue], ArrayBuffer[Seq[DBValue]]] = HashMap()
  def searchExact(key: Seq[DBValue]): Seq[Seq[DBValue]] = {
    val result = theData(key)
    if (result.isEmpty) List()
    else result
  }
  def clear() {
    theData.clear()
  }
  def insert(key: Seq[DBValue], data: Seq[DBValue]) {
    theData.get(key) match {
      case Some(v) => v += data
      case None => theData += (key -> ArrayBuffer(data))
    }
  }
}

