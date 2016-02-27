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
//
package miniboxing.plugin
package transform
package minibox
package commit

import scala.tools.nsc.transform.TypingTransformers
import scala.tools.nsc.typechecker._
import miniboxing.internal.MiniboxConstants

trait MiniboxCommitTreeTransformer extends TypingTransformers {
  self: MiniboxCommitComponent =>

  import global._
  import definitions._
  import minibox._
  import typer.{ typed, atOwner }

  override def newTransformer(unit: CompilationUnit): Transformer = new Transformer {

    override def transform(tree: Tree): Tree = {
      val specTrans = new MiniboxTreeTransformer(unit, flags.flag_rewire_mbarray, flags.flag_rewire_tuples)
      afterMiniboxCommit(checkNoStorage(specTrans.transform(tree)))
    }
  }

  def checkNoStorage(tree: Tree) = {
    //println(tree)
    for (t <- tree)
      assert(noStorageAnnot(t.tpe), t + ": " + t.tpe)
    tree
  }

  def noStorageAnnot(t: Type): Boolean = {
    var hasStorage = false
    new TypeMap {
      def apply(tp: Type): Type = mapOver(tp)
      override def mapOver(tp: Type): Type = tp match {
        case _ if tp hasAnnotation(StorageClass) =>
          hasStorage = true
          tp
        case _ =>
          super.mapOver(tp)
      }
    }.apply(t)

    !hasStorage
  }

  class CoercionExtractor {
    def unapply(tree: Tree, sym: Symbol): Option[(Tree, Type, List[Symbol])] = tree match {
      case Apply(TypeApply(fun, targ :: reprTpes), List(inner)) if fun.symbol == sym => Some((inner, targ.tpe, reprTpes.map(_.tpe.typeSymbol)))
      case _ => None
    }
  }

  object MiniboxToBox extends CoercionExtractor {
    def unapply(tree: Tree): Option[(Tree, Type, Symbol)] = unapply(tree, marker_minibox2box).map({ case (tree, tpe, List(repr)) => (tree, tpe, repr) })
  }

  object BoxToMinibox extends CoercionExtractor {
    def unapply(tree: Tree): Option[(Tree, Type, Symbol)] = unapply(tree, marker_box2minibox).map({ case (tree, tpe, List(repr)) => (tree, tpe, repr) })
  }

  object MiniboxToMinibox extends CoercionExtractor {
    def unapply(tree: Tree): Option[(Tree, Type, Symbol, Symbol)] = unapply(tree, marker_minibox2minibox).map({ case (tree, tpe, List(repr1, repr2)) => (tree, tpe, repr1, repr2) })
  }

  class MiniboxTreeTransformer(unit: CompilationUnit, mbArray_transform: Boolean, mbTuple_transform: Boolean) extends TypingTransformer(unit) {

    override def transform(tree0: Tree): Tree = {
      val oldTpe = tree0.tpe
      val newTpe = deepTransformation(oldTpe)
//      println(oldTpe + " ==> " + newTpe)

      // force new info on the symbol
      if (tree0.hasSymbolField)
        tree0.symbol.info

      def invariantErrorTree(tree0: Tree, targ: Tree, tags: Map[Symbol, Tree]): Tree = {
        reporter.error(tree0.pos, "An internal invariant of the miniboxing transformation was violated. "+
                          "Technical details: " + tree0 + " tag not found " + targ + " with " +
                          "tags being " + tags + ".")
        gen.mkMethodCall(Predef_???, Nil)
      }

      val tree1 =
        tree0 match {

          // rewrite mock array apply to miniboxed array runtime apply
          case Apply(TypeApply(mock, List(targ0)), List(array, pos)) if mockApplyToRealApply.isDefinedAt(mock.symbol) =>
            val tags = typeTagTrees(currentOwner)
            val targ = targ0.tpe.typeSymbol
            val tree1 =
              if (tags.isDefinedAt(targ)) {
                val tag = tags(targ)
                val tree1 = gen.mkMethodCall(mockApplyToRealApply(mock.symbol), List(transform(array), transform(pos), tag))
                stats("rewrote array apply: " + tree0 + " ==> " + tree1)
                tree1
              } else
                invariantErrorTree(tree0, targ0, tags)
            localTyper.typed(tree1)

          // rewrite mock array update to miniboxed array runtime update
          case Apply(TypeApply(mock, List(targ0)), List(array, pos, elem)) if mockUpdateToRealUpdate.isDefinedAt(mock.symbol) =>
            val tags = typeTagTrees(currentOwner)
            val targ = targ0.tpe.typeSymbol
            val tree1 =
              if (tags.isDefinedAt(targ)) {
                val tag = tags(targ)
                val tree1 = gen.mkMethodCall(mockUpdateToRealUpdate(mock.symbol), List(transform(array), transform(pos), transform(elem), tag))
                stats("rewrote array update: " + tree0 + " ==> " + tree1)
                tree1
              } else
                invariantErrorTree(tree0, targ0, tags)
            localTyper.typed(tree1)

          // Array new + warning!
          case tree@Apply(newArray @ Select(manifest, _), List(size)) if newArray.symbol == Manifest_newArray =>
            val tags = typeTagTrees(currentOwner)
            val tree1 = manifest.tpe.widen.typeArgs match {
              case targ :: Nil if tags.isDefinedAt(targ.typeSymbol) =>
                val tag = tags(targ.typeSymbol)
                val tree1 = gen.mkMethodCall(mbarray_new, List(targ), List(transform(size), tag))
                val ptSym = tree.tpe.dealiasWiden.typeSymbol
                val tree2 =
                  if (ptSym == ArrayClass)
                    gen.mkAttributedCast(tree1, tree.tpe)
                  else
                    tree1
                stats("rewrote array new: " + tree + " ==> " + tree1)
                ReplaceArrayByMbArrayBackwardWarning(newArray.symbol, metadata.getStemTypeParam(targ.typeSymbol), newArray.pos).warn()
                tree2
              case _ =>
                super.transform(tree)
            }
            localTyper.typed(tree1)

          // Array length
          case tree@Apply(length @ Select(array, _), Nil) if length.symbol == Array_length =>
            val tags = typeTagTrees(currentOwner)
            val tree1 = array.tpe.widen.typeArgs match {
              case tpe :: Nil if tags.isDefinedAt(tpe.typeSymbol) =>
                val tag = tags(tpe.typeSymbol)
                val tree1 = gen.mkMethodCall(mbarray_length, List(transform(array), tag))
                stats("rewrote array length: " + tree + " ==> " + tree1)
                tree1
              case _ =>
                super.transform(tree)
            }
            localTyper.typed(tree1)

          // Array[T](t1, t2, ...) => warning
          case tree@Apply(apply, List(_ ,evidence)) if apply.symbol == ArrayModule_genericApply =>
            val targs = tree.tpe.widen.typeArgs
            if (targs.length == 1) {
              val targ = targs.head
              if (flags.flag_warn_mbarrays && metadata.getStemTypeParam(targ.typeSymbol.deSkolemize).isMiniboxAnnotated)
                ReplaceArrayByMbArrayBackwardWarning(apply.symbol.typeParams.head, metadata.getStemTypeParam(targ.typeSymbol), tree0.pos, false).warn()
            }
            super.transform(tree)

          // Type classes
          case _ if (interop.TypeClasses.contains(tree0.tpe.typeSymbol)) =>
            val targs  = tree0.tpe.dealiasWiden.typeArgs
            assert(targs.length == 1, "targs don't match for " + tree0 + ": " + targs)
            val targ = targs.head

            // warn only if the type parameter is either a primitive type or a miniboxed type parameter
            if (ScalaValueClasses.contains(targ.typeSymbol))
              ReplaceTypeClassByMbTypeClassWarning(tree0.pos, None, tree0.tpe.typeSymbol, interop.TypeClasses(tree0.tpe.typeSymbol), targ).warn()
            else if (metadata.getStemTypeParam(targ.typeSymbol.deSkolemize).isMiniboxAnnotated)
              ReplaceTypeClassByMbTypeClassWarning(tree0.pos,
                                                   Some(metadata.getStemTypeParam(targ.typeSymbol.deSkolemize)),
                                                   tree0.tpe.typeSymbol,
                                                   interop.TypeClasses(tree0.tpe.typeSymbol),
                                                   targ).warn()

            super.transform(tree0)

          // simplify equality between miniboxed values
          case tree@Apply(Select(MiniboxToBox(val1, targ1, repr1), eqeq), List(MiniboxToBox(val2, targ2, repr2))) if (tree.symbol == Any_==) && (repr1 == repr2) =>
            val tags = typeTagTrees(currentOwner)
            val tag1 = tags(targ1.dealiasWiden.typeSymbol)
            val tag2 = tags(targ2.dealiasWiden.typeSymbol)
            val tree1 = {
              if ((tag1 == tag2) || (tag1.symbol == tag2.symbol))
                gen.mkMethodCall(notag_==(repr1), List(transform(val1), transform(val2)))
              else
                gen.mkMethodCall(tag_==(repr1), List(transform(val1), tag1, transform(val2), tag2))
            }
            localTyper.typed(tree1)

          // simplify equality between miniboxed values 2 - comparison with other values
          case tree@Apply(Select(MiniboxToBox(val1, targ1, repr), eqeq), List(arg)) if tree.symbol == Any_== =>
            val tags = typeTagTrees(currentOwner)
            val tag1 = tags(targ1.dealiasWiden.typeSymbol)
            val tree1 = gen.mkMethodCall(other_==(repr), List(transform(val1), tag1, transform(arg)))
            localTyper.typed(tree1)

          // simplify hashCode
          case tree@Apply(Select(MiniboxToBox(val1, targ1, repr), hash), _) if tree.symbol == Any_hashCode =>
            val tags = typeTagTrees(currentOwner)
            val tag1 = tags(targ1.dealiasWiden.typeSymbol)
            val tree1 = gen.mkMethodCall(tag_hashCode(repr), List(transform(val1), tag1))
            localTyper.typed(tree1)

          // simplify toString
          case tree@Apply(Select(MiniboxToBox(val1, targ1, repr), toString), _) if tree.symbol == Any_toString =>
            val tags = typeTagTrees(currentOwner)
            val tag1 = tags(targ1.dealiasWiden.typeSymbol)
            val tree1 = gen.mkMethodCall(tag_toString(repr), List(transform(val1), tag1))
            localTyper.typed(tree1)

          // MbArray transformations: MbArray.apply
          case tree@BoxToMinibox(Apply(fun @ Select(array, _), args), targ, repr) if fun.symbol == MbArray_apply && mbArray_transform =>
            val tags = minibox.typeTagTrees(currentOwner)
            val tag1 = tags(targ.dealiasWiden.typeSymbol)
            val tree1 = gen.mkMethodCall(MbArrayOpts_apply(repr), List(targ), transform(array) :: args.map(transform) ::: List(tag1))
            localTyper.typed(tree1)

          // MbArray transformations: MbArray.apply if primitive type
          case tree@Apply(fun @ Select(array, _), args) if fun.symbol == MbArray_apply && mbArray_transform && ScalaValueClasses.contains(tree.tpe.dealiasWiden.typeSymbol) =>
            val tpeSym = tree.tpe.dealiasWiden.typeSymbol
            val tags = minibox.typeTagTrees(currentOwner)
            val tag1 = tags(tpeSym)
            val repr: Symbol = extractRepresentation(tpeSym)
            val tree1 = gen.mkMethodCall(MbArrayOpts_apply(repr), List(tree.tpe.dealiasWiden), transform(array) :: args.map(transform) ::: List(tag1))
            val tree2 = gen.mkMethodCall(minibox2x(repr)(tpeSym), List(tree1))
            localTyper.typed(tree2)

          // MbArray transformations: MbArray.update
          case Apply(fun @ Select(array, _), List(index, MiniboxToBox(value, targ, repr))) if fun.symbol == MbArray_update && mbArray_transform =>
            val tags = minibox.typeTagTrees(currentOwner)
            val tag1 = tags(targ.dealiasWiden.typeSymbol)
            val tree1 = gen.mkMethodCall(MbArrayOpts_update(repr), List(targ), transform(array) :: transform(index) :: transform(value) :: tag1 :: Nil)
            localTyper.typed(tree1)

          // MbArray transformations: MbArray.update if primitive type
          case Apply(fun @ Select(array, _), List(index, tree)) if fun.symbol == MbArray_update && mbArray_transform && ScalaValueClasses.contains(tree.tpe.dealiasWiden.typeSymbol) =>
            val tpeSym = tree.tpe.dealiasWiden.typeSymbol
            val tags = minibox.typeTagTrees(currentOwner)
            val tag1 = tags(tpeSym)
            val repr: Symbol = extractRepresentation(tpeSym)
            val tree1 = gen.mkMethodCall(x2minibox(repr)(tpeSym), List(transform(tree)))
            val tree2 = gen.mkMethodCall(MbArrayOpts_update(repr), List(tree.tpe.dealiasWiden), transform(array) :: transform(index) :: tree1 :: tag1 :: Nil)
            localTyper.typed(tree2)

          // MbArray transformations: MbArray.empty,  MbArray.clone and MbArray.apply[T](t: T*)
          case tree@Apply(TypeApply(fun, List(targ)), List(arg)) if (fun.symbol == MbArray_empty || fun.symbol == MbArray_clone || fun.symbol == MbArray_applyconstructor) && mbArray_transform =>
            val pspec = PartialSpec.specializationsFromOwnerChain(currentOwner).toMap
            val tpe1 = targ.tpe
            val typeSymbol = tpe1.dealiasWiden.typeSymbol.deSkolemize

            def specialize(repr: Symbol, primType: Boolean): Tree = {
              val tags = minibox.typeTagTrees(currentOwner)
              val tag1 = tags(typeSymbol)
              if (fun.symbol == MbArray_applyconstructor) {
                val tree1 = gen.mkMethodCall(MbArrayOpts_applyconstr_alternatives(repr)(primType), List(tpe1), transform(arg) :: tag1 :: Nil)
                localTyper.typed(tree1)
              }
              else {
                val tree1 = gen.mkMethodCall(MbArrayOpts_alternatives(fun.symbol)(repr), List(tpe1), transform(arg) :: tag1 :: Nil)
                localTyper.typed(tree1)
              }
            }

            pspec.get(typeSymbol) match {
              case Some(minibox.Miniboxed(repr))                  => specialize(repr, false)
              case Some(minibox.Boxed)                            => super.transform(tree)
              case None if tpe1 <:< AnyRefTpe                     => super.transform(tree)
              case None if ScalaValueClasses.contains(typeSymbol) => specialize(extractRepresentation(typeSymbol), true)
              case _ =>
                var pos = tree.pos
                if (pos == NoPosition) pos = fun.pos
                if (pos == NoPosition) pos = arg.pos
                AmbiguousMbArrayTypeArgumentWarning(pos).warn()
                super.transform(tree)
            }

          // Tuple1 constructor
          case Apply(Select(New(tpt), nme.CONSTRUCTOR), List(MiniboxToBox(t1, _, repr1))) if mbTuple_transform && (tpt.tpe.typeSymbol == Tuple1Class) =>
            val targ1 = tpt.tpe.typeArgs(0).dealiasWiden
            val tags = minibox.typeTagTrees(currentOwner)
            val ttag1 = tags(targ1.typeSymbol.deSkolemize)
            val ctor = MbTuple1Constructors(repr1)
            val tree1 = gen.mkMethodCall(ctor, List(targ1), List(ttag1, transform(t1)))
            localTyper.typed(tree1)

          // Tuple2 constructor
          case Apply(Select(New(tpt), nme.CONSTRUCTOR), List(MiniboxToBox(t1, _, repr1), MiniboxToBox(t2, _, repr2))) if mbTuple_transform && (tpt.tpe.typeSymbol == Tuple2Class) =>
            val targ1 = tpt.tpe.typeArgs(0).dealiasWiden
            val targ2 = tpt.tpe.typeArgs(1).dealiasWiden
            val tags = minibox.typeTagTrees(currentOwner)
            val ttag1 = tags(targ1.typeSymbol.deSkolemize)
            val ttag2 = tags(targ2.typeSymbol.deSkolemize)
            val ctor = MbTuple2Constructors((repr1,repr2))
            val tree1 = gen.mkMethodCall(ctor, List(targ1, targ2), List(ttag1, ttag2, transform(t1), transform(t2)))
            localTyper.typed(tree1)

          // Tuple accessor (both _1 and _2)
          case BoxToMinibox(tree@Apply(Select(tuple, field), _), _, repr) if mbTuple_transform && tupleAccessorSymbols.contains(tree.symbol) && tupleFieldNames.contains(field) =>
            val targs = tuple.tpe.widen.typeArgs
            assert(targs.length == numberOfTargsForTupleXClass(tuple.tpe.typeSymbol), "targs don't match for " + tree0 + ": " + targs)
            val targ = if (field == nme._1) targs(0) else targs(1)
            val tags = minibox.typeTagTrees(currentOwner)
            val ttag = tags(targ.typeSymbol.deSkolemize)
            val accessor = MbTupleAccessor(tree.symbol)(repr)
            val tree1 = gen.mkMethodCall(accessor, targs, List(ttag, tuple))
            localTyper.typed(tree1)

          // coercion
          case BoxToMinibox(tree, targ, repr) =>
            val tags = minibox.typeTagTrees(currentOwner)
            val tree1 =
              x2minibox(repr).get(targ.typeSymbol) match {
                case Some(optConv) =>
                  gen.mkMethodCall(optConv, List(transform(tree))) // notice the fewer arguments
                case None =>
                  gen.mkMethodCall(box2minibox(repr), List(targ), List(transform(tree), tags(targ.typeSymbol)))
              }
            localTyper.typed(tree1)

          // coercion
          case MiniboxToBox(tree, targ, repr) =>
            val tags = minibox.typeTagTrees(currentOwner)
            val tree1 =
              minibox2x(repr).get(targ.typeSymbol) match {
                case Some(optConv) =>
                  gen.mkMethodCall(optConv, List(transform(tree))) // notice the fewer arguments
                case None =>
                  gen.mkMethodCall(minibox2box(repr), List(targ), List(transform(tree), tags(targ.typeSymbol)))
              }
            localTyper.typed(tree1)

          // coercion case 3
          case MiniboxToMinibox(tree, targ, repr1, repr2) =>
            val tree1 = gen.mkMethodCall(unreachableConversion, List(Literal(Constant(repr1.nameString)), Literal(Constant(repr2.nameString))))
            localTyper.typed(tree1)

          // Optimized function bridge
          case Apply(TypeApply(conv, _targs), _args) if flags.flag_rewire_functionX_bridges && interop.function_bridges(conv.symbol) =>
            val targs = _targs.map(transform(_).tpe)
            val args  = _args.map(transform)
            val bridge = conv.symbol
            val pspec = PartialSpec.fromTargsAllTargs(NoPosition, conv.symbol.typeParams zip targs, currentOwner)
            val allTparsAreMboxed = !pspec.exists(_._2 == minibox.Boxed)
            if (allTparsAreMboxed) {
              val reprs = pspec.map({ case (tpar, minibox.Miniboxed(repr)) => repr; case _ => ??? }).toList
              interop.function_bridge_optimized.get(bridge).flatMap(_.get(reprs)) match {
                case Some(bridge_opt) =>
                  val tags = minibox.typeTagTrees(currentOwner)
                  val tag_targs = targs.map(_.typeSymbol)
                  val tag_opts = tag_targs.map(tags.get(_))
                  if (tag_opts.exists(_ == None)) {
                    global.reporter.error(tree0.pos,
                        s"""[miniboxing plugin internal error] Cannot find tag when rewriting function to miniboxed function bridge.
                           |Diagnostics:
                           |  tree:  ${tree0}
                           |  pspec: $pspec
                           |  tags:  $tags
                        """.stripMargin)
                  }
                  val tag_params = tag_opts.map(_.getOrElse(gen.mkMethodCall(Predef_???, Nil))).toList
                  val tree1 = gen.mkMethodCall(bridge_opt, targs, tag_params.map(localTyper.typed) ::: args)
                  localTyper.typed(tree1)
                case None =>
                  global.reporter.error(tree0.pos,
                      s"""[miniboxing plugin internal error] Cannot find tag when rewriting function to miniboxed function bridge.
                         |Diagnostics:
                         |  tree:  ${tree0}
                         |  pspec: $pspec
                         |  tags:  ${conv.symbol}
                      """.stripMargin)
                  localTyper.typed(gen.mkMethodCall(Predef_???, Nil))
              }
            } else
              super.transform(tree0)

          // Reflection-based constant folding
          // Warning: this may not be correct!
          case If(Literal(Constant(cond: Boolean)), thenb, elseb) =>
            if (cond) thenb else elseb

          case _ =>
            super.transform(tree0)
        }

      tree1.setType(newTpe)
    }

    def extractRepresentation(valueType: Symbol): Symbol =
      PartialSpec.valueClassRepresentation(valueType) match {
        case Miniboxed(repr) => repr
        case Boxed =>
          reporter.error(currentOwner.pos, s"Miniboxing plugin internal error: invalid extraction for ${valueType} in "+
                                           s"${currentOwner.ownerChain.reverse.mkString(" > ")}. Compilation will fail.")
          LongClass
      }
  }
}
