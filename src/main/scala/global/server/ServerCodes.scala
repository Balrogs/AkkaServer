package global.server

trait ServerCodes {
  def code: String
}

case class AuthResp(code: String) extends ServerCodes

case object LoginError extends ServerCodes {
  val code = "-100"
}

case object RegisterErrorNameExists extends ServerCodes {
  val code = "-201"
}

case object GameStarted extends ServerCodes {
  val code = "-301"
}

case object GamePaused extends ServerCodes {
  val code = "-302"
}

case object GameResumed extends ServerCodes {
  val code = "-303"
}

case object GameAborted extends ServerCodes {
  val code = "-304"
}

case object ServerConnectionError extends ServerCodes {
  val code = "-400"
}

case object UserEnteredRoom extends ServerCodes {
  val code = "-500"
}

case object UserNotFound extends ServerCodes {
  val code = "-600"
}

case object UserEnteredLobby extends ServerCodes {
  val code = "-700"
}
