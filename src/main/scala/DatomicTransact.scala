package reactivedatomic

trait DataFunction {
  def func: Keyword
}

case class Fact(entity: Id, attribute: Keyword, value: DatomicData)

case class Id(partition: String, id: Option[Long] = None){
  lazy val value: datomic.db.DbId = (id match {
    case Some(id) => datomic.Peer.tempid(partition, id)
    case None => datomic.Peer.tempid(partition)
  }).asInstanceOf[datomic.db.DbId]
}

case class Add(fact: Fact) extends DataFunction {
  override val func = Keyword("add", Some(Namespace.DB))
}
case class Retract(fact: Fact) extends DataFunction  {
  override val func = Keyword("retract", Some(Namespace.DB))
}
case class RetractEntity(fact: Fact) extends DataFunction {
  override val func = Keyword("retractEntity", Some(Namespace.DB.FN))
}


