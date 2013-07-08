package miniboxing.tests.compile
import miniboxing.plugin.minispec

trait Field1[@minispec A] {
  def zero: A
  def plus(o1: A, o2: A): A
  def times(o1: A, o2: A): A
}

trait ArrayInnerProductSpace1[@minispec A] {
  def scalar: Field1[A]

  def dot(v: Array[A], w: Array[A], zero: A): A = {
    var z = scalar.zero
    var i = 0
    while (i < v.length && i < w.length) {
      z = scalar.plus(z, scalar.times(v(i), w(i)))
      i += 1
    }
    z
  }
}
