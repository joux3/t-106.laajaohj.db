package minidb.storage
import minidb.sqlexpr.DBValue
import minidb.queryproc.QueryProcException
import scala.collection.mutable.ArrayBuffer

class IndexCreateFailed(msg: String) extends QueryProcException(msg)
class IndexSearchFailed(msg: String) extends QueryProcException(msg)
class IndexDeleteFailed(msg: String) extends QueryProcException(msg)
class IndexNotFound(msg: String) extends QueryProcException(msg)

/** Abstract superclass for index structures.
 * The index is a mapping from keys, i.e., the values of certain
 * columns (whose numbers are given in columnNums) to the row objects
 * (of type DBRow).
 * @param indexName a unique name given for the index
 * @param columnNums which columns of each row to use as index attributes
 */
abstract class Index(val indexName: String,
                     val columnNums: Seq[Int]) {
  /** Searches for exactly the given key.
   * @param key the key (values for the given columnNums) to look for
   * @return a sequence of all rows from the table with this key
   */
  def searchExact(key: DBKey): Seq[DBRow]
  /** Empties the index */
  def clear()
  /** Inserts a row given as key and data into the index
   * @param key the key (selected columns from data)
   * @param data the row object
   */
  def insert(key: DBKey, data: DBRow)
  /** Inserts the given row into the index.
   * Should usually not be overriden in subclasses (calls the other insert).
   * @param row the row to insert
   */
  def insert(row: DBRow) {
    insert(new DBKey(columnNums map { row(_) }), row)
  }
  /** Does this index support deletion (delete)?
   * Override this in the subclasses that implement the delete method.
   * If deletion is not supported, it is done very inefficiently using
   * rebuild.
   */
  def supportsDelete: Boolean = false
  /** Deletes a row from the index given its key.
   * @param key the key (selected columns from data)
   * @param data the row object to delete (currently stored in the index)
   */
  def delete(key: DBKey, data: DBRow) {
    throw new IndexDeleteFailed("Internal error: tried to delete from "+
                                "an index that does not support deletion: "+
                                this.getClass().toString)
  }
  /** Deletes the given row from the index.
   * Should usually not be overriden in subclasses (calls the other delete).
   * @param row the row to delete
   */
  def delete(row: DBRow) {
    delete(new DBKey(columnNums map { row(_) }), row)
  }
  /** Deletes all rows with the given key from the index.
   * Currently only used in Testindex.
   * @param key the key to delete
   */
  def delete(key: DBKey) {
    // map with identity to make a copy of the result, so that the
    // result of searchExact does not change while the deletions are
    // done (Seq.clone() is protected for some reason)
    val rows = searchExact(key).map {x=>x}
    rows foreach { row => delete(key, row) }
  }
  /** Does this index support range searches (searchRange)?
   * Override this in the subclasses that implement searchRange.
   */
  def supportsRangeSearch: Boolean = false
  /** Searches for the given range.
   * @param low minimum key of range (a key, or an empty sequence
   *            for no lower bound)
   * @param high maximum key of range (a key, or an empty sequence
   *             for no upper bound)
   * @param lowInclusive true to return keys that are == low
   * @param highInclusive true to return keys that are == high
   * @return a sequence of all rows that are included in the range
   */
  def searchRange(low: DBKey,
                  high: DBKey,
                  lowInclusive: Boolean,
                  highInclusive: Boolean): Seq[DBRow] =
    throw new IndexSearchFailed("Range search is not supported!")
  /** Rebuilds the index using the data provided.
   * The default implementation calls clear and inserts everything one by
   * one; override as necessary.
   * @param rows all rows in the table
   */
  def rebuild(rows: Seq[DBRow]) {
    clear()
    rows foreach { insert(_) }
  }
}

object Index {
  // default to using rbtree because it can support range queries
  val defaultIndexType = "rbtree"
  val allIndexes: ArrayBuffer[Index] = ArrayBuffer()

  /** Creates a new (empty) index.
   * Meant to be called from inside Table.createIndex.
   * @param indexName a suggested name of the index (it will be made unique)
   * @param indexType the type of index or Index.defaultIndexType
   * @param columnNums which columns of each table row to index by
   * @returns the created index
   */
  def create(indexName: String,
             indexType: String,
             columnNums: Seq[Int]): Index = {
    var realIndexName = indexName
    val existingNames = allIndexes map { _.indexName }
    while (existingNames contains realIndexName) {
      realIndexName += "*"
    }
    val index = indexType match {
      case "primitivehash" =>
        new PrimitiveHashIndex(realIndexName, columnNums)
      case "hash" => 
        new HashIndex(realIndexName, columnNums)
      case "rbtree" =>
        new RedBlackTreeIndex(realIndexName, columnNums)
     case "avltree" =>
        new AvlTreeIndex(realIndexName, columnNums)
     case "bplustree" =>
        new BPlusTreeIndex(realIndexName, columnNums)
      case _ => 
        throw new IndexCreateFailed("Unknown index type: "+indexType)
    }
    allIndexes += index
    index
  }
}
