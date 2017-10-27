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
case class LastEntryTime(time:LocalDateTime)
case class LastExitTime(time:LocalDateTime)

case class EntryEvent(time:LocalDateTime, student: Student)
case class ExitEvent(time:LocalDateTime, Student: Student)

case class EntryRecord(entryTimes: List[LocalDateTime] = Nil, exitTimes: List[LocalDateTime] = Nil, isExist : Boolean = false){
  def addEntry(entryEvent:EntryEvent) : EntryRecord =
    copy(entryEvent.time :: entryTimes, exitTimes, true)
  def addExit(exitEvent:ExitEvent) : EntryRecord =
    copy(entryTimes, exitEvent.time :: exitTimes, false)

  def lastEntryTime : LocalDateTime = entryTimes.head
  def lastExitTime : LocalDateTime = exitTimes.head
}

class EntryController(val student : Student)
    extends PersistentActor {
  override def persistenceId = s"entry-controller-${student.number}-${student.nickName}"

  var entryRecord = EntryRecord()
  var lastUpdateTime : LocalDateTime = null
  def entryUpdateTimeFrom : LocalDateTime = 
    LocalDateTime.of(LocalDateTime.now.toLocalDate(),
                     LocalTime.of(12,0))
  def entryUpdateTimeTo : LocalDateTime =
    LocalDateTime.of(LocalDateTime.now.toLocalDate(),
                     LocalTime.of(18,30))

  def exitUpdateTimeFrom : LocalDateTime =
    LocalDateTime.of(LocalDateTime.now.toLocalDate(),
                   LocalTime.of(18,30))

  def exitUpdateTimeTo : LocalDateTime =
    LocalDateTime.of(LocalDateTime.now.toLocalDate(),
                   LocalTime.of(23,0))

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
      var canUpdate = true
      val updateInterval = 120
      if( lastUpdateTime != null ){ 
        canUpdate = (ChronoUnit.SECONDS.between(lastUpdateTime, nowTime)
                       > updateInterval)
      }
      lastUpdateTime = nowTime

      ChronoUnit.MINUTES.between(exitUpdateTimeFrom, nowTime) > 0
      

      if( canUpdate ){
        if( ChronoUnit.MINUTES.between(entryUpdateTimeFrom, nowTime) > 0
             && ChronoUnit.MINUTES.between(nowTime, entryUpdateTimeTo) > 0
        ){
          println(s"set entry! ${student.name}")
          persist(EntryEvent(nowTime, student)){ event =>
            entryRecord = entryRecord.addEntry(event)
            sender ! event
            if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
              saveSnapshot(entryRecord)
          }
        }else if(
          ChronoUnit.MINUTES.between(exitUpdateTimeFrom, nowTime) > 0
            && ChronoUnit.MINUTES.between(nowTime, exitUpdateTimeTo) > 0
           ){
          println(s"set exit! ${student.name}")
          persist(ExitEvent(nowTime, student)){ event =>
            entryRecord = entryRecord.addExit(event)
            sender ! event
            if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
              saveSnapshot(entryRecord)
          }
        }else {
          sender ! Reload
        }
      }else{
        sender ! Reload
      }
  }
}
