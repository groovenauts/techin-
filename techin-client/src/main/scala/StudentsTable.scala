package techin.client

import scalafx.Includes._
import scalafx.scene.control.{
  TableView,
  TableColumn,
  TableRow
}
import TableColumn._

import scalafx.beans.property.{
  IntegerProperty,
  StringProperty,
  ObjectProperty
}
import scalafx.collections.ObservableBuffer
import techin.{
  StudentVer2,
  StudentsTable2 => RemoteStudentsTable,
  GetStudentList,
  Addresses
}

import akka.actor.{
  ActorSelection,
  Actor
}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{
  Future
}

class StudentData(val student:StudentVer2){
  val number = StringProperty("%03d".format(student.number))
  val nickName = StringProperty(student.nickName)
  val name = StringProperty(student.name)
  val emailTO = StringProperty(student.emailAddress.to.mkString(","))
  val emailCC = StringProperty(student.emailAddress.cc.mkString(","))
  val emailBCC = StringProperty(student.emailAddress.bcc.mkString(","))
}

class StudentsList(actorRef: ActorSelection) {
  val students = ObservableBuffer[StudentData]()

  implicit val timeout = Timeout(20 seconds)
  val interval = 1000

  def reload : Future[Any] = (actorRef ? GetStudentList).flatMap {
    (a:Any) =>
    println(a)
    if( a.isInstanceOf[RemoteStudentsTable] ){
      val studentsTable = a.asInstanceOf[RemoteStudentsTable]
      val studentsList : List[StudentVer2] = studentsTable.getStudentsList
      for( newStudent <- studentsList ){
        // find update student
        var updateStudent : StudentData = null
        for( existStudent <- students.toList ){
          if( "%03d".format(newStudent.number) == existStudent.number() ){
            updateStudent = existStudent
          }
        }
        // update or add student
        if( updateStudent == null ){
          students.add(new StudentData(newStudent))
        }else{
          if( updateStudent.nickName() != newStudent.nickName )
            updateStudent.nickName() = newStudent.nickName
          
          if( updateStudent.name() != newStudent.name )
            updateStudent.name() = newStudent.name

          updateStudent.emailTO() = newStudent.emailAddress.to.mkString(",")
          updateStudent.emailCC() = newStudent.emailAddress.cc.mkString(",")
          updateStudent.emailBCC() = newStudent.emailAddress.bcc.mkString(",")

        }
      }
    }
    Thread.sleep(interval)
    reload
  }

  reload
}


class StudentsTable(val studentsList: StudentsList)
    extends TableView[StudentData](studentsList.students) {
  columns ++= Seq(
    new TableColumn[StudentData, String] {
      text = "student number"
      cellValueFactory = {  s => s.value.number }
      prefWidth = 100
    },
    new TableColumn[StudentData, String] {
     text = "nickname"
      cellValueFactory = { _.value.nickName }
      prefWidth = 100
    },
    new TableColumn[StudentData, String] {
      text = "name" 
      cellValueFactory = { _.value.name }
      prefWidth = 100
    },
    new TableColumn[StudentData, String] {
      text = "email to"
      cellValueFactory = { _.value.emailTO }
      prefWidth = 100
    },
    new TableColumn[StudentData, String] {
      text = "email cc"
      cellValueFactory = { _.value.emailCC }
      prefWidth = 100
    },
    new TableColumn[StudentData, String] {
      text = "email bcc"
      cellValueFactory = { _.value.emailBCC }
      prefWidth = 100
    }
  )
}
