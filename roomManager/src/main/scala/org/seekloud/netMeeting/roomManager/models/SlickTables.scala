package org.seekloud.netMeeting.roomManager.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tUserInfo.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tUserInfo
   *  @param uid Database column uid SqlType(bigserial), AutoInc, PrimaryKey
   *  @param userName Database column user_name SqlType(varchar), Length(100,true)
   *  @param account Database column account SqlType(varchar), Length(100,true)
   *  @param password Database column password SqlType(varchar), Length(100,true)
   *  @param headImg Database column head_img SqlType(varchar), Length(256,true), Default()
   *  @param coverImg Database column cover_img SqlType(varchar), Length(256,true), Default()
   *  @param createTime Database column create_time SqlType(int8)
   *  @param rtmpUrl Database column rtmp_url SqlType(varchar), Length(100,true) */
  case class rUserInfo(uid: Long, userName: String, account: String, password: String, headImg: String = "", coverImg: String = "", createTime: Long, rtmpUrl: String)
  /** GetResult implicit for fetching rUserInfo objects using plain SQL queries */
  implicit def GetResultrUserInfo(implicit e0: GR[Long], e1: GR[String]): GR[rUserInfo] = GR{
    prs => import prs._
    rUserInfo.tupled((<<[Long], <<[String], <<[String], <<[String], <<[String], <<[String], <<[Long], <<[String]))
  }
  /** Table description of table user_info. Objects of this class serve as prototypes for rows in queries. */
  class tUserInfo(_tableTag: Tag) extends profile.api.Table[rUserInfo](_tableTag, "user_info") {
    def * = (uid, userName, account, password, headImg, coverImg, createTime, rtmpUrl) <> (rUserInfo.tupled, rUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(uid), Rep.Some(userName), Rep.Some(account), Rep.Some(password), Rep.Some(headImg), Rep.Some(coverImg), Rep.Some(createTime), Rep.Some(rtmpUrl))).shaped.<>({r=>import r._; _1.map(_=> rUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column uid SqlType(bigserial), AutoInc, PrimaryKey */
    val uid: Rep[Long] = column[Long]("uid", O.AutoInc, O.PrimaryKey)
    /** Database column user_name SqlType(varchar), Length(100,true) */
    val userName: Rep[String] = column[String]("user_name", O.Length(100,varying=true))
    /** Database column account SqlType(varchar), Length(100,true) */
    val account: Rep[String] = column[String]("account", O.Length(100,varying=true))
    /** Database column password SqlType(varchar), Length(100,true) */
    val password: Rep[String] = column[String]("password", O.Length(100,varying=true))
    /** Database column head_img SqlType(varchar), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("head_img", O.Length(256,varying=true), O.Default(""))
    /** Database column cover_img SqlType(varchar), Length(256,true), Default() */
    val coverImg: Rep[String] = column[String]("cover_img", O.Length(256,varying=true), O.Default(""))
    /** Database column create_time SqlType(int8) */
    val createTime: Rep[Long] = column[Long]("create_time")
    /** Database column rtmp_url SqlType(varchar), Length(100,true) */
    val rtmpUrl: Rep[String] = column[String]("rtmp_url", O.Length(100,varying=true))
  }
  /** Collection-like TableQuery object for table tUserInfo */
  lazy val tUserInfo = new TableQuery(tag => new tUserInfo(tag))
}
