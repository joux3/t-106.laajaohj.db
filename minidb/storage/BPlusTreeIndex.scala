package minidb.storage
import scala.collection.mutable.ArrayBuffer

/** B+ tree
  * handles multiples with ArrayBuffer
  * TODO: deletion, range search */

// order = maximum number of children in nodes & values in leaves
class BPlusTree(val order: Int) {
  if (order < 4) throw new
                 RuntimeException("Order of BPlusTree must be >= 4")
  // this is to simplify the implementation
  if (order % 2 != 0) throw new
                      RuntimeException("Order of BPlusTree must be divisible by 2")
  
  abstract class Node(nKeysMax: Int) {
    // in a leaf node, key represents the key of the corresponding value
    // in an inner node, key represents the smallest key in right subtree
    val keys = new Array[DBKey](nKeysMax)
    var nKeys: Int = 0
    
    def full() = (nKeys == nKeysMax)
    
    //** returns index of given key or -1 if not found
    // TODO: implement binary search
    def findIndexOfExactKey(key: DBKey) : Int = {
      var index = 0
      while (index < nKeys && keys(index) < key) index += 1
      if (index < nKeys && keys(index) == key) index
      else -1
    }
    
    //** returns index of first key that is >= than given key
    // TODO: implement binary search and join with findexact?
    def findIndexOfKey(key: DBKey) : Int = {
      var index = 0
      while (index < nKeys && keys(index) < key) index += 1
      index
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

  def shiftArrayElems[T](array: Array[T], start: Int, end: Int, direction: Int) {
    var range = if (direction > 0) (end to start by -1) else (start to end)
    for (i <- range) array(i + direction) = array(i)
  }

  //** split given child; the child must be full, parent must not be full
  def splitChild(parent: InnerNode, index: Int) {
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

  def search(key: DBKey) : Seq[DBRow] = {
    var node = root
    while (!(node.isInstanceOf[LeafNode]))
      node = node.asInstanceOf[InnerNode].children(node.findIndexOfKey(key))
    val index = node.findIndexOfExactKey(key)
    if (index == -1) List() else node.asInstanceOf[LeafNode].values(index)
  }

  def insert(key: DBKey, value: DBRow) {
    // travel down the tree, splitting full nodes on the way
    var node: Node = root
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
      val index = currNode.findIndexOfKey(key)
      if (!currNode.children(index).full())
        node = currNode.children(index)
      else {
        splitChild(currNode, index)
        if (currNode.keys(index) > key) node = currNode.children(index)
        else node = currNode.children(index+1)
      }
    }
    
    val leafNode = node.asInstanceOf[LeafNode]
    val insIndex = leafNode.findIndexOfKey(key)
    if (leafNode.keys(insIndex) == key) leafNode.values(insIndex) += value
    else {
      shiftArrayElems(leafNode.keys, insIndex, leafNode.nKeys - 1, 1)
      shiftArrayElems(leafNode.values, insIndex, leafNode.nKeys - 1, 1)
      leafNode.keys(insIndex) = key
      leafNode.values(insIndex) += value
      leafNode.nKeys += 1
    }
  }
}

class BPlusTreeIndex(override val indexName: String,
                 columnNums: Seq[Int]) extends Index(indexName,
                                                     columnNums) {
  private val order = 16
  private var bPlusTree = new BPlusTree(order)
  
  def clear() { bPlusTree = new BPlusTree(order) }
  def searchExact(key: DBKey): Seq[DBRow] = bPlusTree.search(key)
  def insert(key: DBKey, data: DBRow) { bPlusTree.insert(key, data) }
}
