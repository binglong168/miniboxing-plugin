package miniboxing.benchmarks.ideal

/**
 * Inthe arrays are tricky since we need to use the natural representation for them.
 *
 * Inthe plugin if arrays are used inside minispeced code according to the following rules:
 *  - every array creation is done via: MiniboxArray.newArray[Int](len)
 *  - every access to the array requires a cast to its type: array.asInstanceOf[Array[Int]](p)
 *  - local array variables are not supported
 */
class ResizableArray {
  private final val initialSize = 4
  private var size: Int = initialSize
  private var elemCount: Int = 0
  private var array: Array[Int] = new Array[Int](initialSize)
  private var newarray: Array[Int] = _

  def extend(): Unit = {
    if (elemCount == size) {
      var pos = 0
      newarray = new Array[Int](2 * size)
      while(pos < size) {
        newarray.asInstanceOf[Array[Int]](pos) = array.asInstanceOf[Array[Int]](pos)
        pos += 1
      }
      array = newarray
      size *= 2
    }
  }

  def add(elem: Int) = {
    extend()
    array.asInstanceOf[Array[Int]](elemCount) = elem
    elemCount += 1
  }


  def reverse() = {
    var pos = 0
    while (pos * 2 < elemCount) {
      val tmp1: Int = getElement(pos)
      val tmp2: Int = getElement(elemCount-pos-1)
      setElement(pos, tmp2)
      setElement(elemCount-pos-1, tmp1)
      pos += 1
    }
  }

  def contains(elem: Int): Boolean = {
    var pos = 0
    while (pos < elemCount){
      if (getElement(pos) == elem)
        return true
      pos += 1
    }
    return false
  }

  def length = elemCount

  def setElement(p: Int, t: Int) = {
    array.asInstanceOf[Array[Int]](p) = t
  }
  def getElement(p: Int): Int = array.asInstanceOf[Array[Int]](p)
}

