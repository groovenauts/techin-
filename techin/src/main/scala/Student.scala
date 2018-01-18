package techin

case class Student(
  number:Int,
  nickName: String,
  name: String,
  emailAddress:String
){
  def toVer2 : StudentVer2 =
    StudentVer2(number, nickName, name,
                Addresses(
                  Set(emailAddress),
                  Set(),
                  Set()
                )
    )
}

case class StudentVer2 (
  number:Int,
  nickName: String,
  name: String,
  emailAddress: Addresses
)

case class Addresses (
  to : Set[String],
  cc : Set[String],
  bcc : Set[String]
)

