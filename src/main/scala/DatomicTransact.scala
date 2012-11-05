package reactivedatomic

trait Identified {
  def id: DId
}

trait Referenceable {
  def ident: DRef
}

case class Fact(id: DId, attr: Keyword, value: DatomicData)

case class Partition(keyword: Keyword) {
  override def toString = keyword.toString
}

object Partition {
  val DB = Partition(Keyword("db", Some(Namespace.DB.PART)))
  val TX = Partition(Keyword("tx", Some(Namespace.DB.PART)))
  val USER = Partition(Keyword("user", Some(Namespace.DB.PART)))
}

trait Operation extends Nativeable

trait DataFunction extends Operation {
  def func: Keyword
}

case class Add(fact: Fact) extends DataFunction with Identified {
  override val func = Keyword("add", Some(Namespace.DB))
  override val id = fact.id

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  } 
}

object Add {
  def apply( id: DId, attr: Keyword, value: DatomicData) = new Add(Fact(id, attr, value))
}


case class Retract(fact: Fact) extends DataFunction with Identified {
  override val func = Keyword("retract", Some(Namespace.DB))
  override val id = fact.id

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  }
}

object Retract {
  def apply( id: DId, attr: Keyword, value: DatomicData) = new Retract(Fact(id, attr, value))
}

case class RetractEntity(entId: DLong) extends DataFunction {
  override val func = Keyword("retractEntity", Some(Namespace.DB.FN))

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, entId.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  } 

  //override def toString = toNative.toString
}

case class AddEntity(props: Map[Keyword, DatomicData]) extends Operation with Identified {
  def id = props(Keyword("id", Namespace.DB)).asInstanceOf[DId]
  def toNative: java.lang.Object = {
    import scala.collection.JavaConverters._
    ( props.map( t => (t._1.toNative, t._2.toNative) ) + (Keyword("id", Namespace.DB).toNative -> id.toNative) ).asJava
  }
}

object AddEntity {
  def apply(id: DId, props: Map[Keyword, DatomicData]): AddEntity = new AddEntity(props + (Keyword("id", Namespace.DB) -> id) )
  def apply(id: DId)(props: (Keyword, DatomicData)*): AddEntity = new AddEntity(props.toMap + (Keyword("id", Namespace.DB) -> id) )
}

case class AddIdent(override val ident: DRef, partition: Partition = Partition.USER) extends Operation with Identified with Referenceable {
  override lazy val id = DId(partition)

  def toNative = Add( Fact(id, Keyword("ident", Namespace.DB), ident) ).toNative

  override def toString = toNative.toString

}

object AddIdent {
  def apply(ident: Keyword) = new AddIdent(DRef(ident))
  def apply(ident: Keyword, partition: Partition) = new AddIdent(DRef(ident), partition)
}

case class Transaction(operations: Seq[Operation]) 


case class SchemaType(keyword: Keyword)

object SchemaType {
  val string = SchemaType(Keyword(Namespace.DB.TYPE, "string"))
  val boolean = SchemaType(Keyword(Namespace.DB.TYPE, "boolean"))
  val long = SchemaType(Keyword(Namespace.DB.TYPE, "long"))
  val bigint = SchemaType(Keyword(Namespace.DB.TYPE, "bigint"))
  val float = SchemaType(Keyword(Namespace.DB.TYPE, "float"))
  val double = SchemaType(Keyword(Namespace.DB.TYPE, "double"))
  val bigdec = SchemaType(Keyword(Namespace.DB.TYPE, "bigdec"))
  val ref = SchemaType(Keyword(Namespace.DB.TYPE, "ref"))
  val instant = SchemaType(Keyword(Namespace.DB.TYPE, "instant"))
  val uuid = SchemaType(Keyword(Namespace.DB.TYPE, "uuid"))
  val uri = SchemaType(Keyword(Namespace.DB.TYPE, "uri"))
  val bytes = SchemaType(Keyword(Namespace.DB.TYPE, "bytes"))
}

case class Cardinality(keyword: Keyword)

object Cardinality {
  val one = Cardinality(Keyword(Namespace.DB.CARDINALITY, "one"))
  val many = Cardinality(Keyword(Namespace.DB.CARDINALITY, "many"))
}

case class Unique(keyword: Keyword)

object Unique {
  val value = Unique(Keyword(Namespace.DB.UNIQUE, "value"))
  val identity = Unique(Keyword(Namespace.DB.UNIQUE, "identity"))
}

case class Attribute(
  ident: Keyword,
  valueType: SchemaType,
  cardinality: Cardinality,
  doc: Option[String] = None,
  unique: Option[Unique] = None,
  index: Option[Boolean] = None,
  fulltext: Option[Boolean] = None,
  isComponent: Option[Boolean] = None,
  noHistory: Option[Boolean] = None
) extends Operation with Identified {
  // using partiton :db.part/db
  override lazy val id = DId(Partition.DB)

  def withDoc(str: String): Attribute = copy( doc = Some(str) )
  def withUnique(u: Unique): Attribute = copy( unique = Some(u) )
  def withIndex(b: Boolean) = copy( index = Some(b) )
  def withFulltext(b: Boolean) = copy( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy( noHistory = Some(b) )

  lazy val toAddEntity: AddEntity = {
    val mb = new scala.collection.mutable.MapBuilder[Keyword, DatomicData, Map[Keyword, DatomicData]](Map(
      Attribute.id -> id,
      Attribute.ident -> DRef(ident),
      Attribute.valueType -> DRef(valueType.keyword),
      Attribute.cardinality -> DRef(cardinality.keyword)
    ))
    if(doc.isDefined) mb += Attribute.doc -> DString(doc.get)
    if(unique.isDefined) mb += Attribute.unique -> DRef(unique.get.keyword)
    if(index.isDefined) mb += Attribute.index -> DBoolean(index.get)
    if(fulltext.isDefined) mb += Attribute.fulltext -> DBoolean(fulltext.get)
    if(isComponent.isDefined) mb += Attribute.isComponent -> DBoolean(isComponent.get)
    if(noHistory.isDefined) mb += Attribute.noHistory -> DBoolean(noHistory.get)
    
    // installing attribute
    // _db.install/'_attribute -> _db.part/'db

    mb += Attribute.installAttr -> DRef(Partition.DB.keyword)

    AddEntity(id, mb.result())
  }
  
  def toNative: java.lang.Object = toAddEntity.toNative

  override def toString = s"""
{ 
  ${Attribute.id} $id
  ${Attribute.ident} ${DRef(ident)}
  ${Attribute.valueType} ${DRef(valueType.keyword)}
  ${Attribute.cardinality} ${DRef(cardinality.keyword)}""" +
  ( if(doc.isDefined) { "\n  " + Attribute.doc + " " + DString(doc.get) } else { "" } ) +
  ( if(unique.isDefined) { "\n  " + Attribute.unique + " " + DRef(unique.get.keyword) } else { "" } ) +
  ( if(index.isDefined) { "\n  " + Attribute.index + " " + DBoolean(index.get) } else { "" }) +
  ( if(fulltext.isDefined) { "\n  " + Attribute.fulltext + " " + DBoolean(fulltext.get) } else { "" }) +
  ( if(isComponent.isDefined) { "\n  " + Attribute.isComponent + " " + DBoolean(isComponent.get) } else { "" }) +
  ( if(noHistory.isDefined) { "\n  " + Attribute.noHistory + " " + DBoolean(noHistory.get) } else { "" }) +
  "\n  " + Attribute.installAttr + " " + DRef(Partition.DB.keyword) + 
  "\n}"
}

object Attribute {
  val id = Keyword(Namespace.DB, "id")
  val ident = Keyword(Namespace.DB, "ident")
  val valueType = Keyword(Namespace.DB, "valueType")
  val cardinality = Keyword(Namespace.DB, "cardinality")
  val doc = Keyword(Namespace.DB, "doc")
  val unique = Keyword(Namespace.DB, "unique")
  val index = Keyword(Namespace.DB, "index")
  val fulltext = Keyword(Namespace.DB, "fulltext")
  val isComponent = Keyword(Namespace.DB, "isComponent")
  val noHistory = Keyword(Namespace.DB, "noHistory")
  val installAttr = Keyword(Namespace.DB.INSTALL, "_attribute")
}

case class Schema(ops: Seq[Operation]) {
  def :+(op: Operation) = Schema(ops :+ op)
  def ++(other: Schema) = Schema(ops ++ other.ops)
}

object Schema {
  def apply(op: Operation) = new Schema(Seq(op))
  def apply(op: Operation, ops: Operation*) = new Schema(Seq(op) ++ ops)
  def :+(op: Operation) = Schema(Seq(op))
  def ++(ops: Seq[Operation]) = Schema(ops)
}

trait ParsingExpr
case class ScalaExpr(expr: String) extends ParsingExpr
case class DSeqParsing(elts: Seq[Either[ParsingExpr, DatomicData]]) extends ParsingExpr

case class AddEntityParsing(props: Map[Keyword, Either[ParsingExpr, DatomicData]])
