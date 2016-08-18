package global

import argonaut.Argonaut._
import argonaut.{EncodeJson, CodecJson}

sealed trait EventMsg {
  def code: Int
}

case class EventType(code: Int) extends EventMsg

case class Login(id: Long, password: String) extends EventMsg {
  val code = 1
}

case class RegisterUser(name: String, country: Int, password: String) extends EventMsg {
  val code = 2
}

case class ServerResp(answer: Long) extends EventMsg {
  val code = 3
}

case class EnterRoom(player_id: Long, room_id: Long) extends EventMsg {
  val code = 4
}

case class InviteIntoRoom(player_name: String, player_rank: Int, room_id: Long) extends EventMsg {
  val code = 5
}

case class GameAction(player_id: Long, room_id: Long, angle: Double, power: Double, arrow: String) extends EventMsg {
  val code = 6
}

case class GameOver(winner_id: Long, room_id: Long) extends EventMsg {
  val code = 7
}

case class PlayersStats(name: String, rank: Int, battles_count: Int, battles_win: Int, battles_loose: Int, date_reg: String) extends EventMsg {
  val code = 8
}

case class StatsRequest(name: String) extends EventMsg {
  val code = 9
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
    casecodec3(RegisterUser.apply, RegisterUser.unapply)("name", "country", "password")
}

object ServerResp {
  val code = 3

  implicit def ServerRespEncodeJson: EncodeJson[ServerResp] =
    jencode2L((p: ServerResp) => (p.answer, p.code))("answer", "code")
}

object EnterRoom {
  val code = 4

  implicit def EnterRoomCodecJson: CodecJson[EnterRoom] =
    casecodec2(EnterRoom.apply, EnterRoom.unapply)("player_id", "room_id")
}

object InviteIntoRoom {
  val code = 5

  implicit def InviteIntoRoomCodecJson: CodecJson[InviteIntoRoom] =
    casecodec3(InviteIntoRoom.apply, InviteIntoRoom.unapply)("player_name", "player_rank", "room_id")

  implicit def InviteIntoRoomEncodeJson: EncodeJson[InviteIntoRoom] =
    jencode4L((p: InviteIntoRoom) => (p.player_name, p.player_rank, p.room_id, p.code))("player_name", "player_rank", "room_id", "code")
}

object GameAction {
  val code = 6

  implicit def GameActionCodecJson: CodecJson[GameAction] =
    casecodec5(GameAction.apply, GameAction.unapply)("player_id", "room_id", "angle", "power", "arrow")

  implicit def GameActionEncodeJson: EncodeJson[GameAction] =
    jencode6L((p: GameAction) => (p.player_id, p.room_id, p.angle, p.power, p.arrow, p.code))("player_id", "room_id", "angle", "power", "arrow", "code")
}

object GameOver {
  val code = 7

  implicit def GameOverCodecJson: CodecJson[GameOver] =
    casecodec2(GameOver.apply, GameOver.unapply)("winner_id", "room_id")

  implicit def GameOverEncodeJson: EncodeJson[GameOver] =
    jencode3L((p: GameOver) => (p.winner_id, p.room_id, p.code))("winner_id", "room_id", "code")
}

object PlayersStats {
  val code = 8

  implicit def PlayersStatsEncodeJson: EncodeJson[PlayersStats] =
    jencode7L((p: PlayersStats) => (p.name, p.rank, p.battles_count, p.battles_win, p.battles_loose, p.date_reg, p.code))("name", "rank", "battles_count", "battles_win", "battles_loose", "date_reg", "code")
}

object StatsRequest {
  val code = 9

  implicit def StatsRequestCodecJson: CodecJson[StatsRequest] =
    casecodec1(StatsRequest.apply, StatsRequest.unapply)("name")
}