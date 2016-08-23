package global.server

import argonaut.Argonaut._
import argonaut.{EncodeJson, CodecJson}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, Macros, BSONDocumentWriter}

case class AccessToken(id: Long, token: String)

object AccessToken {

  implicit def AccessTokenCodecJson: CodecJson[AccessToken] =
    casecodec2(AccessToken.apply, AccessToken.unapply)("id", "token")

  implicit def AccessTokenEncodeJson: EncodeJson[AccessToken] =
    jencode2L((p: AccessToken) => (p.id, p.token))("id", "token")

  implicit def accessTokenWriter: BSONDocumentWriter[AccessToken] = Macros.writer[AccessToken]

  implicit object AccessTokenReader extends BSONDocumentReader[AccessToken] {
    def read(doc: BSONDocument): AccessToken = {
      val id = doc.getAs[Long]("id").get
      val token = doc.getAs[String]("token").get
      AccessToken(id, token)
    }
  }

}
