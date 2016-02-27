//
//     _____   .__         .__ ___.                    .__ scala-miniboxing.org
//    /     \  |__|  ____  |__|\_ |__    ____  ___  ___|__|  ____     ____
//   /  \ /  \ |  | /    \ |  | | __ \  /  _ \ \  \/  /|  | /    \   / ___\
//  /    Y    \|  ||   |  \|  | | \_\ \(  <_> ) >    < |  ||   |  \ / /_/  >
//  \____|__  /|__||___|  /|__| |___  / \____/ /__/\_ \|__||___|  / \___  /
//          \/          \/          \/               \/         \/ /_____/
// Copyright (c) 2011-2015 Scala Team, École polytechnique fédérale de Lausanne
//
// Authors:
//    * Vlad Ureche
//    * Cristian Talau
//
package miniboxing.plugin
package metadata

import scala.tools.nsc.plugins.PluginComponent
import scala.collection.immutable.ListMap
import miniboxing.internal.MiniboxConstants._
import miniboxing.internal.MiniboxArrayLong

trait MiniboxDefinitions extends ScalacVersion {
  this: PluginComponent =>

  import global._
  import definitions._

  // Specialization/normalization info:

  /**  PartialSpec is a binding from type parameters to their representation (Boxed/Miniboxed)
   *   INVARIANT: Regardless of whether the PartialSpec refers to the stem or a variant class,
   *   the parent's type parameters are used. */
  type PartialSpec = Map[Symbol, SpecInfo]

  sealed trait SpecInfo
  case class  Miniboxed(repr: Symbol) extends SpecInfo
  case object Boxed                   extends SpecInfo


  // Symbols:

  lazy val MinispecClass = rootMirror.getRequiredClass("scala.miniboxed")
  /**
   * This class should only appear in the tree during the `minibox` phase
   * and should be cleaned up afterwards, during the `minibox-cleanup` phase.
   */
  lazy val StorageClass = {
    // This is what is should look like:
    // ```
    //   package __root__.scala {
    //     class storage[Tpe] extends Annotation with TypeConstraint
    //   }
    // ```
    val AnnotationName = "scala.annotation.Annotation"
    val TypeConstrName = "scala.annotation.TypeConstraint"
    val AnnotationTpe = rootMirror.getRequiredClass(AnnotationName).tpe
    val TypeConstrTpe = rootMirror.getRequiredClass(TypeConstrName).tpe

    val StorageName = newTypeName("storage")
    val StorageSym = ScalaPackageClass.newClassSymbol(StorageName, NoPosition, 0L)
    val TypeParamName = newTypeName("Tpe")
    val TypeParamSym = StorageSym.newTypeParameter(TypeParamName, NoPosition, 0L) setInfo TypeBounds.empty
    StorageSym setInfoAndEnter PolyType(List(TypeParamSym), ClassInfoType(List(AnnotationTpe, TypeConstrTpe), newScope, StorageSym))
    StorageSym
  }

  lazy val GenericClass = rootMirror.getRequiredClass("scala.generic")

  lazy val CompileTimeOnlyClass =
    if (scalaVersionMinor == 10)
      // see https://github.com/scala/scala/blob/2.10.x/src/reflect/scala/reflect/internal/annotations/compileTimeOnly.scala
      rootMirror.getRequiredClass("scala.reflect.internal.annotations.compileTimeOnly")
    else
      // see https://github.com/scala/scala/blob/2.11.x/src/library/scala/annotation/compileTimeOnly.scala
      rootMirror.getRequiredClass("scala.annotation.compileTimeOnly")

//  lazy val MangledNameClass = {
//    val AnnotationName = "scala.annotation.Annotation"
//    val AnnotationTpe = rootMirror.getRequiredClass(AnnotationName).tpe
//
//    val MangledNameName = newTypeName("mangled")
//    val MangledNameSym = ScalaPackageClass.newClassSymbol(MangledNameName, NoPosition, 0L)
//    MangledNameSym setInfoAndEnter ClassInfoType(List(AnnotationTpe), newScope, MangledNameSym)
//    MangledNameSym
//  }

  // artificially created marker methods:

  def withStorage(tpar: Symbol, repr: Symbol) =
    tpar.tpeHK withAnnotation AnnotationInfo(appliedType(PolyType(StorageClass.typeParams, StorageClass.tpe), List(repr.tpeHK)), Nil, Nil)

  //   def marker_minibox2box[T, St](t: T @storage[St]): T
  lazy val marker_minibox2box =
    newPolyMethod(2, ConversionsObjectSymbol, newTermName("marker_minibox2box"), 0L)(
      tpar => (Some(List(withStorage(tpar(0), tpar(1)))), tpar(0).tpeHK))
  //   def marker_box2minibox[T, St](t: T): T @storage[St]
  lazy val marker_box2minibox =
    newPolyMethod(2, ConversionsObjectSymbol, newTermName("marker_box2minibox"), 0L)(
      tpar => (Some(List(tpar(0).tpeHK)), withStorage(tpar(0), tpar(1))))
  //   def marker_minibox2minibox[T, St1, St2](t: T @storage[St1]): T @storage[St2]
  lazy val marker_minibox2minibox =
    newPolyMethod(3, ConversionsObjectSymbol, newTermName("marker_minibox2minibox"), 0L)(
      tpar => (Some(List(withStorage(tpar(0), tpar(1)))), withStorage(tpar(0), tpar(2))))


  // Library:

  // array ops
  lazy val MiniboxArrayObjectSymbol       = rootMirror.getRequiredModule("miniboxing.internal.MiniboxArray")
  lazy val MiniboxArrayLongObjectSymbol   = rootMirror.getRequiredModule("miniboxing.internal.MiniboxArrayLong")
  lazy val MiniboxArrayDoubleObjectSymbol = rootMirror.getRequiredModule("miniboxing.internal.MiniboxArrayDouble")

  trait ArrayDefinitions {
    def owner: Symbol
    lazy val mbarray_update       = definitions.getMember(owner, newTermName("mbarray_update_minibox"))
    lazy val mbarray_apply        = definitions.getMember(owner, newTermName("mbarray_apply_minibox"))
  }

  object array_1way        extends ArrayDefinitions { lazy val owner  = if (flags.flag_float_object) MiniboxArrayLongObjectSymbol else MiniboxArrayObjectSymbol }
  object array_2way_long   extends ArrayDefinitions { lazy val owner  = MiniboxArrayLongObjectSymbol }
  object array_2way_double extends ArrayDefinitions { lazy val owner  = MiniboxArrayDoubleObjectSymbol }

  def array(repr: Symbol): ArrayDefinitions = repr match {
    case _ if !flags.flag_two_way => array_1way
    case LongClass          => array_2way_long
    case DoubleClass        => array_2way_double
  }

  lazy val mbarray_length  = definitions.getMember(MiniboxArrayObjectSymbol, newTermName("mbarray_length"))
  lazy val mbarray_new     = definitions.getMember(MiniboxArrayObjectSymbol, newTermName("mbarray_new"))
  def mbarray_update(repr: Symbol) = array(repr).mbarray_update
  def mbarray_apply(repr: Symbol)  = array(repr).mbarray_apply

  lazy val mockApplyToRealApply   = collection.mutable.Map[Symbol, Symbol]()
  lazy val mockUpdateToRealUpdate = collection.mutable.Map[Symbol, Symbol]()

  // Mock array_{apply,update}, before transformation to mbarray_{apply,update}
  abstract class MockDefinitions {
    def repr: Symbol

    // mock method names:
    private lazy val mock_apply_name = newTermName("mock_array_apply_" + repr.nameString.toLowerCase())
    private lazy val mock_update_name = newTermName("mock_array_update_" + repr.nameString.toLowerCase())

    // mock method symbols:
    lazy val mock_apply =
      newPolyMethod(1, ScalaRunTimeModule, mock_apply_name, 0L) { tparams: List[Symbol] =>
        val tparam = tparams.head
        (Some(List(appliedType(ArrayClass, tparam.tpe), IntTpe)), withStorage(tparam, repr))
      }
    lazy val mock_update =
      newPolyMethod(1, ScalaRunTimeModule, mock_update_name, 0L) { tparams: List[Symbol] =>
        val tparam = tparams.head
        (Some(List(appliedType(ArrayClass, tparam.tpe), IntTpe, withStorage(tparam, repr))), UnitTpe)
      }

    mockApplyToRealApply   += mock_apply  -> array(repr).mbarray_apply
    mockUpdateToRealUpdate += mock_update -> array(repr).mbarray_update
  }

  object mock_1way        extends MockDefinitions { def repr = LongClass }
  object mock_2way_long   extends MockDefinitions { def repr = LongClass }
  object mock_2way_double extends MockDefinitions { def repr = DoubleClass }

  def mocks(repr: Symbol): MockDefinitions = repr match {
    case _ if !flags.flag_two_way => mock_1way
    case LongClass                => mock_2way_long
    case DoubleClass              => mock_2way_double
  }

  // Any ops
  trait ops {
    def owner: Symbol
    lazy val tag_hashCode = definitions.getMember(owner, newTermName("mboxed_hashCode"))
    lazy val other_==     = definitions.getMember(owner, newTermName("mboxed_eqeq_other"))
    lazy val tag_==       = definitions.getMember(owner, newTermName("mboxed_eqeq_tag"))
    lazy val notag_==     = definitions.getMember(owner, newTermName("mboxed_eqeq_notag"))
    lazy val tag_toString = definitions.getMember(owner, newTermName("mboxed_toString"))
  }
  object ops_1way        extends ops { lazy val owner = rootMirror.getRequiredModule("miniboxing.internal.MiniboxDispatch") }
  object ops_2way_long   extends ops { lazy val owner = rootMirror.getRequiredModule("miniboxing.internal.MiniboxDispatchLong") }
  object ops_2way_double extends ops { lazy val owner = rootMirror.getRequiredModule("miniboxing.internal.MiniboxDispatchDouble")}

  def ops(repr: Symbol): ops = repr match {
    case _ if !flags.flag_two_way => ops_1way
    case LongClass          => ops_2way_long
    case DoubleClass        => ops_2way_double
  }

  def tag_hashCode(repr: Symbol) = ops(repr).tag_hashCode
  def other_==(repr: Symbol)     = ops(repr).other_==
  def tag_==(repr: Symbol)       = ops(repr).tag_==
  def notag_==(repr: Symbol)     = ops(repr).notag_==
  def tag_toString(repr: Symbol) = ops(repr).tag_toString

  // conversions
  lazy val ConversionsObjectSymbol = rootMirror.getRequiredModule("miniboxing.internal.MiniboxConversions")
  lazy val ConversionsObjectLongSymbol = rootMirror.getRequiredModule("miniboxing.internal.MiniboxConversionsLong")
  lazy val ConversionsObjectDoubleSymbol = rootMirror.getRequiredModule("miniboxing.internal.MiniboxConversionsDouble")
  trait convs {
    def owner: Symbol
    lazy val box2minibox = definitions.getMember(owner, newTermName("box2minibox_tt"))
    lazy val minibox2box = definitions.getMember(owner, newTermName("minibox2box"))
    def minibox2x: Map[Symbol, Symbol]
    def x2minibox: Map[Symbol, Symbol]
  }
  def x2minibox_long: Map[Symbol, Symbol] =
    Map(
//      UnitClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("unit2minibox")),
      BooleanClass -> definitions.getMember(ConversionsObjectSymbol, newTermName("boolean2minibox")),
      ByteClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("byte2minibox")),
      CharClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("char2minibox")),
      ShortClass ->   definitions.getMember(ConversionsObjectSymbol, newTermName("short2minibox")),
      IntClass ->     definitions.getMember(ConversionsObjectSymbol, newTermName("int2minibox")),
      LongClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("long2minibox"))
    )
  def minibox2x_long: Map[Symbol, Symbol] =
    Map(
//      UnitClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2unit")),
      BooleanClass -> definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2boolean")),
      ByteClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2byte")),
      CharClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2char")),
      ShortClass ->   definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2short")),
      IntClass ->     definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2int")),
      LongClass ->    definitions.getMember(ConversionsObjectSymbol, newTermName("minibox2long"))
    )
  def x2minibox_double(owner: Symbol): Map[Symbol, Symbol] =
    Map(
      DoubleClass ->  definitions.getMember(owner, newTermName("double2minibox")),
      FloatClass ->   definitions.getMember(owner, newTermName("float2minibox"))
    )
  def minibox2x_double(owner: Symbol): Map[Symbol, Symbol] =
    Map(
      DoubleClass ->  definitions.getMember(owner, newTermName("minibox2double")),
      FloatClass ->   definitions.getMember(owner, newTermName("minibox2float"))
    )

  object convs_1way extends convs {
    def owner = ConversionsObjectSymbol
    lazy val minibox2x = minibox2x_long ++ minibox2x_double(owner)
    lazy val x2minibox = x2minibox_long ++ x2minibox_double(owner)
  }
  object convs_2way_long extends convs {
    def owner = ConversionsObjectLongSymbol
    lazy val minibox2x = minibox2x_long
    lazy val x2minibox = x2minibox_long
  }
  object convs_2way_double extends convs {
    def owner = ConversionsObjectDoubleSymbol
    lazy val minibox2x = minibox2x_double(owner)
    lazy val x2minibox = x2minibox_double(owner)
  }

  def convs(repr: Symbol): convs =  repr match {
    case _ if !flags.flag_two_way => convs_1way
    case LongClass          => convs_2way_long
    case DoubleClass        => convs_2way_double
  }

  def minibox2box(repr: Symbol) = convs(repr).minibox2box
  def box2minibox(repr: Symbol) = convs(repr).box2minibox
  def minibox2x(repr: Symbol) = convs(repr).minibox2x
  def x2minibox(repr: Symbol) = convs(repr).x2minibox
  lazy val unreachableConversion = definitions.getMember(ConversionsObjectSymbol, newTermName("unreachableConversion"))

  // direct conversions

  lazy val standardTypeTagTrees = Map[Symbol, Tree](
      UnitClass ->    Literal(Constant(UNIT)),
      BooleanClass -> Literal(Constant(BOOLEAN)),
      ByteClass ->    Literal(Constant(BYTE)),
      CharClass ->    Literal(Constant(CHAR)),
      ShortClass ->   Literal(Constant(SHORT)),
      IntClass ->     Literal(Constant(INT)),
      LongClass ->    Literal(Constant(LONG)),
      DoubleClass ->  Literal(Constant(DOUBLE)),
      FloatClass ->   Literal(Constant(FLOAT)),
      NothingClass -> Literal(Constant(REFERENCE)),
      NullClass ->    Literal(Constant(REFERENCE))
    )

  // Manifest's newArray
  lazy val Manifest_newArray = definitions.getMember(FullManifestClass, newTermName("newArray"))

  def storageType(tparam: Symbol, spec: SpecInfo): Type = {
    val Miniboxed(repr) = spec
    withStorage(tparam, repr)
  }

  // Tuples

  lazy val Tuple1Class = definitions.TupleClass(1)
  lazy val Tuple2Class = definitions.TupleClass(2)
  lazy val Tuple1_1 = definitions.getMember(Tuple1Class, newTermName("_1"))
  lazy val Tuple2_1 = definitions.getMember(Tuple2Class, newTermName("_1"))
  lazy val Tuple2_2 = definitions.getMember(Tuple2Class, newTermName("_2"))
  lazy val tupleAccessorSymbols = Set(Tuple1_1, Tuple2_1, Tuple2_2)
  lazy val tupleFieldNames: Set[global.Name] = Set(nme._1, nme._2)
  lazy val numberOfTargsForTupleXClass = Map(Tuple1Class -> 1, Tuple2Class -> 2)


  lazy val MbTupleModule = rootMirror.getRequiredModule("miniboxing.internal.MiniboxedTuple")

  def tupleConstructor(n: Int, repr: List[String]) = definitions.getMember(MbTupleModule, newTermName(s"newTuple$n${repr.map("_"+_).mkString}"))
  lazy val MbTuple1Constructors: Map[Symbol, Symbol] =
    Map(LongClass   -> tupleConstructor(1, List("long")),
        DoubleClass -> tupleConstructor(1, List("double")))
  lazy val MbTuple2Constructors: Map[(Symbol, Symbol), Symbol] =
    Map((LongClass, LongClass)     -> tupleConstructor(2, List("long", "long")),
        (LongClass, DoubleClass)   -> tupleConstructor(2, List("long", "double")),
        (DoubleClass, LongClass)   -> tupleConstructor(2, List("double", "long")),
        (DoubleClass, DoubleClass) -> tupleConstructor(2, List("double", "double")))

  def tupleAccessor(n: Int, field: Int, repr: String) = definitions.getMember(MbTupleModule, newTermName(s"tuple${n}_accessor_${field}_${repr}"))
  lazy val MbTupleAccessor: Map[Symbol, Map[Symbol, Symbol]] =
    Map(
      Tuple1_1 ->
        Map(
          LongClass   -> tupleAccessor(1, 1, "long"),
          DoubleClass -> tupleAccessor(1, 1, "double")
        ),
      Tuple2_1 ->
        Map(
          LongClass   -> tupleAccessor(2, 1, "long"),
          DoubleClass -> tupleAccessor(2, 1, "double")
        ),
      Tuple2_2 ->
        Map(
          LongClass   -> tupleAccessor(2, 2, "long"),
          DoubleClass -> tupleAccessor(2, 2, "double")
        )
    )

  // filled in from outside
  def flags: Flags
}
