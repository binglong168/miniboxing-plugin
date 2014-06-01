package miniboxing
package benchmarks
package launch

import org.scalameter.CurveData
import org.scalameter.api._
import org.scalameter.utils.Tree
import org.scalameter.execution.LocalExecutor

class TweakedPerfomanceTest extends PerformanceTest {

  @transient lazy val reporter = new LoggingReporter {

    override def report(result: CurveData, persistor: Persistor) {
      for (measurement <- result.measurements)
        print(f"  ${result.context.scope}%-60s: ${measurement.params}%-30s: ${measurement.value}% 10.5f\n")
    }

    override def report(result: Tree[CurveData], persistor: Persistor) = {
      true
    }
  }

  @transient lazy val executor = SeparateJvmsExecutor(
    Executor.Warmer.Default(),
    Aggregator.average,
    new Executor.Measurer.Default
  )

  def persistor = Persistor.None

  def report(bench: String) =
    println(s"Starting $bench benchmarks. Lay back, it might take a few minutes to stabilize...")

  // Benchmarks to run
  object miniboxed
  object specialized
  object generic
  object library
  object scalablitz
0
  val sizes = Gen.range("size")(50000, 5000000, 500000)
  val tests = List(miniboxed, specialized, generic, library)

  def step = 5.0
  def zero = 3.0
}



object MiniboxedBenchmark extends TweakedPerfomanceTest {

  import simple.miniboxed._

  // sanity check:
  lazy val fun = new Function1[Int, Int] { def apply(i: Int) = i }
  assert(fun.getClass().getInterfaces().map(_.getSimpleName()).contains("Function1_JJ"), fun.getClass().getInterfaces())

  implicit object Num_D extends Numeric[Double] {
    def plus(x: Double, y: Double): Double = x + y
    def zero: Double = 0.0
  }

  var listx: List[Double] = Nil
  var listy: List[Double] = Nil

  if (tests.contains(miniboxed)) {

    report("miniboxed")
    performance of this.getClass.getSimpleName in {
      measure method "Least Squares Method with List[Double]" in {
        using(sizes) setUp {
          size =>
            val random = new scala.util.Random(0)
            // Random between (-1.0, 1.0), mean = 0
            def rand = random.nextDouble - random.nextDouble
            // Function to approximate = 5x + 3
            val func = new Function1[Int, Double] {
              def apply(x: Int): Double = step*x + zero
            }
            listx = Nil
            listy = Nil
            var i = 0
            while (i < size) {
              listx = (i + rand) :: i + rand :: listx
              listy = (func(i) + rand) :: func(i) + rand :: listy
              i += 1
            }
        } config (
          exec.benchRuns -> 20,
          exec.minWarmupRuns -> 10,
          exec.independentSamples -> 5
        ) in {
          size =>

          val listxy = listx.zip(listy)

          val sumx  = listx.sum
          val sumy  = listy.sum

          // function (x, y) => x * y
          val fxy = new Function1[Tuple2[Double,Double], Double] {
            def apply(t: Tuple2[Double, Double]): Double = t._1 * t._2
          }
          val sumxy = listxy.map(fxy).sum

          // function x => x * x
          val fxx = new Function1[Double, Double] {
            def apply(x: Double): Double = x * x
          }
          val squarex = listx.map(fxx).sum

          val m = (size*sumxy - sumx*sumy) / (size*squarex - sumx*sumx)
          val b = (sumy*squarex - sumx*sumxy) / (size*squarex - sumx*sumx)

          // was it a good approximation?
          assert(m - step < 0.1, "m exceeded 10% of error : " + m + " instead of " + step)
          assert(b - zero < 0.1, "b exceeded 10% of error : " + b + " instead of " + zero)
        }
      }
    }
  }
}



object GenericBenchmark extends TweakedPerfomanceTest {

  import simple.generic._

  // sanity check:
  lazy val fun = new Function1[Int, Int] { def apply(i: Int) = i }
  assert(fun.getClass().getInterfaces().map(_.getSimpleName()).contains("Function1"), fun.getClass().getInterfaces())

  implicit object Num_D extends Numeric[Double] {
    def plus(x: Double, y: Double): Double = x + y
    def zero: Double = 0.0
  }

  var listx: List[Double] = Nil
  var listy: List[Double] = Nil

  if (tests.contains(generic)) {

    report("generic")
    performance of this.getClass.getSimpleName in {
      measure method "Least Squares Method with List[Double]" in {
        using(sizes) setUp {
          size =>
            val random = new scala.util.Random(0)
            // Random between (-1.0, 1.0), mean = 0
            def rand = random.nextDouble - random.nextDouble
            // Function to approximate = 5x + 3
            val func = new Function1[Int, Double] {
              def apply(x: Int): Double = step*x + zero
            }
            listx = Nil
            listy = Nil
            var i = 0
            while (i < size) {
              listx = (i + rand) :: i + rand :: listx
              listy = (func(i) + rand) :: func(i) + rand :: listy
              i += 1
            }
        } config (
          exec.benchRuns -> 20,
          exec.minWarmupRuns -> 10,
          exec.independentSamples -> 5
        ) in {
          size =>

          val listxy = listx.zip(listy)

          val sumx  = listx.sum
          val sumy  = listy.sum

          // function (x, y) => x * y
          val fxy = new Function1[Tuple2[Double,Double], Double] {
            def apply(t: Tuple2[Double, Double]): Double = t._1 * t._2
          }
          val sumxy = listxy.map(fxy).sum

          // function x => x * x
          val fxx = new Function1[Double, Double] {
            def apply(x: Double): Double = x * x
          }
          val squarex = listx.map(fxx).sum

          val m = (size*sumxy - sumx*sumy) / (size*squarex - sumx*sumx)
          val b = (sumy*squarex - sumx*sumxy) / (size*squarex - sumx*sumx)

          // was it a good approximation?
          assert(m - step < 0.1, "m exceeded 10% of error : " + m + " instead of " + step)
          assert(b - zero < 0.1, "b exceeded 10% of error : " + b + " instead of " + zero)
        }
      }
    }
  }
}



object SpecializedBenchmark extends TweakedPerfomanceTest {

  import simple.specialized._

  // sanity check:
  lazy val fun = new Function1[Int, Int] { def apply(i: Int) = i }
  assert(fun.getClass().getInterfaces().map(_.getSimpleName()).contains("Function1$mcII$sp"), fun.getClass().getInterfaces())

  implicit object Num_D extends Numeric[Double] {
    def plus(x: Double, y: Double): Double = x + y
    def zero: Double = 0.0
  }

  var listx: List[Double] = Nil
  var listy: List[Double] = Nil

  if (tests.contains(specialized)) {

    report("specialized")
    performance of this.getClass.getSimpleName in {
      measure method "Least Squares Method with List[Double]" in {
        using(sizes) setUp {
          size =>
            val random = new scala.util.Random(0)
            // Random between (-1.0, 1.0), mean = 0
            def rand = random.nextDouble - random.nextDouble
            // Function to approximate = 5x + 3
            val func = new Function1[Int, Double] {
              def apply(x: Int): Double = step*x + zero
            }
            listx = Nil
            listy = Nil
            var i = 0
            while (i < size) {
              listx = (i + rand) :: i + rand :: listx
              listy = (func(i) + rand) :: func(i) + rand :: listy
              i += 1
            }
        } config (
          exec.benchRuns -> 20,
          exec.minWarmupRuns -> 10,
          exec.independentSamples -> 5
        ) in {
          size =>

          val listxy = listx.zip(listy)

          val sumx  = listx.sum
          val sumy  = listy.sum

          // function (x, y) => x * y
          val fxy = new Function1[Tuple2[Double,Double], Double] {
            def apply(t: Tuple2[Double, Double]): Double = t._1 * t._2
          }
          val sumxy = listxy.map(fxy).sum

          // function x => x * x
          val fxx = new Function1[Double, Double] {
            def apply(x: Double): Double = x * x
          }
          val squarex = listx.map(fxx).sum

          val m = (size*sumxy - sumx*sumy) / (size*squarex - sumx*sumx)
          val b = (sumy*squarex - sumx*sumxy) / (size*squarex - sumx*sumx)

          // was it a good approximation?
          assert(m - step < 0.1, "m exceeded 10% of error : " + m + " instead of " + step)
          assert(b - zero < 0.1, "b exceeded 10% of error : " + b + " instead of " + zero)
        }
      }
    }
  }
}



object LibraryGenericBenchmark extends TweakedPerfomanceTest {

  // sanity check:
  lazy val fun = new Function1[Int, Int] { def apply(i: Int) = i }
  assert(fun.getClass().getInterfaces().map(_.getSimpleName()).exists(_.endsWith("Function1$mcII$sp")), fun.getClass().getInterfaces())

  var listx: List[Double] = Nil
  var listy: List[Double] = Nil

  if (tests.contains(library)) {

    report("library")
    performance of this.getClass.getSimpleName in {
      measure method "Least Squares Method with List[Double]" in {
        using(sizes) setUp {
          size =>
            val random = new scala.util.Random(0)
            // Random between (-1.0, 1.0), mean = 0
            def rand = random.nextDouble - random.nextDouble
            // Function to approximate = 5x + 3
            val func = new Function1[Int, Double] {
              def apply(x: Int): Double = step*x + zero
            }
            listx = Nil
            listy = Nil
            var i = 0
            while (i < size) {
              listx = (i + rand) :: i + rand :: listx
              listy = (func(i) + rand) :: func(i) + rand :: listy
              i += 1
            }
        } config (
          exec.benchRuns -> 20,
          exec.minWarmupRuns -> 10,
          exec.independentSamples -> 5
        ) in {
          size =>

          val listxy = listx.zip(listy)

          val sumx  = listx.sum
          val sumy  = listy.sum

          val sumxy = listxy.map(t => t._1 * t._2).sum
          val squarex = listx.map(x => x * x).sum

          val m = (size*sumxy - sumx*sumy) / (size*squarex - sumx*sumx)
          val b = (sumy*squarex - sumx*sumxy) / (size*squarex - sumx*sumx)

          // was it a good approximation?
          assert(m - step < 0.1, "m exceeded 10% of error : " + m + " instead of " + step)
          assert(b - zero < 0.1, "b exceeded 10% of error : " + b + " instead of " + zero)
        }
      }
    }
  }
}



//object LibraryScalaBlitzBenchmark extends TweakedPerfomanceTest {
//
//  // sanity check:
//  lazy val fun = new Function1[Int, Int] { def apply(i: Int) = i }
//  assert(fun.getClass().getSimpleName() == "Function1$mcII$sp")
//
//  var listx: List[Double] = Nil
//  var listy: List[Double] = Nil
//
//  if (tests.contains(scalablitz)) {
//
//    report("library scalablitz")
//    performance of this.getClass.getSimpleName in {
//      measure method "Least Squares Method with List[Double]" in {
//        using(sizes) setUp {
//          size =>
//            val random = new scala.util.Random(0)
//            // Random between (-1.0, 1.0), mean = 0
//            def rand = random.nextDouble - random.nextDouble
//            // Function to approximate = 5x + 3
//            val func = new Function1[Int, Double] {
//              def apply(x: Int): Double = step*x + zero
//            }
//            listx = Nil
//            listy = Nil
//            var i = 0
//            while (i < size) {
//              listx = (i + rand) :: i + rand :: listx
//              listy = (func(i) + rand) :: func(i) + rand :: listy
//              i += 1
//            }
//        } config (
//          exec.benchRuns -> 20,
//          exec.minWarmupRuns -> 10,
//          exec.independentSamples -> 5
//        ) in {
//          size =>
//          import scala.collection.optimizer._
//          optimize {
//            val listxy = listx.zip(listy)
//            val sumx  = listx.sum
//            //  [error] /mnt/data1/Work/Workspace/dev/miniboxing-plugin/tests/lib-bench/test/miniboxing/benchmarks/launch/SimpleLinkedList.scala:292: value sum is not a member of scala.collection.par.Par[List[Double]]
//            //  [error]           val sumx  = listx.sum
//            //  [error]               ^
//            //  [error] one error found
//            val sumy  = listy.sum
//
//            val sumxy = listxy.map(t => t._1 * t._2).sum
//            val squarex = listx.map(x => x * x).sum
//
//            val m = (size*sumxy - sumx*sumy) / (size*squarex - sumx*sumx)
//            val b = (sumy*squarex - sumx*sumxy) / (size*squarex - sumx*sumx)
//          }
//
//          // was it a good approximation?
//          assert(m - step < 0.1, "m exceeded 10% of error : " + m + " instead of " + step)
//          assert(b - zero < 0.1, "b exceeded 10% of error : " + b + " instead of " + zero)
//        }
//      }
//    }
//  }
//}
