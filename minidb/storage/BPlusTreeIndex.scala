package minidb.storage
import scala.collection.mutable.ArrayBuffer

/** B+ tree index
  * handles multiples with ArrayBuffer
  * order = maximum number of children in nodes & values in leaves
  * TODO: deletion */

class BPlusTree(val order: Int) {
  if (order < 4) throw new
                 RuntimeException("Order of BPlusTree must be >= 4")
  // this is to make sure we can divide a full node into legal
  // nodes without adding any keys/values
  if (order % 2 != 0) throw new
                      RuntimeException("Order of BPlusTree must be divisible by 2")
  
  object SearchType extends Enumeration {
    type SearchType = Value
    val Exact, Bigger, BiggerOrEqual = Value
  }
  import SearchType._
  
  abstract class Node(nKeysMax: Int) {
    // in a leaf node, key is the key of the corresponding value
    // in an inner node, key is the smallest key in right subtree
    val keys = new Array[DBKey](nKeysMax)
    var nKeys: Int = 0
    
    def full() = (nKeys == nKeysMax)

    //** returns index of:
    // * given key or -1 if not found (if searchType == Exact)
    // * first key that is >= than given key (if searchType == BiggerOrEqual)
    // * first key that is > than given key (if searchType == Bigger)
    def findIndexOfKey(key: DBKey, searchType: SearchType) : Int = {
      var min = 0; var max = nKeys - 1
      while (min < max) {
        val mid = min + (max - min) / 2
        if (keys(mid) > key) max = mid - 1
        else if (keys(mid) < key) min = mid + 1
        else { min = mid; max = mid }
      }
      if (searchType == Bigger) {
          if (min < nKeys && keys(min) <= key) min+1 else min
      } else if (searchType == BiggerOrEqual) {
          if (min < nKeys && keys(min) < key) min+1 else min
      } else /* (searchType == Exact) */ {
          if (min < nKeys && keys(min) == key) min else -1
      }
    }
  }

  class InnerNode extends Node(order-1) {
    val children = new Array[Node](order)
  }
  
  class LeafNode extends Node(order) {
    val values = new Array[ArrayBuffer[DBRow]](order)
    var nextLeaf: LeafNode = null
  }
  
  private var root: Node = new LeafNode

  //** debug printing functions
  def f(path: Seq[Int]) : Node = { // find given node
    var node: Node = root
    path foreach (i => node = node.asInstanceOf[InnerNode].children(i))
    node
  }
  
  def pk(path: Seq[Int]) { // print keys of a node
    var node = f(path)
    for (i <- 0 to node.nKeys-1) print(node.keys(i).toString() + " ")
  }
  
  def pv(path: Seq[Int]) { // print values of a leaf node
    var node = f(path).asInstanceOf[LeafNode]
    for (i <- 0 to node.nKeys-1) print(node.values(i).toString() + " ")
  }

  def shiftArrayElems[T](array: Array[T], start: Int, end: Int, direction: Int) {
    var range = if (direction > 0) (end to start by -1) else (start to end)
    for (i <- range) array(i + direction) = array(i)
  }

  def clear() { root = new LeafNode }

  //** split given child; the child must be full, parent must not be full
  private def splitChild(parent: InnerNode, index: Int) {
    val child = parent.children(index)

    // shift keys and children in parent to make room for the new child
    shiftArrayElems(parent.keys, index, parent.nKeys - 1, 1)
    shiftArrayElems(parent.children, index+1, parent.nKeys, 1)

    if (child.isInstanceOf[InnerNode]) {
      val child = parent.children(index).asInstanceOf[InnerNode]
      // create new child
      val newChild = new InnerNode
      for (i <- 0 to order/2 - 2) newChild.keys(i) = child.keys(order/2 + i)
      newChild.nKeys = order/2 - 1
      for (i <- 0 to order/2 - 1) newChild.children(i) = child.children(order/2 + i)
      // add new child and corresponding key to parent
      parent.keys(index) = child.keys(order/2 - 1)
      parent.nKeys += 1
      parent.children(index + 1) = newChild
      // fix split child
      child.nKeys = order/2 - 1
    }

    else {
      val child = parent.children(index).asInstanceOf[LeafNode]
      // create new child
      val newChild = new LeafNode
      for (i <- 0 to order/2 - 1) newChild.keys(i) = child.keys(order/2 + i)
      newChild.nKeys = order/2
      for (i <- 0 to order/2 - 1) newChild.values(i) = child.values(order/2 + i)
      newChild.nextLeaf = child.nextLeaf
      // add new child and corresponding key to parent
      parent.keys(index) = newChild.keys(0)
      parent.nKeys += 1
      parent.children(index + 1) = newChild
      // fix split child
      child.nKeys = order/2
      child.nextLeaf = newChild
    }
  }

  //** find leaf node where the key should be if it existed
  private def findLeafNode(key: DBKey) : LeafNode = {
    var node = root
    while (!(node.isInstanceOf[LeafNode]))
      node = node.asInstanceOf[InnerNode].children(node.findIndexOfKey(key, Bigger))
    node.asInstanceOf[LeafNode]
  }

  def search(key: DBKey) : Seq[DBRow] = {
    val node = findLeafNode(key)
    val index = node.findIndexOfKey(key, Exact)
    if (index == -1) Seq[DBRow]() else node.values(index)
  }

  def searchRange(low: DBKey, high: DBKey, lowIncl: Boolean, 
                           highIncl: Boolean): Seq[DBRow] = {
    // special case ???
    if (low == high && (lowIncl || highIncl)) return search(low)
    
    val result = new ArrayBuffer[DBRow]
    var node = findLeafNode(low)
    var index = if (lowIncl) node.findIndexOfKey(low, BiggerOrEqual)
                else node.findIndexOfKey(low, Bigger)
    if (index >= node.nKeys) { node = node.nextLeaf; index = 0 }
    val compareF = if (highIncl) { (x: DBKey) => x<=high } else { (x: DBKey) => x<high }
    
    while (node != null && compareF(node.keys(index))) {
      result ++= node.values(index)
      index += 1
      if (index >= node.nKeys) { node = node.nextLeaf; index = 0 }
    }
    result
  }

  def insert(key: DBKey, value: DBRow) {
    // travel down the tree, splitting full nodes on the way
    var node = root
    if (root.full()) {
      root = new InnerNode
      val rootNode = root.asInstanceOf[InnerNode]
      rootNode.children(0) = node
      splitChild(rootNode, 0)
      if (rootNode.keys(0) > key) node = rootNode.children(0)
      else node = rootNode.children(1)
    }
    
    while (!node.isInstanceOf[LeafNode]) {
      val currNode = node.asInstanceOf[InnerNode]
      val index = currNode.findIndexOfKey(key, Bigger)
      if (!currNode.children(index).full())
        node = currNode.children(index)
      else {
        splitChild(currNode, index)
        if (currNode.keys(index) > key) node = currNode.children(index)
        else node = currNode.children(index+1)
      }
    }
    
    val leafNode = node.asInstanceOf[LeafNode]
    val insIndex = leafNode.findIndexOfKey(key, BiggerOrEqual)
    if (insIndex < leafNode.nKeys && leafNode.keys(insIndex) == key)
      leafNode.values(insIndex) += value
    else {
      shiftArrayElems(leafNode.keys, insIndex, leafNode.nKeys - 1, 1)
      shiftArrayElems(leafNode.values, insIndex, leafNode.nKeys - 1, 1)
      leafNode.keys(insIndex) = key
      leafNode.values(insIndex) = new ArrayBuffer[DBRow]
      leafNode.values(insIndex) += value
      leafNode.nKeys += 1
    }
  }
}

class BPlusTreeIndex(override val indexName: String,
                 columnNums: Seq[Int]) extends Index(indexName,
                                                     columnNums) {
  override def supportsRangeSearch = true
  private val order = 16
  private val bPlusTree = new BPlusTree(order)
  
  def clear() { bPlusTree.clear() }
  def searchExact(key: DBKey): Seq[DBRow] = bPlusTree.search(key)  
  override def searchRange(low: DBKey, high: DBKey, lowInclusive: Boolean, 
                           highInclusive: Boolean): Seq[DBRow] =
    bPlusTree.searchRange(low, high, lowInclusive, highInclusive)

  def insert(key: DBKey, data: DBRow) { bPlusTree.insert(key, data) }
}
