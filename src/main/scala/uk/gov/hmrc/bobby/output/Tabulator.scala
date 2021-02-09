/*
 * Copyright 2021 HM Revenue & Customs
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

//
// heavily influenced by http://stackoverflow.com/a/7542476/599068.
//
// - changed formatting to be left justified
// - updated cell separators to be padded by adding extra spaces and dashes in mkString calls
//
object Tabulator {

  def format(table: Seq[Seq[fansi.Str]]): String = formatAsStrings(table).mkString("\n")

  def formatAsStrings(table: Seq[Seq[fansi.Str]]): Seq[String] = table match {
    case Seq() => Seq()
    case _ =>
      val sizes    = for (row <- table) yield (for (cell <- row) yield if (cell == null) 0 else {
        cell.length
      })
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows     = for (row <- table) yield formatRow(row, colSizes)
      formatRows(fansi.Str(rowSeparator(colSizes)), rows).map(_.render)
  }

  def formatRows(rowSeparator: fansi.Str, rows: Seq[fansi.Str]): Seq[fansi.Str] =
    rowSeparator ::
      rows.head ::
      rowSeparator ::
      rows.tail.toList :::
      rowSeparator ::
      List()

  def formatRow(row: Seq[fansi.Str], colSizes: Seq[Int]): fansi.Str = {
    val cells = (for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else  item + (" " * (size-item.length)))
    cells.mkString("| ", " | ", " |")
  }

  def rowSeparator(colSizes: Seq[Int]) = colSizes map { "-" * _ } mkString ("+-", "-+-", "-+")
}
