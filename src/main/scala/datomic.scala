package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Datomic {

  implicit def connection(implicit uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  implicit def database(implicit conn: Connection) = conn.database

  def q(s: String)(implicit db: datomic.Database): ResultSet = {
    import scala.collection.JavaConversions._

    ResultSet(datomic.Peer.q(s, db).toList.map(_.toList))
  }

  def query(s: String)(implicit db: datomic.Database): ResultSet = q(s)

  val _db = new NameSpace("db") {
    val part = NameSpace("db.part")
    val typ = NameSpace("db.type")
    val cardinality = NameSpace("db.cardinality")
    val install = NameSpace("db.install")
    val unique = NameSpace("db.unique")
  }
  
  def tempid(partition: String) = TemporaryId(partition)
  def tempid(partition: NameSpaceable) = TemporaryId(partition.toString)
  def tempid(partition: String, idNumber: Long) = TemporaryId(partition, Some(idNumber))
  def tempid(partition: NameSpaceable, idNumber: Long) = TemporaryId(partition.toString, Some(idNumber))

  /*type ??[T] = Var[T]
  def ??[T](s: String) = Var[T](s)

  val $$ = Input("$data")*/

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }
}

case class TxResult(dbBefore: datomic.db.Db, dbAfter: datomic.db.Db, txData: Seq[datomic.db.Datum] = Seq(), tempids: Map[Any, Any] = Map()) 

trait Connection {
  import scala.collection.JavaConverters._
  import scala.collection.JavaConversions._

  def connection: datomic.Connection

  def database: datomic.Database = connection.db()


  def createSchema(schema: Schema): Future[TxResult] = {
    transact(schema.ops)
  }

  def transact(ops: Seq[Operation]) = {
    val m: Map[Any, Any] = connection.transact(ops.map( _.asJava ).toList.asJava).get().toMap.map( t => (t._1.toString, t._2))

    println("MAP:"+m)
    println("datomic.Connection.DB_BEFORE="+m.get(datomic.Connection.DB_BEFORE.toString))
    val opt = for( 
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]];
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]];
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.db.Datum]]];
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[Any, Any]]]
    ) yield Future(TxResult(dbBefore, dbAfter, txData.toSeq, tempids.toMap))
    
    opt.getOrElse(Future.failed(new RuntimeException("couldn't parse TxResult")))    
  }
}

trait Res {
  def values: List[Any]

  def toList = values

}

object Res {
  def apply(vs: List[Any]) = new Res {
    override lazy val values: List[Any] = vs
  }

  def unapplySeq(res: Res): Option[List[Any]] = Some(res.toList)
}

trait ResultSet {

  def results: List[Res]
  def collect[T](pf: PartialFunction[Res, T]): List[T] = results.collect(pf)
  def map[T](f: Res => T): List[T] = results.map(f)

}

object ResultSet {
  def apply(rez: List[List[Any]]) = new ResultSet{
    override lazy val results: List[Res] = rez.map( (values: List[Any]) => Res(values) )
  }
}


case class TemporaryId(partition: String, idNumber: Option[Long] = None) {
  lazy val value: datomic.db.DbId = (idNumber match {
    case Some(id) => datomic.Peer.tempid(partition, id)
    case None => datomic.Peer.tempid(partition)
  }).asInstanceOf[datomic.db.DbId]

  //override def toString() = value.toString /*"#db/id[" + partition + idNumber.map(" " + _.toString).getOrElse("") + "]"*/
}


case class NameSpace(name: String) {
  def /(s: String): Attribute = Attribute(s, this)
  def /(s: Symbol): Attribute = Attribute(s.name, this)
  def /(op: Operation): Operation = op.withNs(this)

  override def toString() = ":" + name
}

trait NameSpaceable {
  def name: String
  def ns: NameSpace = Datomic._db

  override def toString() = ns.toString + "/" + name
}

case class Attribute(override val name: String, override val ns: NameSpace = Datomic._db) extends NameSpaceable 

trait Operation extends NameSpaceable{
  def withNs(ns: NameSpace): Operation
  def asJava: java.lang.Object
} 

trait DatomicTypes {
  def toDatomic(a: Any): java.lang.Object = {
    a match {
      case id: TemporaryId => id.value
      case ns: NameSpace => clojure.lang.Keyword.intern(ns.name)
      case n: NameSpaceable => clojure.lang.Keyword.intern(n.ns.name + "/" + n.name)
      case l: Long => new java.lang.Long(l)
      case i: Int => new java.lang.Integer(i)
      case b: Boolean => new java.lang.Boolean(b)
      case a => a.toString
    }
  }
}

case class add[T](id: Any, attr: Attribute, value: T, override val ns: NameSpace = Datomic._db) extends Operation with DatomicTypes{
  import scala.collection.JavaConverters._

  override def name: String = "add"
  override def withNs(ns: NameSpace) = add(id, attr, value, ns)

  override def toString() = "  [" + ns/name + " " + id + " " + attr + " " + value.toString + "]"

  def asJava = {
    val l = List[Any](toDatomic(ns/name), toDatomic(id), toDatomic(attr), toDatomic(value))
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  }
}

object add {
  def apply[T](id: Attribute, attr: Attribute, value: T) = new add(id.toString, attr, value)
  def apply[T](tmpid: TemporaryId, attr: Attribute, value: T, ns: NameSpace) = new add(tmpid, attr, value, ns)
  def apply[T](attr: Attribute, value: T, ns: NameSpace)(implicit tmpid: TemporaryId) = new add(tmpid, attr, value, ns)
  def apply[T](attr: Attribute, value: T)(implicit tmpid: TemporaryId) = new add(tmpid, attr, value)
  def apply(attrs: (Attribute, Any)*) = new mapadd(attrs)
}

case class mapadd(attrs: Seq[(Attribute, Any)]) extends Operation with DatomicTypes{
  import scala.collection.JavaConverters._

  override val name = "add"
  override def withNs(ns: NameSpace) = this

  override def toString() = " {" + attrs.map{ case (a, v) =>  a + " " + v.toString }.mkString("\n  ") + "}"

  def asJava = attrs.map( t => (toDatomic(t._1), toDatomic(t._2)) ).toMap.asJava
}

case class Schema(ops: Seq[Operation] = Seq()) {
  import scala.collection.JavaConverters._

  def :+(op: Operation) = Schema(ops :+ op)
  def ++(op: Seq[Operation]) = Schema(ops ++ op)
  //def :+(attr: SchemaAttribute) = Schema(ops ++ attr.ops)
  //def add(attr: SchemaAttribute) = Schema(ops ++ attr.ops)

  override def toString() = "[\n" + ops.mkString("\n\n") + "\n]"

  def asJava = ops.map( _.asJava ).toList.asJava
}

object Schema {
  def apply(ops: Operation*)(implicit d: DummyImplicit) = new Schema(ops)
  def :+(op: Operation) = Schema(Seq(op))
  def ++(ops: Seq[Operation]) = Schema(ops)
  //def :+(attr: SchemaAttribute) = Schema(attr.ops)
} 

/*case class SchemaAttribute(ops: Seq[Operation])

object SchemaAttribute {
  def apply(attrs: (Attribute, Any) *)(implicit s:DummyImplicit): SchemaAttribute = SchemaAttribute(Seq(mapadd(attrs.map( e => e._1 -> e._2 ))))
}*/

trait Term[T]
case class Var[T](s: String) extends Term[T]
case class Const[T](value: T) extends Term[T]
case class Input[T](value: T) extends Term[T]

case class Datom(entity: Term[_], attr: Term[_], value: Option[Term[_]] = None, tx: Option[Term[_]] = None, added: Boolean = false)

object Datom{
  def apply(entity: Term[_], attr: Term[_], value: Term[_]) = new Datom(entity, attr, Some(value))
}
/*class find1[A1](v1: Var[A1], rules: Seq[Datom] = Seq()) {
  def where( pf: PartialFunction[Var[A1], Seq[Datom]] ) = if(pf.isDefinedAt(v1)) new find1(v1, rules = pf(v1))
  def in[I1]( in1: Input[I1] ) = new find2(v1, in1)
}

class find2[A1, A2](v1: Var[A1], v2: Var[A2], rules: Seq[Datom] = Seq()) {
  def where( pf: PartialFunction[(Var[A1], Var[A2]), Seq[Datom]] ) = if(pf.isDefinedAt((v1, v2))) new find2(v1, v2, rules = pf((v1, v2)))
  def in[I1]( in: Input[I1] ) = new find3(v1, v2, input, rules)
  def in[I1, I2]( in1: Input[I1], in2: Input[I2] ) = new find4(v1, v2, in1, in2, rules)

  def apply()
}

class find3[A1, A2, A3](_1: A1, _2: A2, _3: A3, rules: Seq[Datom] = Seq()) {
  def where( pf: PartialFunction[(A1, A2, A3), Seq[Datom]] ) = if(pf.isDefinedAt((_1, _2, _3))) new find3(_1, _2, _3, rules = pf((_1, _2, _3)))
  //def in( input: Input[_] ) = find3(_1, _2, input)
  //def in( input1: Input[_], input2: Input[_] ) = new find3(_1, _2, input1, input2)
}

class find4[A1, A2, A3, A4](_1: A1, _2: A2, _3: A3, _4: A4, rules: Seq[Datom] = Seq()) extends Function2[A1, A2, (A3, A4)] {
  def where( pf: PartialFunction[(A1, A2, A3, A4), Seq[Datom]] ) = if(pf.isDefinedAt((_1, _2, _3, _4))) new find4(_1, _2, _3, _4, rules = pf((_1, _2, _3, _4)))
  //def in( input: Input[_] ) = find3(_1, _2, input)
  //def in( input1: Input[_], input2: Input[_] ) = new find3(_1, _2, input1, input2)

  def apply(a1: A1, a2: A2): (A3, A4) = {

  }
}


object Find{
  def apply[A1, A2](v1: Var[A1], v2: Var[A2]) = new find2(v1, v2)
}

*/
object DummyDatomic {
   def bootstrap(uri: String) = {
    println("Creating and connecting to database %s...".format(uri))

    datomic.Peer.createDatabase(uri)
    val conn = datomic.Peer.connect(uri)

    println("Parsing schema dtm file and running transaction...")

    val schema_rdr = new FileReader("samples/seattle/seattle-schema.dtm")
    val schema_tx: java.util.List[Object] = datomic.Util.readAll(schema_rdr).get(0).asInstanceOf[java.util.List[Object]]
    val txResult = conn.transact(schema_tx).get()
    println(txResult)

    println("Parsing seed data dtm file and running transaction...")

    val data_rdr = new FileReader("samples/seattle/seattle-data0.dtm")
    val data_tx: java.util.List[Object] = datomic.Util.readAll(data_rdr).get(0).asInstanceOf[java.util.List[Object]]
    data_rdr.close()
    val txResult2 = conn.transact(data_tx).get()
    println(txResult2)
  }

  def fakeSchema(uri:String) = {

    println("Parsing schema dtm file and running transaction...")

    val schema_rdr = new FileReader("samples/seattle/seattle-schema-test.dtm")
    val schema_tx: java.util.List[Object] = datomic.Util.readAll(schema_rdr).get(0).asInstanceOf[java.util.List[Object]]
    println("SCHEMA_TEST:"+schema_tx.get(0).asInstanceOf[java.util.List[Object]].get(1).getClass)
    //val txResult = conn.transact(schema_tx).get()
  }
}