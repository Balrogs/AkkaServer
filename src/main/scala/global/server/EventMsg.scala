package global.server

import argonaut.Argonaut._
import argonaut.{CodecJson, EncodeJson}
import global.game.{CustomPlayerView, Rankings}

sealed trait EventMsg {
  def code: Int
}

sealed trait EventMsgSecured {
  def token: AccessToken
}


case class EventType(code: Int) extends EventMsg

case class EventToken(token: AccessToken) extends EventMsgSecured

case class Login(name: String, password: String, playerView: CustomPlayerView) extends EventMsg {
  val code = 1
}

case class RegisterUser(name: String, country: Int, password: String) extends EventMsg {
  val code = 2
}

case class ServerResp(answer: String) extends EventMsg {
  val code = 3
}

case class EnterRoom(player_id: Long, room_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 4
}

case class EnterLobby(player_id: Long, lobby_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 40
}

case class DenyInvite(player_id: Long, room_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 41
}

case class LeaveLobby(player_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 42
}

case class InviteIntoRoom(player_id: Long, player_name: String, player_rank: Int, room_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 5
}

case class GameAction(player_id: Long, room_id: Long, angle: Float, power: Float, x: Int, y: Int, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 60
}

case class GameMove(player_id: Long, room_id: Long, dir: Int, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 61
}

case class GameAim(player_id: Long, room_id: Long, angle: Float, power: Float, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 62
}

case class GameChangeArrow(player_id: Long, room_id: Long, arrow: String, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 63
}

case class GameOver(winner_id: Long, room_id: Long, v_type: Int, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 7
}

case class UserInfoRequest(s_type: Int, name: String) extends EventMsg {
  val code = 8
}

case class GetGameEvent(player_id: Long, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 81
}

case class UserInfo(name: String, country: Int, playerView: CustomPlayerView, friends: Array[FriendInfo], rank: Int, global_rank: Int, country_rank: Int, battles_count: Int, battles_win: Int, battles_loose: Int, date_reg: String) extends EventMsg {
  val code = 9
}

case class FriendInfo(name: String, id: Long, online: Boolean)

case class RankingsRequest(name: String, list: Array[Rankings]) extends EventMsg {
  val code = 10
}

case class RankingsRequestCountry(name: String, list: Array[Rankings]) extends EventMsg {
  val code = 11
}

case class AddToFriends(id: Long, friend_name: String, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 14
}

case class AddEventScore(id: Long, event_id: Long, name: String, country: Int, score: Int, token: AccessToken) extends EventMsg with EventMsgSecured {
  val code = 12
}


object EventType {
  implicit def EventTypeCodecJson: CodecJson[EventType] =
    casecodec1(EventType.apply, EventType.unapply)("code")
}

object EventToken {
  implicit def EventTokenCodecJson: CodecJson[EventToken] =
    casecodec1(EventToken.apply, EventToken.unapply)("token")
}


object Login {
  val code = 1

  implicit def LoginCodecJson: CodecJson[Login] =
    casecodec3(Login.apply, Login.unapply)("name", "password", "playerView")

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
    casecodec3(EnterRoom.apply, EnterRoom.unapply)("player_id", "room_id", "token")
}

object EnterLobby {
  val code = 40

  implicit def EnterLobbyCodecJson: CodecJson[EnterLobby] =
    casecodec3(EnterLobby.apply, EnterLobby.unapply)("player_id", "lobby_id", "token")
}

object DenyInvite {
  val code = 41

  implicit def DenyInviteCodecJson: CodecJson[DenyInvite] =
    casecodec3(DenyInvite.apply, DenyInvite.unapply)("player_id", "room_id", "token")
}

object LeaveLobby {
  val code = 42

  implicit def LeaveLobbyCodecJson: CodecJson[LeaveLobby] =
    casecodec2(LeaveLobby.apply, LeaveLobby.unapply)("player_id", "token")
}

object InviteIntoRoom {
  val code = 5

  implicit def InviteIntoRoomCodecJson: CodecJson[InviteIntoRoom] =
    casecodec5(InviteIntoRoom.apply, InviteIntoRoom.unapply)("player_id", "player_name", "player_rank", "room_id", "token")

  implicit def InviteIntoRoomEncodeJson: EncodeJson[InviteIntoRoom] =
    jencode5L((p: InviteIntoRoom) => (p.player_id, p.player_name, p.player_rank, p.room_id, p.code))("player_id", "player_name", "player_rank", "room_id", "code")
}

object GameAction {
  val code = 60

  implicit def GameActionCodecJson: CodecJson[GameAction] =
    casecodec7(GameAction.apply, GameAction.unapply)("player_id", "room_id", "angle", "power", "x", "y", "token")

  implicit def GameActionEncodeJson: EncodeJson[GameAction] =
    jencode7L((p: GameAction) => (p.player_id, p.room_id, p.angle, p.power, p.x, p.y, p.code))("player_id", "room_id", "angle", "power", "x", "y", "code")
}

object GameMove {
  val code = 61

  implicit def GameMoveCodecJson: CodecJson[GameMove] =
    casecodec4(GameMove.apply, GameMove.unapply)("player_id", "room_id", "dir", "token")

  implicit def GameMoveEncodeJson: EncodeJson[GameMove] =
    jencode4L((p: GameMove) => (p.player_id, p.room_id, p.dir, p.code))("player_id", "room_id", "dir", "code")
}

object GameAim {
  val code = 62

  implicit def GameAimCodecJson: CodecJson[GameAim] =
    casecodec5(GameAim.apply, GameAim.unapply)("player_id", "room_id", "angle", "power", "token")

  implicit def GameAimEncodeJson: EncodeJson[GameAim] =
    jencode5L((p: GameAim) => (p.player_id, p.room_id, p.angle, p.power, p.code))("player_id", "room_id", "angle", "power", "code")
}

object GameChangeArrow {
  val code = 63

  implicit def GameChangeArrowCodecJson: CodecJson[GameChangeArrow] =
    casecodec4(GameChangeArrow.apply, GameChangeArrow.unapply)("player_id", "room_id", "arrow", "token")

  implicit def GameChangeArrowEncodeJson: EncodeJson[GameChangeArrow] =
    jencode5L((p: GameChangeArrow) => (p.player_id, p.room_id, p.arrow, p.token, p.code))("player_id", "room_id", "arrow", "token", "code")
}


object GameOver {
  val code = 7

  implicit def GameOverCodecJson: CodecJson[GameOver] =
    casecodec4(GameOver.apply, GameOver.unapply)("winner_id", "room_id", "v_type", "token")

  implicit def GameOverEncodeJson: EncodeJson[GameOver] =
    jencode5L((p: GameOver) => (p.winner_id, p.room_id, p.v_type, p.token, p.code))("winner_id", "room_id", "v_type", "token", "code")
}

object UserInfoRequest {
  val code = 8

  implicit def UserInfoRequestCodecJson: CodecJson[UserInfoRequest] =

    casecodec2(UserInfoRequest.apply, UserInfoRequest.unapply)("s_type", "name")

}

object UserInfo {
  val code = 9

  implicit def PrivateUserInfoEncodeJson: EncodeJson[UserInfo] =
    jencode12L((p: UserInfo) => (p.name, p.country, p.playerView, p.friends, p.rank, p.global_rank, p.country_rank, p.battles_count, p.battles_win, p.battles_loose, p.date_reg, p.code))("name", "country", "playerView", "friends", "rank", "global_rank", "country_rank", "battles_count", "battles_win", "battles_loose", "date_reg", "code")


  implicit def PublicUserInfoEncodeJson: EncodeJson[UserInfo] =
    jencode11L((p: UserInfo) => (p.name, p.country, p.playerView, p.rank, p.global_rank, p.country_rank, p.battles_count, p.battles_win, p.battles_loose, p.date_reg, p.code))("name", "country", "playerView", "rank", "global_rank", "country_rank", "battles_count", "battles_win", "battles_loose", "date_reg", "code")


}

object FriendInfo {

  implicit def FriendInfoEncodeJson: EncodeJson[FriendInfo] =
    jencode3L((p: FriendInfo) => (p.name, p.id, p.online))("player_name", "player_id", "online")

}

object RankingsRequest {
  val code = 10

  implicit def RankingsRequestEncodeJson: EncodeJson[RankingsRequest] =
    jencode3L((p: RankingsRequest) => (p.name, p.list, p.code))("name", "list", "code")
}

object RankingsRequestCountry {
  val code = 11

  implicit def RankingsRequestCountryEncodeJson: EncodeJson[RankingsRequestCountry] =
    jencode3L((p: RankingsRequestCountry) => (p.name, p.list, p.code))("name", "list", "code")
}

object AddToFriends {
  val code = 14

  implicit def AddToFriendsCodecJson: CodecJson[AddToFriends] =
    casecodec3(AddToFriends.apply, AddToFriends.unapply)("id", "friend_name", "token")
}

object AddEventScore {
  val code = 12

  implicit def AddEventScoreCodecJson: CodecJson[AddEventScore] =
    casecodec6(AddEventScore.apply, AddEventScore.unapply)("id", "event_id", "name", "country", "score", "token")

}

object GetGameEvent {
  val code = 81

  implicit def GetGameEventScoreCodecJson: CodecJson[GetGameEvent] =
    casecodec2(GetGameEvent.apply, GetGameEvent.unapply)("player_id", "token")
}