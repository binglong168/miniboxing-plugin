package miniboxing.benchmarks.launch.tests

import org.scalameter.api._
import miniboxing.internal.MiniboxConstants._
import miniboxing.internal.MiniboxConversions._
import miniboxing.benchmarks.hardcoded.semiswitch._

trait HardcodedMiniboxingSimpleSS extends BaseTest {

  private[this] object TestList {
    def list_insert(): MBList[Int] = {
      var l: MBList[Int] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_J[Int](int2minibox(i), l, INT)
        i += 1
      }
      l
    }

    def list_hashCode(list: MBList[Int]): Int = {
      list.hashCode_J
    }

    def list_find(l: MBList[Int]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ l.contains_J(int2minibox(i))
        i += 10000
      }
      b
    }

    def list_insert_DOUBLE(): MBList[Double] = {
      var l: MBList[Double] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_J[Double](int2minibox(i), l, DOUBLE)
        i += 1
      }
      l
    }

    def list_hashCode_DOUBLE(list: MBList[Double]): Int = {
      list.hashCode_J
    }

    def list_find_DOUBLE(l: MBList[Double]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ l.contains_J(int2minibox(i))
        i += 10000
      }
      b
    }

    def list_insert_LONG(): MBList[Long] = {
      var l: MBList[Long] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_J[Long](int2minibox(i), l, LONG)
        i += 1
      }
      l
    }

    def list_hashCode_LONG(list: MBList[Long]): Int = {
      list.hashCode_J
    }

    def list_find_LONG(l: MBList[Long]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ l.contains_J(int2minibox(i))
        i += 10000
      }
      b
    }

    def list_insert_SHORT(): MBList[Short] = {
      var l: MBList[Short] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_J[Short](int2minibox(i), l, SHORT)
        i += 1
      }
      l
    }

    def list_hashCode_SHORT(list: MBList[Short]): Int = {
      list.hashCode_J
    }

    def list_find_SHORT(l: MBList[Short]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ l.contains_J(int2minibox(i))
        i += 10000
      }
      b
    }

    def list_insert_FLOAT(): MBList[Float] = {
      var l: MBList[Float] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_J[Float](int2minibox(i), l, FLOAT)
        i += 1
      }
      l
    }

    def list_hashCode_FLOAT(list: MBList[Float]): Int = {
      list.hashCode_J
    }

    def list_find_FLOAT(l: MBList[Float]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ l.contains_J(int2minibox(i))
        i += 10000
      }
      b
    }
  }

  private[this] object TestArray {
    def array_insert(): MBResizableArray[Int] = {
      val a: MBResizableArray[Int] = new MBResizableArray_J[Int](INT)
      var i = 0
      while (i < testSize) {
        a.add_J(int2minibox(i))
        i += 1
      }
      a
    }

    def array_reverse(a: MBResizableArray[Int]): MBResizableArray[Int] = {
      a.reverse_J
      a
    }

    def array_find(a: MBResizableArray[Int]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ a.contains_J(int2minibox(i)) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_LONG(): MBResizableArray[Long] = {
      val a: MBResizableArray[Long] = new MBResizableArray_J[Long](LONG)
      var i = 0
      while (i < testSize) {
        a.add_J(long2minibox(i))
        i += 1
      }
      a
    }

    def array_reverse_LONG(a: MBResizableArray[Long]): MBResizableArray[Long] = {
      a.reverse_J
      a
    }

    def array_find_LONG(a: MBResizableArray[Long]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ a.contains_J(long2minibox(i)) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_DOUBLE(): MBResizableArray[Double] = {
      val a: MBResizableArray[Double] = new MBResizableArray_J[Double](DOUBLE)
      var i = 0
      while (i < testSize) {
        a.add_J(double2minibox(i))
        i += 1
      }
      a
    }

    def array_reverse_DOUBLE(a: MBResizableArray[Double]): MBResizableArray[Double] = {
      a.reverse_J
      a
    }

    def array_find_DOUBLE(a: MBResizableArray[Double]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ a.contains_J(double2minibox(i)) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_SHORT(): MBResizableArray[Short] = {
      val a: MBResizableArray[Short] = new MBResizableArray_J[Short](SHORT)
      var i = 0
      while (i < testSize) {
        a.add_J(int2minibox(i))
        i += 1
      }
      a
    }

    def array_reverse_SHORT(a: MBResizableArray[Short]): MBResizableArray[Short] = {
      a.reverse_J
      a
    }

    def array_find_SHORT(a: MBResizableArray[Short]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ a.contains_J(int2minibox(i)) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_FLOAT(): MBResizableArray[Float] = {
      val a: MBResizableArray[Float] = new MBResizableArray_J[Float](FLOAT)
      var i = 0
      while (i < testSize) {
        a.add_J(int2minibox(i))
        i += 1
      }
      a
    }

    def array_reverse_FLOAT(a: MBResizableArray[Float]): MBResizableArray[Float] = {
      a.reverse_J
      a
    }

    def array_find_FLOAT(a: MBResizableArray[Float]): Boolean = {
      var i = 0
      var b = true
      while (i < testSize) {
        b = b ^ a.contains_J(int2minibox(i)) // TODO: Does this cost much?
        i += 10000
      }
      b
    }
}

  def testHardcodedMiniboxingSimpleSS(megamorphic: Boolean) = {
    import TestArray._
    import TestList._

    val transformation = "miniboxed standard SS " + (if (megamorphic) "mega" else "mono")

    def forceMegamorphicCallSites(): Unit =
      if (megamorphic) {
        withTestSize(megamorphicTestSize) {
          array_find(array_reverse(array_insert()))
          list_hashCode(list_insert()); list_find(list_insert())
          array_find_LONG(array_reverse_LONG(array_insert_LONG()))
          list_hashCode_LONG(list_insert_LONG()); list_find_LONG(list_insert_LONG())
          array_find_DOUBLE(array_reverse_DOUBLE(array_insert_DOUBLE()))
          list_hashCode_DOUBLE(list_insert_DOUBLE()); list_find_DOUBLE(list_insert_DOUBLE())
          array_find_SHORT(array_reverse_SHORT(array_insert_SHORT()))
          list_hashCode_SHORT(list_insert_SHORT()); list_find_SHORT(list_insert_SHORT())
          array_find_FLOAT(array_reverse_FLOAT(array_insert_FLOAT()))
          list_hashCode_FLOAT(list_insert_FLOAT()); list_find_FLOAT(list_insert_FLOAT())
        }
      }

    var a: MBResizableArray[Int] = null
    var b: Boolean = true
    test(transformation, "array.insert ", _ => { forceMegamorphicCallSites(); () },                 a = array_insert(),   () => { assert(a.length == testSize); a = null })
    test(transformation, "array.reverse", _ => { forceMegamorphicCallSites(); a = array_insert() }, a = array_reverse(a), () => { assert(a.length == testSize); a = null })
    test(transformation, "array.find   ", _ => { forceMegamorphicCallSites(); a = array_insert() }, b = array_find(a),    () => { assert(b == true); a = null })

    var l: MBList[Int] = null
    var i: Int = 0
    test(transformation, "list.insert  ", _ => { forceMegamorphicCallSites(); () },                 l = list_insert(),    () => { assert(l.length == testSize); l = null })
    test(transformation, "list.hashCode", _ => { forceMegamorphicCallSites(); l = list_insert() },  i = list_hashCode(l), () => { assert(i != 0); l = null })
    test(transformation, "list.find    ", _ => { forceMegamorphicCallSites(); l = list_insert() },  b = list_find(l),     () => { assert(b == true); l = null })
  }

}
