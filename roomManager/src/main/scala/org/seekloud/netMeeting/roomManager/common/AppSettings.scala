/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.netMeeting.roomManager.common

import java.util.concurrent.TimeUnit

import org.seekloud.netMeeting.roomManager.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import collection.JavaConverters._

/**
  * User: Taoz
  * Date: 9/4/2015
  * Time: 4:29 PM
  */
object AppSettings {
  
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


  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")
  val projectVersion = appConfig.getString("projectVersion")
  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")
  val serverHost = appConfig.getString("server.host")

  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")

  val dependenceConfig = config.getConfig("dependence")
  val RtmpUrlHeader = dependenceConfig.getString("rtmpUrlHeader")

  val processorConfig = dependenceConfig.getConfig("processor")
  val processorIp = processorConfig.getString("ip")
  val processorPort = processorConfig.getString("port")

  val emailConfig = dependenceConfig.getConfig("email")
  val emailHost = emailConfig.getString("host")
  val emailPort = emailConfig.getString("port")
  val emailDomain = emailConfig.getString("domain")
  val emailAddresserEmail = emailConfig.getString("addresserEmail")
  val emailAddresserPwd = emailConfig.getString("addresserPwd")

  val hestiaConfig = dependenceConfig.getConfig("hestia.config")
  val hestiaAppId = hestiaConfig.getString("appId")
  val hestiaSecureKey = hestiaConfig.getString("secureKey")
  val hestiaProtocol = hestiaConfig.getString("protocol")
  val hestiaImgProtocol = hestiaConfig.getString("imgProtocol")
  val hestiaDomain = hestiaConfig.getString("domain")

  val fileConfig = dependenceConfig.getConfig("file")
  val headImgSavePath = fileConfig.getString("headImgPath")
  
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

}
