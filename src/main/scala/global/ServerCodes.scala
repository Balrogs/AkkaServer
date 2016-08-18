package global


trait ServerCodes {
  def code: Long
}

case class AuthResp(code:Long) extends ServerCodes

case object LoginError extends ServerCodes { val code = -100l}
case object RegisterErrorNameExists extends ServerCodes { val code = -201l}
case object GameStarted extends ServerCodes { val code = -301l}
case object GamePaused extends ServerCodes { val code = -302l}
case object GameResumed extends ServerCodes { val code = -303l}
case object ServerConnectionError extends ServerCodes { val code = -400l}
case object UserEnteredRoom extends ServerCodes { val code = -500l}
case object UserNotFound extends ServerCodes { val code = -600l}