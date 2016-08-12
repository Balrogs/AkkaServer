package global

import argonaut.Argonaut._
import argonaut.{EncodeJson, CodecJson}


sealed  trait EventMsg
{
  def code : Int
}
  case class EventType(code:Int) extends  EventMsg

  case class Login(id: Long, password: String) extends EventMsg {
    val code = 1
  }

  case class RegisterUser(id: Long, name: String, country: Int, password: String) extends EventMsg {
    val code = 2
  }

  case class AuthResp(answer: Boolean, token: String) extends EventMsg {
    val code = 3
  }

  case class EnterRoom(player_id: Long, room_id: Long, token: String) extends EventMsg {
    val code = 4
  }

  case class InviteIntoRoom(player_id: Long, player_rank: Int, token: String, room_id: Long) extends EventMsg {
    val code = 5
  }

  case class GameAction(player_id: Long, room_id:Long, angle: Double, power: Double, arrow: String, token: String) extends EventMsg {
    val code = 6
  }

  case class GameOver(victory: Boolean) extends EventMsg {
    val code = 7
  }


object EventType {
  implicit def EventTypeCodecJson: CodecJson[EventType] =
    casecodec1(EventType.apply, EventType.unapply)("code")
}


object Login {
  val code = 1
  implicit def LoginCodecJson: CodecJson[Login] =
    casecodec2(Login.apply, Login.unapply)("id", "password")

}

object RegisterUser {
  val code = 2
  implicit def RegisterUserCodecJson: CodecJson[RegisterUser] =
    casecodec4(RegisterUser.apply, RegisterUser.unapply)("id","name", "country", "password")
}

object AuthResp {
  val code = 3
  implicit def AuthRespEncodeJson: EncodeJson[AuthResp] =
    jencode3L((p: AuthResp) => (p.answer, p.token, p.code))("answer", "token", "code")
}

object EnterRoom {
  val code = 4
  implicit def EnterRoomCodecJson: CodecJson[EnterRoom] =
    casecodec3(EnterRoom.apply, EnterRoom.unapply)("player_id", "room_id", "token")
}

object InviteIntoRoom {
  val code = 5
  implicit def InviteIntoRoomCodecJson: CodecJson[InviteIntoRoom] =
    casecodec4(InviteIntoRoom.apply, InviteIntoRoom.unapply)("player_id","player_rank","token", "room_id")
  implicit def InviteIntoRoomEncodeJson: EncodeJson[InviteIntoRoom] =
    jencode5L((p: InviteIntoRoom) => (p.player_id, p.player_rank, p.token, p.room_id, p.code))("player_id", "player_rank", "token", "room_id", "code")
}

object GameAction {
  val code = 6
  implicit def GameActionCodecJson: CodecJson[GameAction] =
    casecodec6(GameAction.apply, GameAction.unapply)("player_id","room_id", "angle", "power", "arrow", "token")
  implicit def GameActionEncodeJson: EncodeJson[GameAction] =
    jencode7L((p: GameAction) => (p.player_id,p.room_id,p.angle,p.power,p.arrow,p.token, p.code))("player_id","room_id","angle", "power", "arrow", "token", "code")
}

object GameOver {
  val code = 7
  implicit def GameOverEncodeJson: EncodeJson[GameOver] =
    jencode2L((p: GameOver) => (p.victory, p.code))("victory", "code")
}
