package techin

import akka.persistence._
import java.util.Date

case class UpdateStudent(number: Int,nickName:String, name:String, emailAddress:String)
case class GetStudent(numbetr:Int)
case object GetStudentList

case class UpdateStudentEvent(student:Student)

case class StudentsTable(students: Map[Int, Student] = Map(), lastNumber:Int = 0){
  def updateStudent(student:Student) : StudentsTable =
    copy(students + (student.number -> student),
         if( lastNumber > student.number ) lastNumber else student.number )

  def getStudent(number:Int) : Option[Student] = students get number

  def getStudentsList : List[Student] = students.toList.map(_._2)
}

class StudentManager(val roomName:String) extends PersistentActor{
  override def persistenceId = s"student-manager-$roomName"

  var studentsTable = new StudentsTable()

  val receiveRecover: Receive = {
    case UpdateStudentEvent(student) =>
      studentsTable =
        studentsTable.updateStudent(student)
    case SnapshotOffer(_, snapShot: StudentsTable) =>
      studentsTable = snapShot
    case _ =>
  }

  val snapShotInterval = 30
  val receiveCommand: Receive = {
    case UpdateStudent(number, nickName, name, emailAddress) =>
      val newStudent = Student(number, nickName, name, emailAddress)
      val newStudentTable = studentsTable.updateStudent(newStudent)
      persist(UpdateStudentEvent(newStudent)) { addStudentEvent =>
        studentsTable = newStudentTable
        sender ! addStudentEvent.student
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(studentsTable)
      }
    case GetStudent(number) =>
      sender ! studentsTable.getStudent(number)
    case GetStudentList =>
      sender ! studentsTable
  }

}

