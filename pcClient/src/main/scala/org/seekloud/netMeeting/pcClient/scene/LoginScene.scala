package org.seekloud.netMeeting.pcClient.scene

import java.awt.Desktop
import java.io.File
import java.net.URI

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.control.{Button, Label, PasswordField, TextField}
import javafx.scene.image.Image
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.{Group, Scene}
import javafx.stage.Stage
import org.seekloud.netMeeting.pcClient.common.Routes

/**
  * @user: wanruolong
  * @date: 2020/1/16 11:09
  *
  */

object LoginScene{
  trait LoginStageListener{
    def login(username: String, password: String)
  }
}

class LoginScene(stage: Stage) {
  import LoginScene._

  var listener: LoginStageListener = _

  val width = 400
  val height = 240
  val group = new Group()
  val scene = new Scene(group, width, height)
  val labelUsername = new Label("用户名：")
  val labelPassword  = new Label("密  码：")
  val userText = new TextField()
  userText.setText("shining")
  val passwordText = new PasswordField()
  passwordText.setText("123")
  val loginButton = new Button("登录")
  val logUpButton = new Button("注册")
  val nameBox = new HBox(labelUsername, userText)
  nameBox.setAlignment(Pos.CENTER)
  val passwordBox = new HBox(labelPassword, passwordText)
  passwordBox.setAlignment(Pos.CENTER)
  passwordBox.setSpacing(5)
  val buttonBox = new HBox(loginButton, logUpButton)
  buttonBox.setSpacing(30)
  buttonBox.setAlignment(Pos.CENTER)

  val errorLabel = new Label("登录失败！")
  errorLabel.setTextFill(Color.RED)
  errorLabel.setAlignment(Pos.CENTER)
  errorLabel.setVisible(false)
  loginButton.setDefaultButton(true)
  loginButton.setOnAction { event =>
//    println("button clicked")
    val username = userText.getText()
    val password = passwordText.getText()
    errorLabel.setVisible(false)
    listener.login(username, password)
  }

  logUpButton.setOnAction{ event =>
    val url = Routes.signUp
    val desktop = Desktop.getDesktop
    if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
      val uri = new URI(url)
      desktop.browse(uri)
    }
  }

  val inputBox = new VBox(nameBox, passwordBox, errorLabel)
  inputBox.setAlignment(Pos.CENTER)
  inputBox.setSpacing(15)
  val vBox = new VBox(inputBox, buttonBox)
  vBox.setSpacing(30)
  val anchorPane = new AnchorPane(vBox)
  anchorPane.setLayoutX((width-270)/2)
  anchorPane.setLayoutY((height-125)/2)
  group.getChildren.add(anchorPane)

  def getScene = this.scene

  def loginError() = {
    errorLabel.setVisible(true)
  }

  def close() = {
    stage.close()
  }

  def setListener(listener: LoginStageListener) = {
    this.listener = listener
  }

}
