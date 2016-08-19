package global.server

import argonaut.Argonaut._
import argonaut.{CodecJson, EncodeJson}
import global.game.Rankings

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

case class GameOver(winner_id: Long, room_id: Long, v_type: Int) extends EventMsg {
  val code = 7
}

case class PlayersStats(name: String, rank: Int,  global_rank: Int, country_rank: Int,battles_count: Int, battles_win: Int, battles_loose: Int, date_reg: String) extends EventMsg {
  val code = 8
}

case class StatsRequest(s_type: Int, name: String) extends EventMsg {
  val code = 9
}

case class RankingsRequest(name:String, list:Array[Rankings]) extends EventMsg {
  val code = 10
}

case class AddToFriends(id:Long, friend_id:Long, add:Boolean) extends EventMsg {
  val code = 11
}

case class AddEventScore(id:Long, event_id:Long, score:Int) extends EventMsg {
  val code = 12
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
    casecodec3(GameOver.apply, GameOver.unapply)("winner_id", "room_id", "v_type")

  implicit def GameOverEncodeJson: EncodeJson[GameOver] =
    jencode4L((p: GameOver) => (p.winner_id, p.room_id, p.v_type, p.code))("winner_id", "room_id", "v_type", "code")
}

object PlayersStats {
  val code = 8

  implicit def PlayersStatsEncodeJson: EncodeJson[PlayersStats] =
    jencode9L((p: PlayersStats) => (p.name, p.rank, p.global_rank, p.country_rank, p.battles_count, p.battles_win, p.battles_loose, p.date_reg, p.code))("name", "rank","global_rank","country_rank", "battles_count", "battles_win", "battles_loose", "date_reg", "code")
}

object StatsRequest {
  val code = 9

  implicit def StatsRequestCodecJson: CodecJson[StatsRequest] =
    casecodec2(StatsRequest.apply, StatsRequest.unapply)("s_type","name")
}

object RankingsRequest {
  val code = 10

  implicit def RankingsRequestEncodeJson: EncodeJson[RankingsRequest] =
    jencode3L((p: RankingsRequest) => (p.name, p.list, p.code))("name", "list","code")
}

object AddToFriends {
  val code = 11

  implicit def AddToFriendsCodecJson: CodecJson[AddToFriends] =
    casecodec3(AddToFriends.apply, AddToFriends.unapply)("id","friend_id", "add")
}

object AddEventScore {
  val code = 12

  implicit def AddEventScoreCodecJson: CodecJson[AddEventScore] =
    casecodec3(AddEventScore.apply, AddEventScore.unapply)("id","event_id", "score")

}