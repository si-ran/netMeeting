package org.seekloud.netMeeting.processor.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import org.seekloud.netMeeting.processor.utils.SessionSupport.SessionConfig
import org.slf4j.LoggerFactory

/**
  * User: cq
  * Date: 2020/1/16
  */
object AppSettings {
  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("application.conf").withFallback(ConfigFactory.load())

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }

  val sessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }
  val appConfig = config.getConfig("app")

  val projectVersion = appConfig.getString("projectVersion")
  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val srsServerUrl = appConfig.getString("server.host")

  val appId = appConfig.getString("appId")
  val secureKey = appConfig.getString("secureKey")
  val rtpToHost = appConfig.getString("rtpToHost")
  val isRecord = appConfig.getBoolean("isRecord")
  val addTs = appConfig.getBoolean("addTs")
  val distributorHost = appConfig.getString("distributorHost")
  val roomManagerHost = appConfig.getString("roomManagerHost")
  val rtpServerDst = appConfig.getString("rtpServerDst")
  val bitRate = appConfig.getInt("bitRate")
  val imageWidth = appConfig.getInt("imageWidth")
  val imageHeight = appConfig.getInt("imageHeight")
  val isTest = appConfig.getBoolean("isTest")
  val debugPath = appConfig.getString("debugPath")
  val isDebug = appConfig.getBoolean("isDebug")

  //slick
  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")



}
