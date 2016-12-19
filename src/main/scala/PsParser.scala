package main.scala

import java.io.{BufferedReader, InputStreamReader}

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, PlatformDataKeys}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.unscramble.AnalyzeStacktraceUtil

/**
  * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
  * Date: 12/16/16
  * Time: 10:20 PM
  */
trait ProcessExecutor {
  val pb = new ProcessBuilder()

  def init(): Unit

  private def readToListOfStrings(reader: BufferedReader, acc: List[String]): List[String] = {
    val line = reader.readLine()
    if (line != null) {
      readToListOfStrings(reader, line :: acc)
    } else {
      acc
    }
  }

  lazy val output: List[String] = {
    init()
    val process = pb.start()
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    readToListOfStrings(reader, Nil)
  }
}

class OpenStacks extends AnAction {
  override def actionPerformed(event: AnActionEvent): Unit = {
    val project = event.getProject
    val psSubstring = Messages.showInputDialog(project,
      "What processes do you wish to grep?",
      "Input process name substring",
      Messages.getQuestionIcon)
    val traces = PsParser.getStackTraces(psSubstring)
    traces.foreach(
      th =>
        AnalyzeStacktraceUtil.addConsole(project, null, th._1, th._2.getStack)
    )
  }
}

class PsParser extends ProcessExecutor {

  override def init(): Unit = pb.command("ps", "aux")

  def grepProcess(substring: String): List[String] = {
    output.
      filter(_.contains("java")).
      filter(_.contains(substring)).
      map(_.split("\\s+")(1))
  }
}

class ThreadDumper(val pid: String) extends ProcessExecutor {
  override def init(): Unit = pb.command("jstack", pid)

  def getStack: String = {
    output.mkString("\n")
  }
}

object PsParser {
  def main(args: Array[String]): Unit = {
    val dumpers: List[(String, ThreadDumper)] = getStackTraces("java")
    println(
      dumpers.map(_._2.getStack).mkString("\n\n==============================\n\n")
    )
  }

  def getStackTraces(substring : String) : List[(String, ThreadDumper)] = {
    val processes = new PsParser().grepProcess(substring)
    processes.map(p => (p, new ThreadDumper(p)))
  }
}
