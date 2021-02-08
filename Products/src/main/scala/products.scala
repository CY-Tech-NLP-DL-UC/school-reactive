import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

case class Product(name:String, var qte:Double){

     var id: Int = Product.next
     override def toString = "Prod :"+name+" id : "+id+" Qte: "+qte

}

case object Product {
     var cpt: Int=0
     def next={cpt=cpt+1; cpt}
}

case object DataBase{

     var products=List.empty[Product]


     def add(name:String,qte:Double)=
     {
          products = Product(name, qte)::products
     }

     def remove (id:Int)=
     {
          products = products.filter(_.id  != id)
     }

     def getProduct(i:Int):Future[Product]=
     {
          Thread.sleep(3000)
          Future{ (products.filter(x => x.id == i)) (0) }
     }

     def gets1:Future[List[Product]]=
     {
          Thread.sleep(3000)
          Future { products }
     }

     def gets2:List[Future[Product]]=
     {
          (1 to (products.size)).toList.map(x => getProduct(x))
     }

     def displayAll:Unit=
     {
          gets1.onComplete{
               case Success(prods) => prods.foreach(println)
               case Failure(e) => println(s"Exception: $e")
          }
     }
}