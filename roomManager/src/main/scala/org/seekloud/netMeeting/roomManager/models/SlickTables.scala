package org.seekloud.netMeeting.roomManager.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.H2Profile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tMatchInfo.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tMatchInfo
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param srcVideoName Database column SRC_VIDEO_NAME SqlType(VARCHAR), Length(1024,true)
   *  @param tstVideoName Database column TST_VIDEO_NAME SqlType(VARCHAR), Length(1024,true)
   *  @param similarity Database column SIMILARITY SqlType(DOUBLE)
   *  @param success Database column SUCCESS SqlType(BOOLEAN), Default(false)
   *  @param createTime Database column CREATE_TIME SqlType(BIGINT)
   *  @param wrongCode Database column WRONG_CODE SqlType(BIGINT)
   *  @param wrongMessage Database column WRONG_MESSAGE SqlType(VARCHAR), Length(256,true), Default()
   *  @param submitTime Database column SUBMIT_TIME SqlType(BIGINT), Default(-1)
   *  @param finishTime Database column FINISH_TIME SqlType(BIGINT), Default(-1)
   *  @param waitTime Database column WAIT_TIME SqlType(BIGINT), Default(-1)
   *  @param runningTime Database column RUNNING_TIME SqlType(BIGINT), Default(-1) */
  case class rMatchInfo(id: Long, srcVideoName: String, tstVideoName: String, similarity: Double, success: Boolean = false, createTime: Long, wrongCode: Long, wrongMessage: String = "", submitTime: Long = -1L, finishTime: Long = -1L, waitTime: Long = -1L, runningTime: Long = -1L)
  /** GetResult implicit for fetching rMatchInfo objects using plain SQL queries */
  implicit def GetResultrMatchInfo(implicit e0: GR[Long], e1: GR[String], e2: GR[Double], e3: GR[Boolean]): GR[rMatchInfo] = GR{
    prs => import prs._
    rMatchInfo.tupled((<<[Long], <<[String], <<[String], <<[Double], <<[Boolean], <<[Long], <<[Long], <<[String], <<[Long], <<[Long], <<[Long], <<[Long]))
  }
  /** Table description of table MATCH_INFO. Objects of this class serve as prototypes for rows in queries. */
  class tMatchInfo(_tableTag: Tag) extends profile.api.Table[rMatchInfo](_tableTag, Some("PUBLIC"), "MATCH_INFO") {
    def * = (id, srcVideoName, tstVideoName, similarity, success, createTime, wrongCode, wrongMessage, submitTime, finishTime, waitTime, runningTime) <> (rMatchInfo.tupled, rMatchInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(srcVideoName), Rep.Some(tstVideoName), Rep.Some(similarity), Rep.Some(success), Rep.Some(createTime), Rep.Some(wrongCode), Rep.Some(wrongMessage), Rep.Some(submitTime), Rep.Some(finishTime), Rep.Some(waitTime), Rep.Some(runningTime))).shaped.<>({r=>import r._; _1.map(_=> rMatchInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column SRC_VIDEO_NAME SqlType(VARCHAR), Length(1024,true) */
    val srcVideoName: Rep[String] = column[String]("SRC_VIDEO_NAME", O.Length(1024,varying=true))
    /** Database column TST_VIDEO_NAME SqlType(VARCHAR), Length(1024,true) */
    val tstVideoName: Rep[String] = column[String]("TST_VIDEO_NAME", O.Length(1024,varying=true))
    /** Database column SIMILARITY SqlType(DOUBLE) */
    val similarity: Rep[Double] = column[Double]("SIMILARITY")
    /** Database column SUCCESS SqlType(BOOLEAN), Default(false) */
    val success: Rep[Boolean] = column[Boolean]("SUCCESS", O.Default(false))
    /** Database column CREATE_TIME SqlType(BIGINT) */
    val createTime: Rep[Long] = column[Long]("CREATE_TIME")
    /** Database column WRONG_CODE SqlType(BIGINT) */
    val wrongCode: Rep[Long] = column[Long]("WRONG_CODE")
    /** Database column WRONG_MESSAGE SqlType(VARCHAR), Length(256,true), Default() */
    val wrongMessage: Rep[String] = column[String]("WRONG_MESSAGE", O.Length(256,varying=true), O.Default(""))
    /** Database column SUBMIT_TIME SqlType(BIGINT), Default(-1) */
    val submitTime: Rep[Long] = column[Long]("SUBMIT_TIME", O.Default(-1L))
    /** Database column FINISH_TIME SqlType(BIGINT), Default(-1) */
    val finishTime: Rep[Long] = column[Long]("FINISH_TIME", O.Default(-1L))
    /** Database column WAIT_TIME SqlType(BIGINT), Default(-1) */
    val waitTime: Rep[Long] = column[Long]("WAIT_TIME", O.Default(-1L))
    /** Database column RUNNING_TIME SqlType(BIGINT), Default(-1) */
    val runningTime: Rep[Long] = column[Long]("RUNNING_TIME", O.Default(-1L))
  }
  /** Collection-like TableQuery object for table tMatchInfo */
  lazy val tMatchInfo = new TableQuery(tag => new tMatchInfo(tag))
}
