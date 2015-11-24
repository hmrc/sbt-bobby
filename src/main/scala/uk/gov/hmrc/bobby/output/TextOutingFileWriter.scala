/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bobby.output

import java.io.File
import java.nio.file.Files

import sbt.ConsoleLogger
import uk.gov.hmrc.bobby.Message
import uk.gov.hmrc.bobby.conf.Configuration

class TextOutingFileWriter(filepath:String) {

  private val logger = ConsoleLogger()

  def outputMessagesToTextFile(messages: List[Message]) = {
    logger.info("[bobby] Output file set to: " + filepath)
    outputToFile(filepath, renderText(messages))
  }

  def renderText(messages: List[Message]): String = {

    val messageModel = messages
      .sorted
      .map { m => m.longTabularOutput }

    Tabulator.format(Message.tabularHeader +: messageModel)
  }

  private def outputToFile(filepath: String, textString: String) = {
    val file: File = new File(filepath)
    file.getParentFile.mkdirs()
    logger.debug("[bobby] Writing Bobby report to: " + file.getAbsolutePath);

    Files.write(file.toPath, textString.getBytes)
  }
}
//
// heavily influenced by http://stackoverflow.com/a/7542476/599068.
//
// - changed formatting to be left justified
// - updated cell separators to be padded by adding extra spaces and dashes in mkString calls
//
object Tabulator {

  def format(table: Seq[Seq[Any]]): String = formatAsStrings(table).mkString("\n")

  def formatAsStrings(table: Seq[Seq[Any]]): Seq[String] = table match {
    case Seq() => Seq()
    case _ =>
      val sizes = for (row <- table) yield (for (cell <- row) yield if (cell == null) 0 else cell.toString.length)
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows = for (row <- table) yield formatRow(row, colSizes)
      formatRows(rowSeparator(colSizes), rows)
  }

  def formatRows(rowSeparator: String, rows: Seq[String]): Seq[String] = rowSeparator ::
    rows.head ::
    rowSeparator ::
    rows.tail.toList :::
    rowSeparator ::
    List()


  def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
    val cells = (for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + -size + "s").format(item))
    cells.mkString("| ", " | ", " |")
  }

  def rowSeparator(colSizes: Seq[Int]) = colSizes map { "-" * _ } mkString("+-", "-+-", "-+")
}
