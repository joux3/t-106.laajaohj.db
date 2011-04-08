package minidb.storage

class AvlTreeException (val message: String) extends Exception(message)

class AvlTreeIndex(override val indexName: String, columnNums: Seq[Int])
  extends Index(indexName, columnNums) {

  class AvlTreeNode(
    val child_left: AvlTreeNode,
    val child_right: AvlTreeNode,
    val search_key: DBKey,
    val value: DBRow) {
    
    //Guard, invalid trees cannot be created
    if (!this.balanced)
      throw new AvlTreeException("Trying to create invalid avlTree, not balanced! " + 
          "Balance factor = " + this.balanceFactor)
    
    private val tree_height: Int = calcTreeHeight(child_left,child_right)
    private def calcTreeHeight(left:AvlTreeNode,right:AvlTreeNode) = {
      math.max(
          (if (left != null) left.tree_height + 1 else 0),
          (if (right != null) right.tree_height + 1 else 0)) 
    }
    private def calcTreeHeight(tree:AvlTreeNode): Int = {
      if (tree != null) 
        calcTreeHeight(tree.child_left,tree.child_right)
      else
        0
    }

    def balanceFactor = calcBalance(child_left,child_right)
    private def calcBalance(left:AvlTreeNode,right:AvlTreeNode) = {
      (if (left != null) left.tree_height+1 else 0) -
      (if (right != null) right.tree_height+1 else 0)
    }
    
    def balanced = {
      List(-1, 0, 1).contains(this.balanceFactor)
    }
    
    // Count of nodes in this tree (root, left and right subtree)
    val weight : Int = 
      (if (child_left != null) child_left.weight else 0) +
      (if (child_right != null) child_right.weight else 0) + 1
    
    def searchExact(key: DBKey): Seq[DBRow] = {
      val emptyResult = Seq[DBRow]()

      if (this.search_key < key) {
        if (child_right != null)
          child_right.searchExact(key)
        else
          emptyResult
      }
      else if (key < this.search_key) {
        if (child_left != null)
          child_left.searchExact(key)
        else
          emptyResult
      }
      else //key == this.search_key
      {
        val left =
          if (child_left != null)
            child_left.searchExact(key)
          else
            emptyResult
        val right =
          if (child_right != null)
            child_right.searchExact(key)
          else
            emptyResult

        left ++ List(value) ++ right
      }
    }
    def searchRange(low: DBKey,
      high: DBKey,
      lowInclusive: Boolean,
      highInclusive: Boolean): Seq[DBRow] = {
      val emptyResult = Seq[DBRow]()

      if (this.search_key < low) {
        if (child_right != null)
          child_right.searchRange(low, high, lowInclusive, highInclusive)
        else
          emptyResult
      }
      else if (high < this.search_key) {
        if (child_left != null)
          child_left.searchRange(low, high, lowInclusive, highInclusive)
        else
          emptyResult
      }
      else {
        val left =
          if (child_left != null)
            if (!lowInclusive && low == this.search_key)
              emptyResult
            else
              child_left.searchRange(low, high, lowInclusive, highInclusive)
          else
            emptyResult
        val right =
          if (child_right != null)
            if (!highInclusive && high == this.search_key)
              emptyResult
            else
              child_right.searchRange(low, high, lowInclusive, highInclusive)
          else
            emptyResult

        val myValue =
          if ((low < this.search_key && this.search_key < high) ||
            (lowInclusive && low == this.search_key) ||
            (highInclusive && high == this.search_key))
            List(value)
          else
            emptyResult

        left ++ myValue ++ right
      }
    }
    /** Creates new tree with inserted data */
    def insert(key: DBKey, data: DBRow): AvlTreeNode = {
      this._insert(key, data)._1
    }
    /**
     * Internal use only.
     *
     *
     * Returns new tree and partial path to the new node
     * @return (new tree, child)
     */
    private def _insert(key: DBKey, data: DBRow): (AvlTreeNode, AvlTreeNode)={
      def getTreeHeight(tree:AvlTreeNode) = 
      {
        (if (tree != null) tree.tree_height+1 else 0)
      }
      
      if (this.search_key < key || (this.search_key == key && this.balanceFactor>=0)) {
        if (this.child_right == null) {
          val newRight =
            new AvlTreeNode(null, null, key, data)
          val newRoot = 
            new AvlTreeNode(this.child_left, newRight, 
                this.search_key, this.value)
          
          (newRoot, newRight)
        }
        else {
          val insertResult = this.child_right._insert(key, data)
          val newRight = insertResult._1
          val grandChild = insertResult._2

          //Is Balanced
          val newBalance = calcBalance(this.child_left,newRight)
          if (math.abs(newBalance) < 2) { //Balanced
            val newNode = 
              new AvlTreeNode(this.child_left, newRight, 
                      this.search_key, this.value)
            
            (newNode, newRight)
          }
          else {
            //Tree is not balanced, rotate tree
            if (newRight.child_left == grandChild) {
              //Right-left case
              //Rotate Right-left to root:
              //"new root"->left to root->right
              //"new root"->right to right->left
              //root to "new root"->left
              //right to "new root"->right
              val rotatedLeft = 
                new AvlTreeNode(this.child_left,grandChild.child_left,
                      this.search_key,this.value)
              val rotatedRight = 
                new AvlTreeNode(grandChild.child_right,newRight.child_right,
                      newRight.search_key,newRight.value)
              val newRoot =
                new AvlTreeNode(rotatedLeft,rotatedRight,
                      grandChild.search_key,grandChild.value)
              
              (newRoot, newRoot.child_right)
            }
            else { //newRight.child_right == grandChild
              //Right-Right case
              //Rotate Right to root:
              //"new root"->left to "old root"->right
              //"old root" to "new root"->left
              val oldRoot = 
                new AvlTreeNode(this.child_left,newRight.child_left,
                    this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(oldRoot,newRight.child_right,
                     newRight.search_key,newRight.value)
              
              //return new root
              (newRoot, newRoot.child_right)
            } 
          }
        }
      } //key <= this.search_key
      else {
        if (this.child_left == null) {
          val newLeft =
            new AvlTreeNode(null, null, key, data)
          val newRoot = 
            new AvlTreeNode(newLeft,this.child_right, 
                this.search_key, this.value)
          
          (newRoot, newLeft)
        }
        else {
          val insertResult = this.child_left._insert(key, data)
          val newLeft = insertResult._1
          val grandChild = insertResult._2

          //Is Balanced
          val newBalance = calcBalance(newLeft,this.child_right)
          if (math.abs(newBalance) < 2) { //Balanced
            val newNode = 
              new AvlTreeNode(newLeft, this.child_right, 
                      this.search_key, this.value)
            
            (newNode, newLeft)
          }
          else {
            //Tree is not balanced
            if (newLeft.child_left == grandChild) {
              //Left-left case
              //Rotate Left to root
              //"new root"->right to "old root"->left
              //"old root" to "new root"->right
              val oldRoot = 
                new AvlTreeNode(newLeft.child_right,this.child_right,
                      this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(newLeft.child_left,oldRoot,
                    newLeft.search_key,newLeft.value)
              
              (newRoot,newRoot.child_left)
            }
            else 
            {
              //Left-right case
              //Rotate Left-right to root
              //"new root"->left to left->right
              //"new root"->right to root->left
              //root to "new root"->right
              //right to "new root"->left
              val rotatedLeft =
                new AvlTreeNode(newLeft.child_left,grandChild.child_left,
                    newLeft.search_key,newLeft.value)
              val rotatedRight =
                new AvlTreeNode(grandChild.child_right,this.child_right,
                    this.search_key,this.value)
              val newRoot = 
                new AvlTreeNode(rotatedLeft,rotatedRight,
                    grandChild.search_key,grandChild.value)
              
              (newRoot,newRoot.child_left)
            }
          }
        }
      }
    }
    
    /**
     * Deletes all nodes with specified key
     *  
     * @return Returns a new valid root of avl-tree without key or 
     * itself if key is not found 
     */
    def remove(key: DBKey) : AvlTreeNode = {
      val newRoot = this._remove(key)
      
      //All nodes removed?
      if (newRoot==null)
        null
      //Some nodes removed
      else if (newRoot != this)
      {
        //if (this.weight - newRoot.weight > 1)
        //  System.out.println("-! Removed:" + (this.weight - newRoot.weight))
        newRoot.remove(key)
      }
      //No more nodes with search_key==key left
      else
        this
    }
    /**
     * Internal use only.
     * 
     * Deletes N nodes with specified key. N is undefined.
     * @return Returns a new root of tree with less nodes or itself if 
     * the key is not found. The height of the new tree is original 
     * height or original height - 1.
     */
    private def _remove(key: DBKey) : AvlTreeNode = {

      //Right subtree changes
      if (this.search_key < key)
      {
        //key not found
        if (this.child_right == null)
        {
          this
        }
        else
        {
          val newRight = this.child_right._remove(key);
          
          //Key not found
          if (newRight == this.child_right)
          {
            this
          }
          else
          {
            //Reconstruct tree
                        
            //Is tree_height different?
            if (newRight != null && 
                newRight.tree_height == this.child_right.tree_height)
            {
              //No -> this node is still balanced
              new AvlTreeNode(this.child_left,newRight,
                  this.search_key,this.value)
            }
            else
            {
              //Tree height changes => new balance factor
              val newBalance = calcBalance(this.child_left,newRight); 
              
              if (math.abs(newBalance) < 2)
              {
                //Remains balanced
                new AvlTreeNode(this.child_left,newRight,
                  this.search_key,this.value)
              }
              else
              {
                if (newBalance != 2) {
                  //Invalid state, new subtree height must
                  //be original_height or original_height - 1 
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                 
                //Tree unbalanced => Rotate tree
                //Rotate right
                val newRight = new AvlTreeNode(
                    this.child_left.child_right,this.child_right,
                    this.search_key,this.value)
                val newNode = new AvlTreeNode(
                    this.child_left.child_left, newRight,
                    this.child_left.search_key,this.child_left.value)
                
                newNode
              }
            }
          }
        }
      }
      //Left subtree changed
      else if (key < this.search_key)
      {
        //key not found
        if (this.child_left == null)
        {
          this
        }
        else
        {
          val newLeft = this.child_left._remove(key);
          
          //Key not found
          if (newLeft == this.child_left)
          {
            this
          }
          else
          {
            //Reconstruct tree
                        
            //Is tree_height different?
            if (newLeft != null && 
                newLeft.tree_height == this.child_left.tree_height)
            {
              //No -> this node is still balanced
              new AvlTreeNode(newLeft,this.child_right,
                  this.search_key,this.value)
            }
            else
            {
              //Tree height changes => new balance factor
              val newBalance = calcBalance(newLeft,this.child_right); 
              
              if (math.abs(newBalance) < 2)
              {
                //Remains balanced
                new AvlTreeNode(newLeft,this.child_right,
                  this.search_key,this.value)
              }
              else
              {
                if (newBalance != -2) {
                  //Invalid state, new subtree height must
                  //be original_height or original_height - 1 
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                 
                //Tree unbalanced => Rotate tree
                //Rotate left
                val newLeft = new AvlTreeNode(
                    this.child_left,this.child_right.child_left,
                    this.search_key,this.value)
                val newNode = new AvlTreeNode(
                    newLeft, this.child_right.child_right,
                    this.child_right.search_key,this.child_right.value)
                
                newNode
              }
            }
          }
        }
      }
      //key == search_key
      else
      {
        //No children
        if (this.child_left == null && this.child_right == null)
          null
        //One child
        else if (this.child_left == null && this.child_right != null)
          this.child_right
        else if (this.child_left != null && this.child_right == null)
          this.child_left
        //2 children
        else
        {
          //Bad design.. fix later
          def removeThisNode() =
          {
            def getPredecessor(node: AvlTreeNode) : (AvlTreeNode,AvlTreeNode)= {
              if (node.child_right != null)
              {
                val (predecessor,newRight) = getPredecessor(node.child_right)
                
                //Tree height changes => new balance factor
                val newBalance = calcBalance(node.child_left,newRight);
          
                if (newBalance == -2) {
                  //Tree cannot grow or shrink too much
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                
                if (newBalance == 2)
                {
                  //Tree unbalanced => Rotate tree
                  //Rotate right
                  val returnNewRight = new AvlTreeNode(
                      node.child_left.child_right,newRight,
                      node.search_key,node.value)
                  
                  (predecessor, 
                      new AvlTreeNode(
                      node.child_left.child_left, returnNewRight,
                      node.child_left.search_key,this.child_left.value))
                }
                else
                {
                  (predecessor,new AvlTreeNode(node.child_left,newRight,node.search_key,node.value))
                }
              }
              else
              {
                (node,node.child_left)
              }
            }
            def getSuccessor(node: AvlTreeNode) : (AvlTreeNode,AvlTreeNode)= {
              if (node.child_left != null)
              {
                val (predecessor,newLeft) = getSuccessor(node.child_left)
                
                //Tree height changes => new balance factor
                val newBalance = calcBalance(newLeft,node.child_right);
          
                if (newBalance == 2) {
                  //Tree cannot grow or shrink too much
                  throw new AvlTreeException(
                      "Invalid tree state in remove!" +
                      "Tree height change < -1")
                }
                
                if (newBalance == -2)
                {
                  //Tree unbalanced => Rotate tree
                  val returnNewLeft = new AvlTreeNode(
                      newLeft,node.child_right.child_left,
                      node.search_key,node.value)
                  (predecessor, 
                      new AvlTreeNode(
                      returnNewLeft, node.child_right.child_right,
                      node.child_right.search_key,node.child_right.value))
                }
                else
                {
                  (predecessor,new AvlTreeNode(newLeft,node.child_right,node.search_key,node.value))
                }
              }
              else
              {
                (node,node.child_right)
              }
            }
            
            if (this.balanceFactor == 1)
            {
              val (predecessor,route) = getPredecessor(this.child_left)
              new AvlTreeNode(route,this.child_right,predecessor.search_key,predecessor.value)
            }
            else if (this.balanceFactor == -1)
            {
              val (successor,route) = getSuccessor(this.child_right)
              new AvlTreeNode(this.child_left,route,successor.search_key,successor.value)
            }
            else // this.balanceFactor == 0
            {
              //        D                       F  
              //      /   \                  /    \ 
              //     B     F     ->        B        G
              //    / \   / \             / \
              //   A   C E   G           A   C 
              //                               \
              //                                E
              //          C           
              //        /   \       
              //  ->   B     F     
              //      / \   / \    
              //     A   - E   G    
              //                   
              
              val nodeB = this.child_left; //not null
              val nodeF = this.child_right;//not null
              
              val nodeA = nodeB.child_left; //can be null
              val nodeC = nodeB.child_left; // -- 
              val nodeE = nodeF.child_left; //can be null
              val nodeG = nodeF.child_left; // --
              
              
              if (nodeA == null && nodeC == null)
              {
                // balance==0 => nodeE == null && nodeG == null
                //Sliding nodeB to root
                new AvlTreeNode(null,nodeF,nodeB.search_key,nodeB.value)
              }
              else if (nodeC != null)
              {
                // balance==0 => nodeE != null || nodeG != null
                val (predecessor,route) = getPredecessor(nodeB)
                new AvlTreeNode(route,nodeF,predecessor.search_key,predecessor.value)
              }
              else if (nodeE != null)
              {
                // balance==0 => nodeA != null
                val (successor,route) = getSuccessor(nodeF)
                new AvlTreeNode(nodeB,route,successor.search_key,successor.value)
              }
              else 
              {
                //nodeA != null && nodeG != null
                //nodeC == null && nodeE == null
                
                //nodeA && nodeG have no children, because
                //otherwise nodeB & F would be unbalanced
                
                if (nodeA.child_left!=null || nodeA.child_right!=null ||
                    nodeG.child_left!=null || nodeG.child_right!=null)
                  throw new AvlTreeException("Tree corrupted in remove, should never happen!")
                
                new AvlTreeNode(nodeA,nodeF,nodeB.search_key,nodeB.value)
              }
            }
          }
          
          //Check the larger child first => more likely to stay balanced 
          if (this.balanceFactor == 1)
          {
            //Check subtrees, left first
            val newLeft = this.child_left._remove(key);

          
            //Found something
            if (newLeft != this.child_left)
            {
              val newRight = 
                //Subtree height changed
                if (calcTreeHeight(newLeft) != this.child_left.tree_height)
                {
                  //Removing from right cannot unbalance this node
                  this.child_right._remove(key);
                }
                else
                {
                  //Not removing from right, avoiding rotations
                  this.child_right
                }
              
              new AvlTreeNode(newLeft,newRight,this.search_key,this.value)
            }
            //No items removed in left
            else
            {
              removeThisNode()
            }
          }
          else if (this.balanceFactor == -1)
          {
            //Check subtrees, right first
            val newRight = this.child_right._remove(key);
            
            //Found something
            if (newRight != this.child_right)
            {
              val newLeft = 
                //Subtree height changed
                if (calcTreeHeight(newRight) != this.child_right.tree_height)
                {
                  //Removing from left cannot unbalance this node
                  this.child_left._remove(key);
                }
                else
                {
                  //Not removing from left, avoiding rotations
                  this.child_left
                }
              
              new AvlTreeNode(newLeft,newRight,this.search_key,this.value)
            }
            //No items removed in right
            else
            {
              removeThisNode()
            }
          }
          else //this.balanceFactor == 0
          {
            //Check both subtrees
            val newLeft = this.child_left._remove(key);
            val newRight = this.child_right._remove(key);
            
            //Found something
            if (newLeft != this.child_left || newRight != this.child_right)
            {
              new AvlTreeNode(newLeft,newRight,this.search_key,this.value)
            }
            //No items removed
            else
            {
              removeThisNode()
            }
          }
        }
      }
    }
  }

  private var root: AvlTreeNode = null;

  def searchExact(key: DBKey): Seq[DBRow] = {
    if (root == null)
      Seq[DBRow]()
    else
      root.searchExact(key)
  }

  def clear() = {
    root = null
  }
  override def delete(key: DBKey) = {
    if (root != null)
      root = root.remove(key)
  }
  def insert(key: DBKey, data: DBRow) = {
    if (root == null)
      root = new AvlTreeNode(null, null, key, data)
    else
      root = root.insert(key, data)
  }
 

  override def supportsRangeSearch: Boolean = true
  override def supportsDelete: Boolean = true

  override def searchRange(low: DBKey,
    high: DBKey,
    lowInclusive: Boolean,
    highInclusive: Boolean): Seq[DBRow] = {
    if (root == null)
      Seq[DBRow]()
    else
      root.searchRange(low, high, lowInclusive, highInclusive)
  }
}
