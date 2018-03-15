package techin

import akka.persistence.{PersistentActor, SnapshotOffer}
import java.time.{
  LocalDateTime,
  LocalDate,
  LocalTime
}
import java.time.temporal.ChronoUnit

case object EntryManage
case object GetLastEntryTime
case object GetLastExitTime
case class LastEntryTime(time:Option[LocalDateTime])
case class LastExitTime(time:Option[LocalDateTime])


case class EntryEvent(time:LocalDateTime, student: StudentVer2)
case class ExitEvent(time:LocalDateTime, Student: StudentVer2)

case class EntryRecord(entryTimes: List[LocalDateTime] = Nil, exitTimes: List[LocalDateTime] = Nil, isExist : Boolean = false){
  def addEntry(entryEvent:EntryEvent) : EntryRecord = {
    val noCheckExistEntryRecord = copy(entryEvent.time :: entryTimes, exitTimes, true)
    copy(noCheckExistEntryRecord.entryTimes,
         noCheckExistEntryRecord.exitTimes,
         noCheckExistEntryRecord.updateIsExist
    )
  }
  def addExit(exitEvent:ExitEvent) : EntryRecord = {
    val noCheckExistEntryRecord = copy(entryTimes, exitEvent.time :: exitTimes, true)
    copy(noCheckExistEntryRecord.entryTimes,
         noCheckExistEntryRecord.exitTimes,
         noCheckExistEntryRecord.updateIsExist
    )
  }


  def lastEntryTime : Option[LocalDateTime] =
    if(entryTimes.isEmpty) None else Some(entryTimes.head)

  def lastExitTime : Option[LocalDateTime] =
    if(exitTimes.isEmpty) None else Some(exitTimes.head)

  def lastUpdateTime : Option[LocalDateTime] =
    (lastEntryTime, lastExitTime) match {
      case (None, None) => None
      case (Some(entryTime), None) => Some(entryTime)
      case (None,Some(exitTime)) => Some(exitTime)
      case (Some(entryTime), Some(exitTime)) =>
        if(entryTime isAfter exitTime) Some(entryTime) else Some(exitTime)
    }

  def updateIsExist : Boolean =
    (lastEntryTime, lastExitTime) match {
      case (None, None) => false
      case (Some(entryTime), None) => true
      case (None,Some(exitTime)) => false
      case (Some(entryTime), Some(exitTime)) =>
        if(entryTime isAfter exitTime) true else false
    }
}

class EntryController(val student : StudentVer2)
    extends PersistentActor {
  override def persistenceId = s"entry-controller3-${student.number}-${student.nickName}"

  var entryRecord = EntryRecord()

  val receiveRecover: Receive = {
    case entryEvent:EntryEvent =>
      entryRecord = entryRecord.addEntry(entryEvent)
    case exitEvent:ExitEvent =>
      entryRecord = entryRecord.addExit(exitEvent)
    case SnapshotOffer(_, snapShot: EntryRecord) =>
      entryRecord = entryRecord
  }

  val snapShotInterval = 30
  val receiveCommand : Receive = {
    case GetLastEntryTime =>
      sender ! LastEntryTime(entryRecord.lastEntryTime)

    case GetLastExitTime =>
      sender ! LastExitTime(entryRecord.lastExitTime)

    case EntryManage =>
      val nowTime = LocalDateTime.now()
      val updateInterval = 300



      if( entryRecord.lastUpdateTime.map(lastUpdateTime => ChronoUnit.SECONDS.between(lastUpdateTime, nowTime) > updateInterval ).getOrElse(true) ){
        // when student don't exist or
        // when student exist but the entry time of student is not today,
        // entryManager add entry Time
        if( !entryRecord.isExist
             || entryRecord.lastEntryTime.map(lastEntryTime =>
               lastEntryTime.toLocalDate != nowTime.toLocalDate).getOrElse(false)
        ){
          // add entry
          println(s"set entry! ${student.name}")
          persist(EntryEvent(nowTime, student)){ event =>
            entryRecord = entryRecord.addEntry(event)
            sender ! event
            if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
              saveSnapshot(entryRecord)
          }
        }else{
          // add exit
          println(s"set exit! ${student.name}")
          persist(ExitEvent(nowTime, student)){ event =>
            entryRecord = entryRecord.addExit(event)
            sender ! event
            if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
              saveSnapshot(entryRecord)
          }
        }
        
      }else{
        sender ! Reload
      }

  }
}
