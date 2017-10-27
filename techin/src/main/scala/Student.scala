package techin

case class Student(
  number:Int,
  nickName: String,
  name: String,
  emailAddress:String
)

case class StudentVer2 (
  number:Int,
  nickName: String,
  name: String,
  emailAddress: Addresses
)

case class Addresses (
  to : String,
  cc : Set[String],
  bcc : Set[String]
)
