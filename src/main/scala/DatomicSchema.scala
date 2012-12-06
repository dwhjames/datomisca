package reactivedatomic

trait SchemaType[DD <: DatomicData] {
  def keyword: Keyword
}

case object SchemaTypeString extends SchemaType[DString] {
  def keyword = Keyword(Namespace.DB.TYPE, "string")
}

case object SchemaTypeBoolean extends SchemaType[DBoolean] {
  def keyword = Keyword(Namespace.DB.TYPE, "boolean")
}

case object SchemaTypeLong extends SchemaType[DLong] {
  def keyword = Keyword(Namespace.DB.TYPE, "long")
}

case object SchemaTypeBigInt extends SchemaType[DBigInt] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigint")
}

case object SchemaTypeFloat extends SchemaType[DFloat] {
  def keyword = Keyword(Namespace.DB.TYPE, "float")
}

case object SchemaTypeDouble extends SchemaType[DDouble] {
  def keyword = Keyword(Namespace.DB.TYPE, "double")
}

case object SchemaTypeBigDec extends SchemaType[DBigDec] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigdec")
}

case object SchemaTypeRef extends SchemaType[DRef] {
  def keyword = Keyword(Namespace.DB.TYPE, "ref")
}

case object SchemaTypeInstant extends SchemaType[DInstant] {
  def keyword = Keyword(Namespace.DB.TYPE, "instant")
}

case object SchemaTypeUuid extends SchemaType[DUuid] {
  def keyword = Keyword(Namespace.DB.TYPE, "uuid")
}

case object SchemaTypeUri extends SchemaType[DUri] {
  def keyword = Keyword(Namespace.DB.TYPE, "uri")
}

case object SchemaTypeBytes extends SchemaType[DBytes] {
  def keyword = Keyword(Namespace.DB.TYPE, "bytes")
}

//case class SchemaType(keyword: Keyword)

object SchemaType {
  val string = SchemaTypeString //SchemaType(Keyword(Namespace.DB.TYPE, "string"))
  val boolean = SchemaTypeBoolean //SchemaType(Keyword(Namespace.DB.TYPE, "boolean"))
  val long = SchemaTypeLong //SchemaType(Keyword(Namespace.DB.TYPE, "long"))
  val bigint = SchemaTypeBigInt //SchemaType(Keyword(Namespace.DB.TYPE, "bigint"))
  val float = SchemaTypeFloat //SchemaType(Keyword(Namespace.DB.TYPE, "float"))
  val double = SchemaTypeDouble //SchemaType(Keyword(Namespace.DB.TYPE, "double"))
  val bigdec = SchemaTypeBigDec //SchemaType(Keyword(Namespace.DB.TYPE, "bigdec"))
  val ref = SchemaTypeRef //SchemaType(Keyword(Namespace.DB.TYPE, "ref"))
  val instant = SchemaTypeInstant //SchemaType(Keyword(Namespace.DB.TYPE, "instant"))
  val uuid = SchemaTypeUuid //SchemaType(Keyword(Namespace.DB.TYPE, "uuid"))
  val uri = SchemaTypeUri //SchemaType(Keyword(Namespace.DB.TYPE, "uri"))
  val bytes = SchemaTypeBytes //SchemaType(Keyword(Namespace.DB.TYPE, "bytes"))
}

trait Cardinality {
  def keyword: Keyword
}

case object CardinalityOne extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "one")
}

case object CardinalityMany extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "many")
}

//case class Cardinality(keyword: Keyword)

object Cardinality {
  val one = CardinalityOne //Cardinality(Keyword(Namespace.DB.CARDINALITY, "one"))
  val many = CardinalityMany //Cardinality(Keyword(Namespace.DB.CARDINALITY, "many"))
}

case class Unique(keyword: Keyword)

object Unique {
  val value = Unique(Keyword(Namespace.DB.UNIQUE, "value"))
  val identity = Unique(Keyword(Namespace.DB.UNIQUE, "identity"))
}

case class Attribute[DD <: DatomicData, Card <: Cardinality](
  ident: Keyword,
  valueType: SchemaType[DD],
  cardinality: Card,
  doc: Option[String] = None,
  unique: Option[Unique] = None,
  index: Option[Boolean] = None,
  fulltext: Option[Boolean] = None,
  isComponent: Option[Boolean] = None,
  noHistory: Option[Boolean] = None
) extends Operation with Identified {
  // using partiton :db.part/db
  override lazy val id = DId(Partition.DB)

  def withDoc(str: String) = copy( doc = Some(str) )
  def withUnique(u: Unique) = copy( unique = Some(u) )
  def withIndex(b: Boolean) = copy( index = Some(b) )
  def withFullText(b: Boolean) = copy( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy( noHistory = Some(b) )

  lazy val toAddOps: AddToEntity = {
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
    mb += Attribute.installAttr -> DRef(Partition.DB.keyword)

    AddToEntity(id, mb.result())
  }
  
  def toNative: java.lang.Object = toAddOps.toNative

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


