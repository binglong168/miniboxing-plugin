//
//     _____   .__         .__ ___.                    .__ scala-miniboxing.org
//    /     \  |__|  ____  |__|\_ |__    ____  ___  ___|__|  ____     ____
//   /  \ /  \ |  | /    \ |  | | __ \  /  _ \ \  \/  /|  | /    \   / ___\
//  /    Y    \|  ||   |  \|  | | \_\ \(  <_> ) >    < |  ||   |  \ / /_/  >
//  \____|__  /|__||___|  /|__| |___  / \____/ /__/\_ \|__||___|  / \___  /
//          \/          \/          \/               \/         \/ /_____/
// Copyright (c) 2012-2014 Scala Team, École polytechnique fédérale de Lausanne
//
// Authors:
//    * Eugene Burmako
//
package miniboxing.plugin
package metadata

import scala.collection.immutable.ListMap

trait MiniboxMetadataAddons {
  self: MiniboxInjectComponent =>

  import global._
  import definitions._
  import scala.collection.immutable

  // Thanks to @xeno-by :)
  implicit class RichSym(sym: Symbol) {
    def getMiniboxedTypeParameters: List[Symbol] =
      sym.typeParams.filter((s: Symbol) => s.isMiniboxAnnotated)
    def hasMiniboxedTypeParameters: Boolean = {
      val existsMbox = sym.typeParams.exists((s: Symbol) => s.isMiniboxAnnotated)
      val existsSpec = sym.typeParams.exists((s: Symbol) => s hasAnnotation SpecializedClass)

      // #117: you can't mix @specialized and @miniboxed!
      if (existsMbox && existsSpec)
        global.reporter.error(sym.pos, s"You can't mix @specialized and @miniboxed in the same ${sym.kindString}. Use only @miniboxing!")

      existsMbox && !existsSpec
    }
    def isMiniboxAnnotated: Boolean = {
      beforeMiniboxInject(sym.info) // make sure the annotation hijacker updated it
      sym hasAnnotation MinispecClass
    }
    def isField = sym.isValue && !sym.isMethod
  }

  implicit class RichType(tpe: Type) {
    def getStorageRepr: Symbol = tpe.dealiasWiden.annotations.filter(_.tpe.typeSymbol == StorageClass) match {
      case Nil         => assert(false, "No storage type detected?!?"); ???
      case List(annot) => annot.tpe.typeArgs(0).typeSymbol
    }
    def isStorage: Boolean = tpe.dealiasWiden.annotations.exists(_.tpe.typeSymbol == StorageClass)
    def withStorage(store: Type): Type = tpe.withAnnotations(List(Annotation.apply(appliedType(StorageClass.tpe, List(store)), Nil, ListMap.empty)))
    def withoutStorage: Type = tpe.filterAnnotations(_.tpe.typeSymbol != StorageClass)
    def withoutStorageDeep: Type = (new TypeMap {
      def apply(tpe: Type): Type = mapOver(tpe)
      override def mapOver(tpe: Type): Type = tpe match {
        case ann: AnnotatedType if ann.annotations.exists(_.tpe.typeSymbol == StorageClass) =>
          tpe.filterAnnotations(_.tpe.typeSymbol != StorageClass)
        case _ =>
          super.mapOver(tpe)
      }}).apply(tpe)
  }

  implicit class RichTree(tree: Tree) {
    def isStorage: Boolean = tree.tpe.isStorage
  }
}
