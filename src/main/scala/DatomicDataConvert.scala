package reactivedatomic

/** DatomicData to Scala reader specific */
trait DD2ScalaReader[-DD <: DatomicData, A] {
  def read(dd: DD): A
}

object DD2ScalaReader extends DD2ScalaReaderImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new DD2ScalaReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DD2DDReader[+DD <: DatomicData] {
  def read(d: DatomicData): DD
}

object DD2DDReader extends DD2DDReaderImplicits{
  def apply[DD <: DatomicData](f: DatomicData => DD) = new DD2DDReader[DD]{
    def read(d: DatomicData): DD = f(d)
  }
}

trait DDReader[-DD <: DatomicData, +A] {
  def read(dd: DD): A
}

object DDReader extends DDReaderImplicits{
  def apply[DD <: DatomicData, A](f: DD => A) = new DDReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DDWriter[+DD <: DatomicData, -A] {
  def write(a: A): DD
}

object DDWriter extends DDWriterImplicits {
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriter[DD, A] {
    def write(a: A) = f(a)
  }
}


