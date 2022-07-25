package org.virtuslab.ideprobe.dependencies.git

import org.eclipse.jgit.api.{CloneCommand, CreateBranchCommand, Git}
import org.eclipse.jgit.lib.{ProgressMonitor, Ref}

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import org.virtuslab.ideprobe.Extensions._

object GitHandler {

  def clone(repository: String, workspace: Path): IdeProbeGit = {
    val command: CloneCommand = new CloneCommand()
      .setURI(repository)
      .setDirectory(workspace.toFile)
      .setCloneAllBranches(true)
      .setMirror(false)
      .setBare(false)
      .setProgressMonitor(new CustomProgressMonitor(repository))
    val res =
      try { command.call() }
      catch {
        case e: Exception => throw new IllegalStateException(s"Could not clone git $repository: ${e.getMessage}")
      }
    new IdeProbeGit(res)
  }

  def clone(repository: URI, workspace: Path): IdeProbeGit = clone(repository.toString, workspace)

  def checkout(git: Git, ref: String): Ref = {
    val checkoutBase = git.checkout()
    val checkoutCmd =
      if (git.getRepository.resolve(ref) != null)
        checkoutBase.setName(ref)
      else
        checkoutBase
          .setCreateBranch(true)
          .setName(ref)
          .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
          .setStartPoint(s"origin/$ref")
    try { checkoutCmd.call() }
    catch {
      case e: Exception =>
        throw new IllegalStateException(s"Could not checkout $ref in ${git.getRepository}: ${e.getMessage}")
    }
  }

  def commitHash(remote: String, ref: String): Option[String] = {
    val lsRemoteCmd = Git.lsRemoteRepository().setRemote(remote)
    val result =
      try {
        lsRemoteCmd.callAsMap()
      } catch {
        case e: Exception => throw new IllegalStateException(s"Could not fetch hashes from $remote: ${e.getMessage}")
      }
    result.asScala.get(ref).map(_.getObjectId.name())
  }

  def commitHash(remote: URI, ref: String): Option[String] = commitHash(remote.toString, ref)

  implicit class IdeProbeGit(git: Git) {

    def checkout(ref: String): Ref = GitHandler.checkout(git, ref)
  }

  private class CustomProgressMonitor(repository: String) extends ProgressMonitor {

    private val totalWork: AtomicInteger = new AtomicInteger()
    private val completion: AtomicInteger = new AtomicInteger()
    private val task: AtomicReference[String] = new AtomicReference()

    @inline
    private def renderProgress(completed: Int): String = {
      val total = totalWork.get()
      val title = task.get()
      val inner =
        if (total == 0)
          ""
        else if (completed == total)
          s"$title 100% ($total/$total)\n"
        else {
          val percent = (100 * completed) / total
          s"$title $percent% ($completed/$total)"
        }
      s"\r$inner"
    }

    override def start(totalTasks: Int): Unit = {
      println(s"Cloning:$repository")
    }

    override def beginTask(title: String, totalWork: Int): Unit = {
      this.totalWork.set(totalWork)
      this.completion.set(0)
      this.task.set(title)
    }

    override def update(completed: Int): Unit = {
      val current = completion.getAndUpdate(_ + completed)
      print(renderProgress(current))
    }

    override def endTask(): Unit = {
      println("")
    }

    override def isCancelled: Boolean = false
  }
}
