package miniboxing.benchmarks.launch.tests

import org.scalameter.api._
import miniboxing.internal.MiniboxConstants._
import miniboxing.internal.MiniboxConversions._
import miniboxing.benchmarks.hardcoded.fullswitch._

trait HardcodedMiniboxingSimpleFS extends BaseTest {

  private[this] object TestList {
    def list_insert(): MBList[Int] = {
      var l: MBList[Int] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_class_J[Int](int2minibox(i), l, INT)
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
        b = b ^ l.contains_J(int2minibox(i), INT)
        i += 10000
      }
      b
    }

    def list_insert_DOUBLE(): MBList[Double] = {
      var l: MBList[Double] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_class_J[Double](int2minibox(i), l, DOUBLE)
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
        b = b ^ l.contains_J(int2minibox(i), DOUBLE)
        i += 10000
      }
      b
    }

    def list_insert_LONG(): MBList[Long] = {
      var l: MBList[Long] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_class_J[Long](int2minibox(i), l, LONG)
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
        b = b ^ l.contains_J(int2minibox(i), LONG)
        i += 10000
      }
      b
    }
    def list_insert_SHORT(): MBList[Short] = {
      var l: MBList[Short] = null
      var i = 0
      while (i < testSize) {
        l = new MBList_class_J[Short](int2minibox(i), l, SHORT)
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
        b = b ^ l.contains_J(int2minibox(i), SHORT)
        i += 10000
      }
      b
    }
  }
  private[this] object TestArray {
    def array_insert(): MBResizableArray[Int] = {
      val a: MBResizableArray[Int] = new MBResizableArray_class_J[Int](INT)
      var i = 0
      while (i < testSize) {
        a.add_J(int2minibox(i), INT)
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
        b = b ^ a.contains_J(int2minibox(i), INT) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_LONG(): MBResizableArray[Long] = {
      val a: MBResizableArray[Long] = new MBResizableArray_class_J[Long](LONG)
      var i = 0
      while (i < testSize) {
        a.add_J(long2minibox(i), LONG)
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
        b = b ^ a.contains_J(long2minibox(i), LONG) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_DOUBLE(): MBResizableArray[Double] = {
      val a: MBResizableArray[Double] = new MBResizableArray_class_J[Double](DOUBLE)
      var i = 0
      while (i < testSize) {
        a.add_J(double2minibox(i), DOUBLE)
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
        b = b ^ a.contains_J(double2minibox(i), DOUBLE) // TODO: Does this cost much?
        i += 10000
      }
      b
    }

    def array_insert_SHORT(): MBResizableArray[Short] = {
      val a: MBResizableArray[Short] = new MBResizableArray_class_J[Short](SHORT)
      var i = 0
      while (i < testSize) {
        a.add_J(int2minibox(i), SHORT)
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
        b = b ^ a.contains_J(int2minibox(i), SHORT) // TODO: Does this cost much?
        i += 10000
      }
      b
    }
  }

  def testHardcodedMiniboxingSimpleFS(megamorphic: Boolean) = {
    import TestArray._
    import TestList._

    val transformation = "miniboxed standard FS " + (if (megamorphic) "mega" else "mono")

    def forceMegamorphicCallSites(): Unit =
      if (megamorphic) {
        withTestSize(megamorphicTestSize) {
          // Doing this will crash Graal with the following stack trace:
          // array_find(array_reverse(array_insert()))
          //  scope: Compiling.GraalCompiler.FrontEnd.HighTier.Lowering (BEFORE_GUARDS).IncrementalCanonicalizer.Lowering iteration 0.Schedule.InterceptException
          //  Exception occurred in scope: Compiling.GraalCompiler.FrontEnd.HighTier.Lowering (BEFORE_GUARDS).IncrementalCanonicalizer.Lowering iteration 0.Schedule.InterceptException
          //  Context obj java.lang.NullPointerException
          //  Context obj com.oracle.graal.phases.schedule.SchedulePhase@18f01d41
          //  Context obj com.oracle.graal.phases.common.LoweringPhase$Round@1f64f1b4
          //  Context obj com.oracle.graal.phases.common.IncrementalCanonicalizerPhase@faa7860
          //  Context obj com.oracle.graal.phases.common.LoweringPhase@b0ba210
          //  Context obj com.oracle.graal.compiler.phases.HighTier@1309bc25
          //  Context obj StructuredGraph:36901{HotSpotMethod<MBResizableArray_class_J.contains_J(long, byte)>}
          //  Use -G:+DumpOnError to enable dumping of graphs on this error
          //  Context obj com.oracle.graal.hotspot.amd64.AMD64HotSpotRuntime@2bd816a1
          //  Context obj DebugDumpScope[3387]
          //  java.lang.NullPointerException
          //  	at com.oracle.graal.nodes.cfg.ControlFlowGraph.commonDominator(ControlFlowGraph.java:338)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase$CommonDominatorBlockClosure.apply(SchedulePhase.java:658)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase.blocksForUsage(SchedulePhase.java:781)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase.latestBlock(SchedulePhase.java:628)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase.assignBlockToNode(SchedulePhase.java:477)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase.assignBlockToNodes(SchedulePhase.java:445)
          //  	at com.oracle.graal.phases.schedule.SchedulePhase.run(SchedulePhase.java:345)
          array_reverse(array_insert()); array_find(array_insert())
          list_hashCode(list_insert()); list_find(list_insert())
          array_find_LONG(array_insert_LONG()); array_reverse_LONG(array_insert_LONG());
          list_hashCode_LONG(list_insert_LONG()); list_find_LONG(list_insert_LONG())
          array_find_DOUBLE(array_insert_DOUBLE()); array_reverse_DOUBLE(array_insert_DOUBLE())
          list_hashCode_DOUBLE(list_insert_DOUBLE()); list_find_DOUBLE(list_insert_DOUBLE())
          array_find_SHORT(array_insert_SHORT()); array_reverse_SHORT(array_insert_SHORT())
          list_hashCode_SHORT(list_insert_SHORT()); list_find_SHORT(list_insert_SHORT())
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
