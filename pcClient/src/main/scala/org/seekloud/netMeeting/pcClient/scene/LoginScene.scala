package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.control.{Button, Label, PasswordField, TextField}
import javafx.scene.image.Image
import javafx.scene.layout.{AnchorPane, HBox, VBox}
import javafx.scene.{Group, Scene}
import javafx.stage.Stage

/**
  * @user: wanruolong
  * @date: 2020/1/16 11:09
  *
  */

object LoginScene{
  trait LoginStageListener{
    def login()
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
  val passwordText = new PasswordField()
  val loginButton = new Button("登录")
  val nameBox = new HBox(labelUsername, userText)
  nameBox.setAlignment(Pos.CENTER)
  val passwordBox = new HBox(labelPassword, passwordText)
  passwordBox.setAlignment(Pos.CENTER)
  passwordBox.setSpacing(5)
  val buttonBox = new HBox(loginButton)
  buttonBox.setAlignment(Pos.CENTER)

  loginButton.setDefaultButton(true)
  loginButton.setOnAction { event =>
    println("button clicked")
    listener.login()
  }

  val inputBox = new VBox(nameBox, passwordBox)
  inputBox.setAlignment(Pos.CENTER)
  inputBox.setSpacing(15)
  val vBox = new VBox(inputBox, buttonBox)
  vBox.setSpacing(30)
  val anchorPane = new AnchorPane(vBox)
  anchorPane.setLayoutX((width-270)/2)
  anchorPane.setLayoutY((height-125)/2)
  group.getChildren.add(anchorPane)

  def getScene = this.scene

  def close() = {
    stage.close()
  }

  def setListener(listener: LoginStageListener) = {
    this.listener = listener
  }

}
