package techin

import akka.persistence._
import java.util.Date

case class UpdateStudent(newStudent: StudentVer2)
case class GetStudent(numbetr:Int)
case object GetStudentList

case class UpdateStudentEvent(student:Student)
case class UpdateStudentEvent2(student:StudentVer2)

case class StudentsTable(students: Map[Int, Student] = Map(), lastNumber:Int = 0){
  def updateStudent(student:Student) : StudentsTable =
    copy(students + (student.number -> student),
         if( lastNumber > student.number ) lastNumber else student.number )

  def getStudent(number:Int) : Option[Student] = students get number

  def getStudentsList : List[Student] = students.toList.map(_._2)

}

object StudentsTable {
  def toVer2(studentsTable: StudentsTable) : StudentsTable2 = {
    StudentsTable2(
      studentsTable.students.mapValues(_.toVer2),
      studentsTable.lastNumber,
    )
  }
}


case class StudentsTable2(students : Map[Int, StudentVer2] = Map(), lastNumber: Int = 0){
  def updateStudent(student:StudentVer2) : StudentsTable2 =
    copy(students + (student.number -> student),
         if( lastNumber > student.number ) lastNumber else student.number )

  def getStudent(number:Int) : Option[StudentVer2] = students get number

  def getStudentsList : List[StudentVer2] = students.toList.map(_._2)
}



class StudentManager(val roomName:String) extends PersistentActor{
  override def persistenceId = s"student-manager2-$roomName"

  var studentsTable = new StudentsTable()
  var studentsTable2 = new StudentsTable2()

  val receiveRecover: Receive = {
    case UpdateStudentEvent(student) =>
      println(UpdateStudentEvent(student))
      studentsTable2 =
        studentsTable2.updateStudent(student.toVer2)
    case UpdateStudentEvent2(student) =>
      println(UpdateStudentEvent2(student))
      studentsTable2 =
        studentsTable2.updateStudent(student)
    case SnapshotOffer(_, snapShot: StudentsTable) =>
      println("get snap shot")
      println(snapShot)
      studentsTable2 = StudentsTable.toVer2(snapShot)
    case SnapshotOffer(_, snapShot: StudentsTable2) =>
      println("get snap shot")
      println(snapShot)
      studentsTable2 = snapShot
    case _ =>
  }

  val snapShotInterval = 30
  val receiveCommand: Receive = {
    case UpdateStudent(newStudent) =>
      val newStudentTable = studentsTable2.updateStudent(newStudent)
      persist(UpdateStudentEvent2(newStudent)) { addStudentEvent =>
        studentsTable2 = newStudentTable
        sender ! addStudentEvent.student
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(studentsTable2)
      }
    case GetStudent(number) =>
      println(" get student  " + number)
      sender ! studentsTable2.getStudent(number)
    case GetStudentList =>
      sender ! studentsTable2
  }

}

