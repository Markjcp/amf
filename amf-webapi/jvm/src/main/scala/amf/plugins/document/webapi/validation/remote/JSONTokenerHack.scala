package amf.plugins.document.webapi.validation.remote

import java.lang
import org.json.{JSONArray, JSONObject, JSONTokener}
import scala.util.matching.Regex

class JSONTokenerHack(text: String) extends JSONTokener(text) {

  override def nextValue(): Object = nextValueHack()

  /** This is extracted from JSONTokener class to replace the JSONObject.stringToValue method used in the superclass
    * so that we can fail when an unquoted value is processed (as all the other json parsers).
    */
  private def nextValueHack(): Object = hack {
    this.nextClean() match {
      case c @ ('"' | '\'') =>
        this.nextString(c)
      case '{' =>
        this.back()
        new JSONObject(this)
      case '[' =>
        this.back()
        new JSONArray(this)
      case c =>
        val sb      = new StringBuilder()
        var newChar = c
        while (newChar >= ' ' && ",:]}/\\\"[{;=#".indexOf(newChar) < 0) {
          sb.append(newChar)
          newChar = this.next()
        }
        this.back()

        val string = sb.toString.trim()
        if ("" == string) throw this.syntaxError("Missing value")
        stringToValue(string)
    }
  }

  private def checkNumber(s: String): Option[Object] =
    try {
      if (isDecimalNotation(s)) checkDouble(s) else checkLong(s)
    } catch {
      case _: Exception => None
    }

  private def checkLong(s: String) = java.lang.Long.valueOf(s) match {
    case l if s == l.toString => Some(if (l.longValue == l.intValue) Integer.valueOf(l.intValue) else l)
    case _                    => None
  }

  private def checkDouble(s: String) = java.lang.Double.valueOf(s) match {
    case d if !d.isInfinite && !d.isNaN => Some(d)
    case _                              => None
  }

  private def numberOption(s: String): Option[Object] = s.charAt(0) match {
    case i if (i >= '0' && i <= '9') || i == '-' => checkNumber(s)
    case _                                       => None
  }

  private def stringToValue(string: String): Object = string match {
    case ci"true"  => java.lang.Boolean.TRUE
    case ci"false" => java.lang.Boolean.FALSE
    case ci"null"  => JSONObject.NULL
    case _ =>
      numberOption(string) match {
        case Some(o) => o
        case _       => throw this.syntaxError("Unquoted string value")
      }
  }

  private def isDecimalNotation(s: String): Boolean =
    s.indexOf('.') > -1 || s.indexOf('e') > -1 || s.indexOf('E') > -1 || "-0" == s

  private def hack(value: Object) = value match {
    case double: lang.Double => hackDouble(double)
    case _                   => value
  }

  private def hackDouble(d: java.lang.Double): Object = {
    val pattern = "[0-9]+(\\.0+)".r
    d.toString match {
      case pattern(group) =>
        new lang.Integer(d.toString.stripSuffix(group))
      case _ => d
    }
  }

  private implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci: Regex = ("(?i)" + sc.parts.mkString).r
  }
}
